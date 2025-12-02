package com.ownspend.app.data.models

import com.google.gson.annotations.SerializedName

// Transaction from server
data class Transaction(
    val id: Int,
    val amount: Double,
    @SerializedName("transaction_type")
    val transactionType: String,
    val merchant: String?,
    val category: String?,
    @SerializedName("payment_method")
    val paymentMethod: String?,
    @SerializedName("bank_name")
    val bankName: String?,
    @SerializedName("account_masked")
    val accountMasked: String?,
    @SerializedName("reference_id")
    val referenceId: String?,
    @SerializedName("transaction_date")
    val transactionDate: String?,
    @SerializedName("raw_text")
    val rawText: String?,
    val notes: String?,
    val tags: String?,
    @SerializedName("created_at")
    val createdAt: String?,
    @SerializedName("updated_at")
    val updatedAt: String?
)

// Rule from server
data class Rule(
    val id: Int,
    val name: String,
    val description: String?,
    val priority: Int,
    @SerializedName("is_active")
    val isActive: Boolean,
    @SerializedName("match_type")
    val matchType: String,
    @SerializedName("match_field")
    val matchField: String,
    @SerializedName("match_value")
    val matchValue: String,
    @SerializedName("match_case_sensitive")
    val matchCaseSensitive: Boolean,
    @SerializedName("action_type")
    val actionType: String,
    @SerializedName("action_value")
    val actionValue: String,
    @SerializedName("created_at")
    val createdAt: String?,
    @SerializedName("updated_at")
    val updatedAt: String?
)

// Category from server
data class Category(
    val id: Int,
    val name: String,
    val icon: String?,
    val color: String?,
    @SerializedName("parent_id")
    val parentId: Int?,
    @SerializedName("is_income")
    val isIncome: Boolean,
    @SerializedName("created_at")
    val createdAt: String?
)

// Spending summary - matches actual API response format
data class SpendingSummary(
    val period: SummaryPeriod,
    val totals: SummaryTotals,
    @SerializedName("category_breakdown")
    val categoryBreakdown: List<CategoryBreakdownItem>,
    @SerializedName("top_merchants")
    val topMerchants: List<TopMerchantItem>
) {
    // Helper properties for UI
    val totalSpent: Double get() = totals.totalSpending
    val totalReceived: Double get() = totals.totalIncome
    val transactionCount: Int get() = totals.transactionCount
    val byCategory: Map<String, Double> get() = categoryBreakdown.associate { 
        (it.categoryName ?: "Uncategorized") to it.totalAmount 
    }
    val byPaymentMethod: Map<String, Double> get() = emptyMap() // API doesn't provide this
}

data class SummaryPeriod(
    @SerializedName("start_date")
    val startDate: String,
    @SerializedName("end_date")
    val endDate: String
)

data class SummaryTotals(
    @SerializedName("total_spending")
    val totalSpending: Double,
    @SerializedName("total_income")
    val totalIncome: Double,
    val net: Double,
    @SerializedName("internal_transfers")
    val internalTransfers: Int,
    @SerializedName("transaction_count")
    val transactionCount: Int
)

data class CategoryBreakdownItem(
    @SerializedName("category_id")
    val categoryId: Int,
    @SerializedName("category_name")
    val categoryName: String?,
    @SerializedName("total_amount")
    val totalAmount: Double,
    @SerializedName("transaction_count")
    val transactionCount: Int,
    val percentage: Double
)

data class TopMerchantItem(
    val merchant: String,
    @SerializedName("total_amount")
    val totalAmount: Double,
    val count: Int
)

data class DateRange(
    @SerializedName("start_date")
    val startDate: String?,
    @SerializedName("end_date")
    val endDate: String?
)

// API Responses
data class TransactionsResponse(
    val transactions: List<Transaction>,
    val total: Int,
    val page: Int,
    @SerializedName("page_size")
    val pageSize: Int
)

data class RulesResponse(
    val rules: List<Rule>,
    val total: Int
)

data class CategoriesResponse(
    val categories: List<Category>
)

// Create/Update requests
data class CreateRuleRequest(
    val name: String,
    val description: String? = null,
    val priority: Int = 100,
    @SerializedName("is_active")
    val isActive: Boolean = true,
    @SerializedName("match_type")
    val matchType: String,
    @SerializedName("match_field")
    val matchField: String,
    @SerializedName("match_value")
    val matchValue: String,
    @SerializedName("match_case_sensitive")
    val matchCaseSensitive: Boolean = false,
    @SerializedName("action_type")
    val actionType: String,
    @SerializedName("action_value")
    val actionValue: String
)

data class UpdateTransactionRequest(
    val category: String? = null,
    val merchant: String? = null,
    val notes: String? = null,
    val tags: String? = null
)

data class CreateCategoryRequest(
    val name: String,
    val icon: String? = null,
    val color: String? = null,
    @SerializedName("parent_id")
    val parentId: Int? = null,
    @SerializedName("is_income")
    val isIncome: Boolean = false
)
