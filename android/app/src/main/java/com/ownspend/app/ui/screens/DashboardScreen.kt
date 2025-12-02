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
                val endDate = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
                
                when (selectedPeriod) {
                    "week" -> calendar.add(Calendar.DAY_OF_YEAR, -7)
                    "month" -> calendar.add(Calendar.MONTH, -1)
                    "year" -> calendar.add(Calendar.YEAR, -1)
                }
                val startDate = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
                
                val response = ApiClient.getService(serverUrl).getSpendingSummary(
                    apiKey = apiKey,
                    startDate = startDate,
                    endDate = endDate
                )
                if (response.isSuccessful) {
                    summary = response.body()
                } else {
                    error = "Failed to load: ${response.code()}"
                }
            } catch (e: Exception) {
                error = e.message ?: "Network error"
            }
            isLoading = false
        }
    }
    
    LaunchedEffect(serverUrl, apiKey, selectedPeriod) {
        loadSummary()
    }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Period selector
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("week" to "Week", "month" to "Month", "year" to "Year").forEach { (value, label) ->
                    FilterChip(
                        selected = selectedPeriod == value,
                        onClick = { selectedPeriod = value },
                        label = { Text(label) }
                    )
                }
            }
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
                // Overview cards
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SummaryCard(
                            modifier = Modifier.weight(1f),
                            title = "Spent",
                            amount = summary!!.totalSpent,
                            currencyFormat = currencyFormat,
                            color = Color(0xFFE53935),
                            icon = Icons.Default.TrendingDown
                        )
                        SummaryCard(
                            modifier = Modifier.weight(1f),
                            title = "Received",
                            amount = summary!!.totalReceived,
                            currencyFormat = currencyFormat,
                            color = Color(0xFF43A047),
                            icon = Icons.Default.TrendingUp
                        )
                    }
                }
                
                // Net balance
                item {
                    val net = summary!!.totalReceived - summary!!.totalSpent
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (net >= 0) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Net Balance", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = currencyFormat.format(net),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (net >= 0) Color(0xFF43A047) else Color(0xFFE53935)
                            )
                        }
                    }
                }
                
                // Transaction count
                item {
                    Card {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Receipt, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Total Transactions")
                            }
                            Text(
                                text = summary!!.transactionCount.toString(),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                
                // Spending by category
                if (summary!!.byCategory.isNotEmpty()) {
                    item {
                        Text(
                            "Spending by Category",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    val sortedCategories = summary!!.byCategory.entries.sortedByDescending { it.value }
                    val maxAmount = sortedCategories.maxOfOrNull { it.value } ?: 1.0
                    
                    items(sortedCategories) { (category, amount) ->
                        CategorySpendingRow(
                            category = category,
                            amount = amount,
                            maxAmount = maxAmount,
                            currencyFormat = currencyFormat
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
