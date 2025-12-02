package com.ownspend.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ownspend.app.data.models.SpendingSummary
import com.ownspend.app.data.remote.ApiClient
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    serverUrl: String,
    apiKey: String
) {
    val scope = rememberCoroutineScope()
    var summary by remember { mutableStateOf<SpendingSummary?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedPeriod by remember { mutableStateOf("month") }
    
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")) }
    
    fun loadSummary() {
        if (serverUrl.isEmpty() || apiKey.isEmpty()) {
            error = "Please configure server settings"
            isLoading = false
            return
        }
        
        scope.launch {
            isLoading = true
            error = null
            try {
                // Calculate date range based on selected period
                val calendar = Calendar.getInstance()
                val dateFormatter = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val endDate = dateFormatter.format(calendar.time)
                
                when (selectedPeriod) {
                    "week" -> calendar.add(Calendar.DAY_OF_YEAR, -7)
                    "month" -> calendar.add(Calendar.MONTH, -1)
                    "year" -> calendar.add(Calendar.YEAR, -1)
                }
                val startDate = dateFormatter.format(calendar.time)
                
                android.util.Log.d("DashboardScreen", "Loading $selectedPeriod: $startDate to $endDate")
                android.util.Log.d("DashboardScreen", "URL: $serverUrl, API Key: ${apiKey.take(10)}...")
                
                val response = ApiClient.getService(serverUrl).getSpendingSummary(
                    apiKey = apiKey,
                    startDate = startDate,
                    endDate = endDate
                )
                if (response.isSuccessful) {
                    summary = response.body()
                    android.util.Log.d("DashboardScreen", "Success: ${summary?.transactionCount} transactions")
                } else {
                    error = "Failed to load: ${response.code()} - ${response.message()}"
                    android.util.Log.e("DashboardScreen", "Error: ${response.code()} - ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                error = e.message ?: "Network error"
                android.util.Log.e("DashboardScreen", "Exception", e)
            }
            isLoading = false
        }
    }
    
    LaunchedEffect(serverUrl, apiKey, selectedPeriod) {
        if (serverUrl.isNotEmpty() && apiKey.isNotEmpty()) {
            loadSummary()
        }
    }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(0.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // Header with gradient background
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                Column {
                    Text(
                        text = "Dashboard",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = when (selectedPeriod) {
                            "week" -> "Last 7 days"
                            "month" -> "Last 30 days"
                            "year" -> "Last year"
                            else -> ""
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        }
        
        // Period selector with modern chips
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                listOf("week" to "Week", "month" to "Month", "year" to "Year").forEach { (value, label) ->
                    FilterChip(
                        selected = selectedPeriod == value,
                        onClick = { selectedPeriod = value },
                        label = { 
                            Text(
                                label,
                                fontWeight = if (selectedPeriod == value) FontWeight.Bold else FontWeight.Normal
                            ) 
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        
        item {
            Spacer(Modifier.height(8.dp))
        }
        
        when {
            isLoading -> {
                item {
                    Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
            error != null -> {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error)
                            Text(error!!, color = MaterialTheme.colorScheme.error)
                            TextButton(onClick = { loadSummary() }) {
                                Text("Retry")
                            }
                        }
                    }
                }
            }
            summary != null -> {
                // Overview cards with modern design
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        ModernSummaryCard(
                            modifier = Modifier.weight(1f),
                            title = "Spent",
                            amount = summary!!.totalSpent,
                            currencyFormat = currencyFormat,
                            backgroundColor = Color(0xFFFFEBEE),
                            textColor = Color(0xFFE53935),
                            icon = Icons.Default.TrendingDown
                        )
                        ModernSummaryCard(
                            modifier = Modifier.weight(1f),
                            title = "Received",
                            amount = summary!!.totalReceived,
                            currencyFormat = currencyFormat,
                            backgroundColor = Color(0xFFE8F5E9),
                            textColor = Color(0xFF43A047),
                            icon = Icons.Default.TrendingUp
                        )
                    }
                }
                
                item {
                    Spacer(Modifier.height(16.dp))
                }
                
                // Net balance with modern design
                item {
                    val net = summary!!.totalReceived - summary!!.totalSpent
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (net >= 0) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(20.dp)
                                .fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        "Net Balance",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = if (net >= 0) Color(0xFF2E7D32) else Color(0xFFC62828),
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = currencyFormat.format(net),
                                        style = MaterialTheme.typography.headlineLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = if (net >= 0) Color(0xFF43A047) else Color(0xFFE53935)
                                    )
                                }
                                Icon(
                                    if (net >= 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = if (net >= 0) Color(0xFF43A047) else Color(0xFFE53935)
                                )
                            }
                            
                            Spacer(Modifier.height(12.dp))
                            Divider(color = if (net >= 0) Color(0xFFC8E6C9) else Color(0xFFFFCDD2))
                            Spacer(Modifier.height(12.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "${summary!!.transactionCount} transactions",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (net >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                                )
                                Text(
                                    when (selectedPeriod) {
                                        "week" -> "This week"
                                        "month" -> "This month"
                                        "year" -> "This year"
                                        else -> ""
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (net >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                                )
                            }
                        }
                    }
                }
                
                // Spending by category
                if (summary!!.byCategory.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(24.dp))
                    }
                    
                    item {
                        Text(
                            "Spending by Category",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                    }
                    
                    item {
                        Spacer(Modifier.height(12.dp))
                    }
                    
                    val sortedCategories = summary!!.byCategory.entries.sortedByDescending { it.value }
                    val maxAmount = sortedCategories.maxOfOrNull { it.value } ?: 1.0
                    
                    items(sortedCategories) { (category, amount) ->
                        ModernCategorySpendingRow(
                            category = category,
                            amount = amount,
                            maxAmount = maxAmount,
                            currencyFormat = currencyFormat,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                        )
                    }
                }
                
                // Spending by payment method
                if (summary!!.byPaymentMethod.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "By Payment Method",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    items(summary!!.byPaymentMethod.entries.toList()) { (method, amount) ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(method.ifEmpty { "Unknown" })
                                Text(
                                    currencyFormat.format(amount),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ModernSummaryCard(
    modifier: Modifier = Modifier,
    title: String,
    amount: Double,
    currencyFormat: NumberFormat,
    backgroundColor: Color,
    textColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = textColor.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    icon,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = currencyFormat.format(amount),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
    }
}

@Composable
fun SummaryCard(
    modifier: Modifier = Modifier,
    title: String,
    amount: Double,
    currencyFormat: NumberFormat,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color)
            }
            Spacer(Modifier.height(8.dp))
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = currencyFormat.format(amount),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
fun ModernCategorySpendingRow(
    category: String,
    amount: Double,
    maxAmount: Double,
    currencyFormat: NumberFormat,
    modifier: Modifier = Modifier
) {
    val progress = (amount / maxAmount).toFloat()
    val categoryColors = mapOf(
        "food" to Color(0xFFFF5722),
        "groceries" to Color(0xFF4CAF50),
        "transport" to Color(0xFF2196F3),
        "shopping" to Color(0xFF9C27B0),
        "bills" to Color(0xFF607D8B),
        "entertainment" to Color(0xFFE91E63),
        "health" to Color(0xFF00BCD4),
        "transfer" to Color(0xFFFF9800)
    )
    val color = categoryColors[category.lowercase()] ?: Color(0xFF757575)
    
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(color.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            getCategoryIcon(category),
                            null,
                            modifier = Modifier.size(22.dp),
                            tint = color
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        category.ifEmpty { "Uncategorized" },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        currencyFormat.format(amount),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                    Text(
                        "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color.copy(alpha = 0.15f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(color)
                )
            }
        }
    }
}

@Composable
fun CategorySpendingRow(
    category: String,
    amount: Double,
    maxAmount: Double,
    currencyFormat: NumberFormat
) {
    val progress = (amount / maxAmount).toFloat()
    val categoryColors = mapOf(
        "food" to Color(0xFFFF5722),
        "groceries" to Color(0xFF4CAF50),
        "transport" to Color(0xFF2196F3),
        "shopping" to Color(0xFF9C27B0),
        "bills" to Color(0xFF607D8B),
        "entertainment" to Color(0xFFE91E63),
        "health" to Color(0xFF00BCD4),
        "transfer" to Color(0xFFFF9800)
    )
    val color = categoryColors[category.lowercase()] ?: Color(0xFF757575)
    
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        getCategoryIcon(category),
                        null,
                        modifier = Modifier.size(20.dp),
                        tint = color
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(category.ifEmpty { "Uncategorized" })
                }
                Text(
                    currencyFormat.format(amount),
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(color.copy(alpha = 0.2f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .background(color)
                )
            }
        }
    }
}
