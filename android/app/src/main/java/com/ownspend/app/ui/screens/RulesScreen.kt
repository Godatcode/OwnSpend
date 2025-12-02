package com.ownspend.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ownspend.app.data.models.Rule
import com.ownspend.app.data.models.CreateRuleRequest
import com.ownspend.app.data.remote.ApiClient
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RulesScreen(
    serverUrl: String,
    apiKey: String
) {
    val scope = rememberCoroutineScope()
    var rules by remember { mutableStateOf<List<Rule>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showActiveOnly by remember { mutableStateOf(false) }
    
    fun loadRules() {
        if (serverUrl.isEmpty() || apiKey.isEmpty()) {
            error = "Please configure server settings"
            isLoading = false
            return
        }
        
        scope.launch {
            isLoading = true
            error = null
            try {
                val response = ApiClient.getService(serverUrl).getRules(
                    apiKey = apiKey,
                    isActive = if (showActiveOnly) true else null
                )
                if (response.isSuccessful) {
                    rules = response.body()?.rules ?: emptyList()
                } else {
                    error = "Failed to load: ${response.code()}"
                }
            } catch (e: Exception) {
                error = e.message ?: "Network error"
            }
            isLoading = false
        }
    }
    
    LaunchedEffect(serverUrl, apiKey, showActiveOnly) {
        loadRules()
    }
    
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, "Add Rule")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Filter
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${rules.size} rules",
                    style = MaterialTheme.typography.titleMedium
                )
                FilterChip(
                    selected = showActiveOnly,
                    onClick = { showActiveOnly = !showActiveOnly },
                    label = { Text("Active only") }
                )
            }
            
            when {
                isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                error != null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Error, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.error)
                            Text(error!!, color = MaterialTheme.colorScheme.error)
                            TextButton(onClick = { loadRules() }) { Text("Retry") }
                        }
                    }
                }
                rules.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Rule, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
                            Spacer(Modifier.height(8.dp))
                            Text("No rules yet")
                            Text("Tap + to create your first rule", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(rules) { rule ->
                            RuleCard(
                                rule = rule,
                                onToggle = {
                                    scope.launch {
                                        try {
                                            ApiClient.getService(serverUrl).toggleRule(apiKey, rule.id)
                                            loadRules()
                                        } catch (e: Exception) { }
                                    }
                                },
                                onDelete = {
                                    scope.launch {
                                        try {
                                            ApiClient.getService(serverUrl).deleteRule(apiKey, rule.id)
                                            loadRules()
                                        } catch (e: Exception) { }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
    
    if (showAddDialog) {
        AddRuleDialog(
            serverUrl = serverUrl,
            apiKey = apiKey,
            onDismiss = { showAddDialog = false },
            onRuleCreated = {
                showAddDialog = false
                loadRules()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RuleCard(
    rule: Rule,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (rule.isActive) 
                MaterialTheme.colorScheme.surface 
            else 
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (rule.isActive) Icons.Default.CheckCircle else Icons.Default.Cancel,
                        contentDescription = null,
                        tint = if (rule.isActive) Color(0xFF4CAF50) else Color(0xFF9E9E9E),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = rule.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Row {
                    IconButton(onClick = onToggle) {
                        Icon(
                            imageVector = if (rule.isActive) Icons.Default.ToggleOn else Icons.Default.ToggleOff,
                            contentDescription = "Toggle",
                            tint = if (rule.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    }
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
            
            rule.description?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            
            Spacer(Modifier.height(8.dp))
            
            // Match condition
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                AssistChip(
                    onClick = {},
                    label = { Text(rule.matchField, style = MaterialTheme.typography.labelSmall) }
                )
                AssistChip(
                    onClick = {},
                    label = { Text(rule.matchType, style = MaterialTheme.typography.labelSmall) }
                )
                AssistChip(
                    onClick = {},
                    label = { Text("\"${rule.matchValue}\"", style = MaterialTheme.typography.labelSmall) }
                )
            }
            
            Spacer(Modifier.height(4.dp))
            
            // Action
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ArrowForward, null, Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "${rule.actionType}: ${rule.actionValue}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
    
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Rule") },
            text = { Text("Are you sure you want to delete '${rule.name}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRuleDialog(
    serverUrl: String,
    apiKey: String,
    onDismiss: () -> Unit,
    onRuleCreated: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var matchField by remember { mutableStateOf("merchant") }
    var matchType by remember { mutableStateOf("contains") }
    var matchValue by remember { mutableStateOf("") }
    var actionType by remember { mutableStateOf("set_category") }
    var actionValue by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    
    val matchFields = listOf("merchant", "raw_text", "bank_name", "payment_method")
    val matchTypes = listOf("contains", "exact", "starts_with", "ends_with", "regex")
    val actionTypes = listOf("set_category", "set_merchant", "add_tag", "set_payment_method")
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Rule") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Rule Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Text("Match Condition", style = MaterialTheme.typography.titleSmall)
                }
                item {
                    ExposedDropdownMenuBox(
                        expanded = false,
                        onExpandedChange = {}
                    ) {
                        OutlinedTextField(
                            value = matchField,
                            onValueChange = { matchField = it },
                            label = { Text("Field") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        matchTypes.take(3).forEach { type ->
                            FilterChip(
                                selected = matchType == type,
                                onClick = { matchType = type },
                                label = { Text(type, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = matchValue,
                        onValueChange = { matchValue = it },
                        label = { Text("Match Value") },
                        placeholder = { Text("e.g., swiggy, amazon") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Text("Action", style = MaterialTheme.typography.titleSmall)
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = actionType == "set_category",
                            onClick = { actionType = "set_category" },
                            label = { Text("Category") }
                        )
                        FilterChip(
                            selected = actionType == "add_tag",
                            onClick = { actionType = "add_tag" },
                            label = { Text("Tag") }
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        value = actionValue,
                        onValueChange = { actionValue = it },
                        label = { Text(if (actionType == "set_category") "Category" else "Value") },
                        placeholder = { Text("e.g., Food, Shopping") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    scope.launch {
                        isLoading = true
                        try {
                            ApiClient.getService(serverUrl).createRule(
                                apiKey = apiKey,
                                request = CreateRuleRequest(
                                    name = name,
                                    description = description.takeIf { it.isNotEmpty() },
                                    matchType = matchType,
                                    matchField = matchField,
                                    matchValue = matchValue,
                                    actionType = actionType,
                                    actionValue = actionValue
                                )
                            )
                            onRuleCreated()
                        } catch (e: Exception) {
                            // Handle error
                        }
                        isLoading = false
                    }
                },
                enabled = name.isNotEmpty() && matchValue.isNotEmpty() && actionValue.isNotEmpty() && !isLoading
            ) {
                if (isLoading) CircularProgressIndicator(Modifier.size(20.dp))
                else Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
