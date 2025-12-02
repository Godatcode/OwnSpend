package com.ownspend.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ownspend.app.data.models.Transaction
import com.ownspend.app.data.models.UpdateTransactionRequest
import com.ownspend.app.data.remote.ApiClient
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    serverUrl: String,
    apiKey: String
) {
    val scope = rememberCoroutineScope()
    var transactions by remember { mutableStateOf<List<Transaction>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedTransaction by remember { mutableStateOf<Transaction?>(null) }
    var showFilterDialog by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var selectedType by remember { mutableStateOf<String?>(null) }
    
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")) }
    
    // Load transactions
    fun loadTransactions() {
        if (serverUrl.isEmpty() || apiKey.isEmpty()) {
            error = "Please configure server settings"
            isLoading = false
            return
        }
        
        scope.launch {
            isLoading = true
            error = null
            try {
                val response = ApiClient.getService(serverUrl).getTransactions(
                    apiKey = apiKey,
                    search = searchQuery.takeIf { it.isNotEmpty() },
                    category = selectedCategory,
                    transactionType = selectedType
                )
                if (response.isSuccessful) {
                    transactions = response.body()?.transactions ?: emptyList()
                } else {
                    error = "Failed to load: ${response.code()}"
                }
            } catch (e: Exception) {
                error = e.message ?: "Network error"
            }
            isLoading = false
        }
    }
    
    LaunchedEffect(serverUrl, apiKey) {
        loadTransactions()
    }
    
    LaunchedEffect(searchQuery, selectedCategory, selectedType) {
        loadTransactions()
    }
    
    Column(modifier = Modifier.fillMaxSize()) {
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("Search transactions...") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = {
                Row {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, "Clear")
                        }
                    }
                    IconButton(onClick = { showFilterDialog = true }) {
                        Icon(Icons.Default.FilterList, "Filter")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )
        
        // Filter chips
        if (selectedCategory != null || selectedType != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                selectedCategory?.let { cat ->
                    FilterChip(
                        selected = true,
                        onClick = { selectedCategory = null },
                        label = { Text(cat) },
                        trailingIcon = { Icon(Icons.Default.Close, null, Modifier.size(16.dp)) }
                    )
                }
                selectedType?.let { type ->
                    FilterChip(
                        selected = true,
                        onClick = { selectedType = null },
                        label = { Text(type) },
                        trailingIcon = { Icon(Icons.Default.Close, null, Modifier.size(16.dp)) }
                    )
                }
            }
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
                        TextButton(onClick = { loadTransactions() }) {
                            Text("Retry")
                        }
                    }
                }
            }
            transactions.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Receipt, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
                        Spacer(Modifier.height(8.dp))
                        Text("No transactions found")
                    }
                }
            }
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(transactions) { transaction ->
                        TransactionCard(
                            transaction = transaction,
                            currencyFormat = currencyFormat,
                            onClick = { selectedTransaction = transaction }
                        )
                    }
                }
            }
        }
    }
    
    // Transaction detail dialog
    selectedTransaction?.let { txn ->
        TransactionDetailDialog(
            transaction = txn,
            currencyFormat = currencyFormat,
            serverUrl = serverUrl,
            apiKey = apiKey,
            onDismiss = { selectedTransaction = null },
            onUpdate = { 
                loadTransactions()
                selectedTransaction = null
            }
        )
    }
    
    // Filter dialog
    if (showFilterDialog) {
        FilterDialog(
            selectedCategory = selectedCategory,
            selectedType = selectedType,
            onCategorySelected = { selectedCategory = it },
            onTypeSelected = { selectedType = it },
            onDismiss = { showFilterDialog = false }
        )
    }
}

