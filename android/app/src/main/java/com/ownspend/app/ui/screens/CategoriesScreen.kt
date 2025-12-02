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
import com.ownspend.app.data.models.Category
import com.ownspend.app.data.models.CreateCategoryRequest
import com.ownspend.app.data.remote.ApiClient
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    serverUrl: String,
    apiKey: String
) {
    val scope = rememberCoroutineScope()
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    
    fun loadCategories() {
        if (serverUrl.isEmpty() || apiKey.isEmpty()) {
            error = "Please configure server settings"
            isLoading = false
            return
        }
        
        scope.launch {
            isLoading = true
            error = null
            try {
                android.util.Log.d("CategoriesScreen", "Loading categories from $serverUrl")
                val response = ApiClient.getService(serverUrl).getCategories(apiKey)
                if (response.isSuccessful) {
                    categories = response.body()?.categories ?: emptyList()
                    android.util.Log.d("CategoriesScreen", "Loaded ${categories.size} categories")
                } else {
                    error = "Failed to load: ${response.code()} - ${response.message()}"
                    android.util.Log.e("CategoriesScreen", "Error: ${response.code()}")
                }
            } catch (e: Exception) {
                error = e.message ?: "Network error"
                android.util.Log.e("CategoriesScreen", "Exception", e)
            }
            isLoading = false
        }
    }
    
    LaunchedEffect(serverUrl, apiKey) {
        loadCategories()
    }
    
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, "Add Category")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
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
                            TextButton(onClick = { loadCategories() }) { Text("Retry") }
                        }
                    }
                }
                categories.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Category, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
                            Spacer(Modifier.height(8.dp))
                            Text("No categories yet")
                            Text("Tap + to create a category", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                else -> {
                    // Group by income/expense
                    val expenseCategories = categories.filter { !it.isIncome }
                    val incomeCategories = categories.filter { it.isIncome }
                    
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (expenseCategories.isNotEmpty()) {
                            item {
                                Text(
                                    "Expense Categories",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE53935)
                                )
                            }
                            items(expenseCategories) { category ->
                                CategoryCard(
                                    category = category,
                                    onDelete = {
                                        scope.launch {
                                            try {
                                                ApiClient.getService(serverUrl).deleteCategory(apiKey, category.id)
                                                loadCategories()
                                            } catch (e: Exception) { }
                                        }
                                    }
                                )
                            }
                        }
                        
                        if (incomeCategories.isNotEmpty()) {
                            item {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Income Categories",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF43A047)
                                )
                            }
                            items(incomeCategories) { category ->
                                CategoryCard(
                                    category = category,
                                    onDelete = {
                                        scope.launch {
                                            try {
                                                ApiClient.getService(serverUrl).deleteCategory(apiKey, category.id)
                                                loadCategories()
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
    }
    
    if (showAddDialog) {
        AddCategoryDialog(
            serverUrl = serverUrl,
            apiKey = apiKey,
            onDismiss = { showAddDialog = false },
            onCategoryCreated = {
                showAddDialog = false
                loadCategories()
            }
        )
    }
}

@Composable
fun CategoryCard(
    category: Category,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    
    val categoryColor = category.color?.let { 
        try { Color(android.graphics.Color.parseColor(it)) } 
        catch (e: Exception) { MaterialTheme.colorScheme.primary }
    } ?: MaterialTheme.colorScheme.primary
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(categoryColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getCategoryIcon(category.name),
                        contentDescription = null,
                        tint = categoryColor
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    category.icon?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
            
            IconButton(onClick = { showDeleteConfirm = true }) {
                Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
    
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Category") },
            text = { Text("Are you sure you want to delete '${category.name}'?") },
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
fun AddCategoryDialog(
    serverUrl: String,
    apiKey: String,
    onDismiss: () -> Unit,
    onCategoryCreated: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var icon by remember { mutableStateOf("") }
    var color by remember { mutableStateOf("#4CAF50") }
    var isIncome by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    
    val colorOptions = listOf(
        "#F44336" to "Red",
        "#E91E63" to "Pink",
        "#9C27B0" to "Purple",
        "#673AB7" to "Deep Purple",
        "#3F51B5" to "Indigo",
        "#2196F3" to "Blue",
        "#00BCD4" to "Cyan",
        "#009688" to "Teal",
        "#4CAF50" to "Green",
        "#8BC34A" to "Light Green",
        "#CDDC39" to "Lime",
        "#FFC107" to "Amber",
        "#FF9800" to "Orange",
        "#FF5722" to "Deep Orange",
        "#795548" to "Brown",
        "#607D8B" to "Blue Grey"
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Category") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Category Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = icon,
                    onValueChange = { icon = it },
                    label = { Text("Icon (emoji or name)") },
                    placeholder = { Text("e.g., 🍔 or food") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Type toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = !isIncome,
                        onClick = { isIncome = false },
                        label = { Text("Expense") },
                        leadingIcon = {
                            if (!isIncome) Icon(Icons.Default.Check, null, Modifier.size(16.dp))
                        }
                    )
                    FilterChip(
                        selected = isIncome,
                        onClick = { isIncome = true },
                        label = { Text("Income") },
                        leadingIcon = {
                            if (isIncome) Icon(Icons.Default.Check, null, Modifier.size(16.dp))
                        }
                    )
                }
                
                // Color picker
                Text("Color", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    colorOptions.take(8).forEach { (hex, _) ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(hex)))
                                .then(
                                    if (color == hex) Modifier.padding(2.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                        .padding(2.dp)
                                        .clip(CircleShape)
                                        .background(Color(android.graphics.Color.parseColor(hex)))
                                    else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (color == hex) {
                                Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    scope.launch {
                        isLoading = true
                        try {
                            ApiClient.getService(serverUrl).createCategory(
                                apiKey = apiKey,
                                request = CreateCategoryRequest(
                                    name = name,
                                    icon = icon.takeIf { it.isNotEmpty() },
                                    color = color,
                                    isIncome = isIncome
                                )
                            )
                            onCategoryCreated()
                        } catch (e: Exception) {
                            // Handle error
                        }
                        isLoading = false
                    }
                },
                enabled = name.isNotEmpty() && !isLoading
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
