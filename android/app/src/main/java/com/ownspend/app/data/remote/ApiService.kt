package com.ownspend.app.data.remote

import com.ownspend.app.data.models.*
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

/**
 * API request/response models
 */
data class IngestEventRequest(
    val source_type: String,
    val source_sender: String,
    val source_package: String?,
    val raw_text: String,
    val received_at: String  // ISO format datetime
)

data class IngestEventResponse(
    val status: String,
    val event_id: Int?,
    val transaction_id: String?,
    val parse_status: String?,
    val message: String?
)

data class HealthResponse(
    val status: String,
    val timestamp: String
)

/**
 * Retrofit API service interface
 */
interface ApiService {
    
    @GET("/health")
    suspend fun healthCheck(): Response<HealthResponse>
    
    @POST("/api/events/ingest")
    suspend fun ingestEvent(
        @Header("api-key") apiKey: String,
        @Body event: IngestEventRequest
    ): Response<IngestEventResponse>
    
    // ============ Transactions ============
    
    @GET("/api/transactions")
    suspend fun getTransactions(
        @Header("api-key") apiKey: String,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 50,
        @Query("category") category: String? = null,
        @Query("transaction_type") transactionType: String? = null,
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null,
        @Query("search") search: String? = null
    ): Response<TransactionsResponse>
    
    @GET("/api/transactions/{id}")
    suspend fun getTransaction(
        @Header("api-key") apiKey: String,
        @Path("id") id: Int
    ): Response<Transaction>
    
    @PATCH("/api/transactions/{id}")
    suspend fun updateTransaction(
        @Header("api-key") apiKey: String,
        @Path("id") id: Int,
        @Body request: UpdateTransactionRequest
    ): Response<Transaction>
    
    @DELETE("/api/transactions/{id}")
    suspend fun deleteTransaction(
        @Header("api-key") apiKey: String,
        @Path("id") id: Int
    ): Response<Unit>
    
    // ============ Categories ============
    
    @GET("/api/categories")
    suspend fun getCategories(
        @Header("api-key") apiKey: String
    ): Response<CategoriesResponse>
    
    @POST("/api/categories")
    suspend fun createCategory(
        @Header("api-key") apiKey: String,
        @Body request: CreateCategoryRequest
    ): Response<Category>
    
    @DELETE("/api/categories/{id}")
    suspend fun deleteCategory(
        @Header("api-key") apiKey: String,
        @Path("id") id: Int
    ): Response<Unit>
    
    // ============ Rules ============
    
    @GET("/api/rules")
    suspend fun getRules(
        @Header("api-key") apiKey: String,
        @Query("is_active") isActive: Boolean? = null
    ): Response<RulesResponse>
    
    @POST("/api/rules")
    suspend fun createRule(
        @Header("api-key") apiKey: String,
        @Body request: CreateRuleRequest
    ): Response<Rule>
    
    @PATCH("/api/rules/{id}")
    suspend fun updateRule(
        @Header("api-key") apiKey: String,
        @Path("id") id: Int,
        @Body request: CreateRuleRequest
    ): Response<Rule>
    
    @DELETE("/api/rules/{id}")
    suspend fun deleteRule(
        @Header("api-key") apiKey: String,
        @Path("id") id: Int
    ): Response<Unit>
    
    @POST("/api/rules/{id}/toggle")
    suspend fun toggleRule(
        @Header("api-key") apiKey: String,
        @Path("id") id: Int
    ): Response<Rule>
    
    // ============ Export & Summary ============
    
    @GET("/api/export/summary")
    suspend fun getSpendingSummary(
        @Header("api-key") apiKey: String,
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null
    ): Response<SpendingSummary>
}

/**
 * API client singleton
 */
object ApiClient {
    private var retrofit: Retrofit? = null
    private var currentBaseUrl: String? = null
    
    fun getService(baseUrl: String): ApiService {
        if (retrofit == null || currentBaseUrl != baseUrl) {
            currentBaseUrl = baseUrl
            
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            
            val client = OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()
            
            retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
        
        return retrofit!!.create(ApiService::class.java)
    }
}
