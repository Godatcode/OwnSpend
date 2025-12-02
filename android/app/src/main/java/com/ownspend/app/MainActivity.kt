package com.ownspend.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.ownspend.app.data.SettingsRepository
import com.ownspend.app.data.local.AppDatabase
import com.ownspend.app.data.local.PendingEvent
import com.ownspend.app.ui.screens.*
import com.ownspend.app.ui.theme.OwnSpendTheme
import com.ownspend.app.workers.SyncWorker
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Handle permission results
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        requestPermissions()
        
        // Schedule periodic sync
        SyncWorker.enqueuePeriodic(this)
        
        setContent {
            OwnSpendTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }
    
    private fun requestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS
        )
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        
        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        
        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val settingsRepo = remember { SettingsRepository(context) }
    val db = remember { AppDatabase.getInstance(context) }
    val scope = rememberCoroutineScope()
    
    var selectedTab by remember { mutableIntStateOf(0) }
    
    val serverUrl by settingsRepo.serverUrl.collectAsState(initial = "")
    val apiKey by settingsRepo.apiKey.collectAsState(initial = "")
    val pendingCount by db.pendingEventDao().getPendingCountFlow().collectAsState(initial = 0)
    val syncedCount by db.pendingEventDao().getSyncedCountFlow().collectAsState(initial = 0)
    val allEvents by db.pendingEventDao().getAllEventsFlow().collectAsState(initial = emptyList())
    val lastSyncTime by settingsRepo.lastSyncTime.collectAsState(initial = 0L)
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        when (selectedTab) {
                            0 -> "Dashboard"
                            1 -> "Transactions"
                            2 -> "Rules"
                            3 -> "Categories"
                            4 -> "Settings"
                            else -> "OwnSpend"
                        }
                    ) 
                },
                actions = {
                    IconButton(onClick = {
                        SyncWorker.enqueueOneTime(context)
                    }) {
                        Icon(Icons.Default.Sync, "Sync Now")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Dashboard, "Dashboard") },
                    label = { Text("Dashboard") },
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Receipt, "Transactions") },
                    label = { Text("Transactions") },
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Rule, "Rules") },
                    label = { Text("Rules") },
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Category, "Categories") },
                    label = { Text("Categories") },
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, "Settings") },
                    label = { Text("Settings") },
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 }
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (selectedTab) {
                0 -> DashboardScreen(serverUrl = serverUrl, apiKey = apiKey)
                1 -> TransactionsScreen(serverUrl = serverUrl, apiKey = apiKey)
                2 -> RulesScreen(serverUrl = serverUrl, apiKey = apiKey)
                3 -> CategoriesScreen(serverUrl = serverUrl, apiKey = apiKey)
                4 -> SettingsScreen(
                    serverUrl = serverUrl,
                    apiKey = apiKey,
                    pendingCount = pendingCount,
                    syncedCount = syncedCount,
                    lastSyncTime = lastSyncTime,
                    onServerUrlChange = { scope.launch { settingsRepo.setServerUrl(it) } },
                    onApiKeyChange = { scope.launch { settingsRepo.setApiKey(it) } }
                )
            }
        }
    }
}

@Composable
fun SettingsScreen(
    serverUrl: String,
    apiKey: String,
    pendingCount: Int,
    syncedCount: Int,
    lastSyncTime: Long,
    onServerUrlChange: (String) -> Unit,
    onApiKeyChange: (String) -> Unit
) {
    val context = LocalContext.current
    var urlInput by remember(serverUrl) { mutableStateOf(serverUrl) }
    var keyInput by remember(apiKey) { mutableStateOf(apiKey) }
    val isConfigured = serverUrl.isNotEmpty() && apiKey.isNotEmpty()
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Connection Status
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isConfigured)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isConfigured) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = if (isConfigured) Color(0xFF4CAF50) else Color(0xFFF44336)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (isConfigured) "Connected" else "Not Configured",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (!isConfigured) {
                            Text(
                                text = "Enter server URL and API key below",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
        
        // Stats
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Pending",
                    value = pendingCount.toString(),
                    icon = Icons.Default.Schedule
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Synced",
                    value = syncedCount.toString(),
                    icon = Icons.Default.CloudDone
                )
            }
        }
        
        // Last sync time
        if (lastSyncTime > 0) {
            item {
                val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                Text(
                    text = "Last sync: ${dateFormat.format(Date(lastSyncTime))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
        
        // Server Configuration
        item {
            Text(
                text = "Server Configuration",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        
        item {
            OutlinedTextField(
                value = urlInput,
                onValueChange = {
                    urlInput = it
                    onServerUrlChange(it)
                },
                label = { Text("Server URL") },
                placeholder = { Text("http://10.0.2.2:8000") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Link, null) }
            )
        }
        
        item {
            OutlinedTextField(
                value = keyInput,
                onValueChange = {
                    keyInput = it
                    onApiKeyChange(it)
                },
                label = { Text("API Key") },
                placeholder = { Text("Your device API key") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Key, null) }
            )
        }
        
        // Permissions
        item {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Permissions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        
        item {
            OutlinedButton(
                onClick = {
                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Notifications, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Enable Notification Access")
            }
        }
        
        item {
            Text(
                text = "• SMS capture: enabled automatically\n• Notification access: required for UPI apps (GPay, PhonePe, Paytm)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
        
        // About
        item {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "About",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("OwnSpend v1.0.0", fontWeight = FontWeight.Medium)
                    Text(
                        "Self-hosted expense tracking",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