@Composable
fun TransactionCard(
    transaction: Transaction,
    currencyFormat: NumberFormat,
    onClick: () -> Unit
) {
    val isDebit = transaction.transactionType == "debit"
    val amountColor = if (isDebit) Color(0xFFE53935) else Color(0xFF43A047)
    val amountPrefix = if (isDebit) "-" else "+"
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getCategoryIcon(transaction.category),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.merchant ?: "Unknown",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = transaction.category ?: "Uncategorized",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
                transaction.transactionDate?.let { date ->
                    Text(
                        text = formatDate(date),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
            
            Text(
                text = "$amountPrefix${currencyFormat.format(transaction.amount)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = amountColor
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailDialog(
    transaction: Transaction,
    currencyFormat: NumberFormat,
    serverUrl: String,
    apiKey: String,
    onDismiss: () -> Unit,
    onUpdate: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var isEditing by remember { mutableStateOf(false) }
    var category by remember { mutableStateOf(transaction.category ?: "") }
    var merchant by remember { mutableStateOf(transaction.merchant ?: "") }
    var notes by remember { mutableStateOf(transaction.notes ?: "") }
    var isLoading by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditing) "Edit Transaction" else "Transaction Details") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Amount
                Text(
                    text = currencyFormat.format(transaction.amount),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (transaction.transactionType == "debit") Color(0xFFE53935) else Color(0xFF43A047)
                )
                
                Divider()
                
                if (isEditing) {
                    OutlinedTextField(
                        value = merchant,
                        onValueChange = { merchant = it },
                        label = { Text("Merchant") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Category") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes") },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    DetailRow("Merchant", transaction.merchant ?: "Unknown")
                    DetailRow("Category", transaction.category ?: "Uncategorized")
                    DetailRow("Payment Method", transaction.paymentMethod ?: "—")
                    DetailRow("Bank", transaction.bankName ?: "—")
                    DetailRow("Account", transaction.accountMasked ?: "—")
                    DetailRow("Reference", transaction.referenceId ?: "—")
                    transaction.notes?.let { DetailRow("Notes", it) }
                }
            }
        },
        confirmButton = {
            if (isEditing) {
                Button(
                    onClick = {
                        scope.launch {
                            isLoading = true
                            try {
                                ApiClient.getService(serverUrl).updateTransaction(
                                    apiKey = apiKey,
                                    id = transaction.id,
                                    request = UpdateTransactionRequest(
                                        category = category.takeIf { it.isNotEmpty() },
                                        merchant = merchant.takeIf { it.isNotEmpty() },
                                        notes = notes.takeIf { it.isNotEmpty() }
                                    )
                                )
                                onUpdate()
                            } catch (e: Exception) {
                                // Handle error
                            }
                            isLoading = false
                        }
                    },
                    enabled = !isLoading
                ) {
                    if (isLoading) CircularProgressIndicator(Modifier.size(20.dp))
                    else Text("Save")
                }
            } else {
                Button(onClick = { isEditing = true }) {
                    Icon(Icons.Default.Edit, null)
                    Spacer(Modifier.width(4.dp))
                    Text("Edit")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isEditing) "Cancel" else "Close")
            }
        }
    )
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = MaterialTheme.colorScheme.outline)
        Text(value, fontWeight = FontWeight.Medium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterDialog(
    selectedCategory: String?,
    selectedType: String?,
    onCategorySelected: (String?) -> Unit,
    onTypeSelected: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    val categories = listOf("Food", "Groceries", "Transport", "Shopping", "Bills", "Entertainment", "Health", "Transfer")
    val types = listOf("debit", "credit")
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filter Transactions") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Category", style = MaterialTheme.typography.titleSmall)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { onCategorySelected(if (selectedCategory == cat) null else cat) },
                            label = { Text(cat) }
                        )
                    }
                }
                
                Text("Type", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    types.forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { onTypeSelected(if (selectedType == type) null else type) },
                            label = { Text(type.replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = {
                onCategorySelected(null)
                onTypeSelected(null)
                onDismiss()
            }) {
                Text("Clear All")
            }
        }
    )
}

@Composable
fun rememberScrollState() = androidx.compose.foundation.rememberScrollState()

fun getCategoryIcon(category: String?) = when (category?.lowercase()) {
    "food", "restaurant", "dining" -> Icons.Default.Restaurant
    "groceries", "grocery" -> Icons.Default.ShoppingCart
    "transport", "travel", "uber", "ola" -> Icons.Default.DirectionsCar
    "shopping", "amazon", "flipkart" -> Icons.Default.ShoppingBag
    "bills", "utilities", "electricity" -> Icons.Default.Receipt
    "entertainment", "movies", "netflix" -> Icons.Default.Movie
    "health", "medical", "pharmacy" -> Icons.Default.LocalHospital
    "transfer", "upi" -> Icons.Default.SwapHoriz
    "salary", "income" -> Icons.Default.AccountBalance
    else -> Icons.Default.Payment
}

fun formatDate(dateStr: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val outputFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val date = inputFormat.parse(dateStr)
        date?.let { outputFormat.format(it) } ?: dateStr
    } catch (e: Exception) {
        dateStr.take(10)
    }
}
