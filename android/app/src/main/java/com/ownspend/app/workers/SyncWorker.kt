package com.ownspend.app.workers

import android.content.Context
import android.util.Log
import androidx.work.*
import com.ownspend.app.config.AppConfig
import com.ownspend.app.data.SettingsRepository
import com.ownspend.app.data.local.AppDatabase
import com.ownspend.app.data.remote.ApiClient
import com.ownspend.app.data.remote.IngestEventRequest
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * WorkManager worker to sync pending events to the backend.
 */
class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    companion object {
        private const val TAG = "SyncWorker"
        private const val WORK_NAME_PERIODIC = "sync_periodic"
        private const val WORK_NAME_ONE_TIME = "sync_one_time"
        
        fun enqueuePeriodic(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            
            val request = PeriodicWorkRequestBuilder<SyncWorker>(
                AppConfig.SYNC_INTERVAL_MINUTES, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    AppConfig.SYNC_RETRY_DELAY_SECONDS,
                    TimeUnit.SECONDS
                )
                .build()
            
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORK_NAME_PERIODIC,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
            
            Log.d(TAG, "Periodic sync scheduled")
        }
        
        fun enqueueOneTime(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            
            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    AppConfig.SYNC_RETRY_DELAY_SECONDS,
                    TimeUnit.SECONDS
                )
                .build()
            
            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    WORK_NAME_ONE_TIME,
                    ExistingWorkPolicy.REPLACE,
                    request
                )
            
            Log.d(TAG, "One-time sync enqueued")
        }
        
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME_PERIODIC)
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME_ONE_TIME)
            Log.d(TAG, "Sync workers cancelled")
        }
    }
    
    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting sync work")
        
        val settingsRepo = SettingsRepository(applicationContext)
        val serverUrl = settingsRepo.serverUrl.first()
        val apiKey = settingsRepo.apiKey.first()
        
        if (serverUrl.isEmpty() || apiKey.isEmpty()) {
            Log.w(TAG, "Server URL or API key not configured")
            return Result.failure()
        }
        
        val db = AppDatabase.getInstance(applicationContext)
        val pendingEvents = db.pendingEventDao().getPendingEvents()
        
        if (pendingEvents.isEmpty()) {
            Log.d(TAG, "No pending events to sync")
            return Result.success()
        }
        
        Log.d(TAG, "Syncing ${pendingEvents.size} events")
        
        val apiService = ApiClient.getService(serverUrl)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        
        var successCount = 0
        var failCount = 0
        
        for (event in pendingEvents) {
            try {
                db.pendingEventDao().markSyncing(event.id)
                
                val request = IngestEventRequest(
                    source_type = event.sourceType,
                    source_sender = event.sourceSender,
                    source_package = event.sourcePackage,
                    raw_text = event.rawText,
                    device_timestamp = dateFormat.format(Date(event.receivedAt))
                )
                
                val response = apiService.ingestEvent(apiKey, request)
                
                if (response.isSuccessful) {
                    db.pendingEventDao().markSynced(event.id)
                    successCount++
                    Log.d(TAG, "Event ${event.id} synced successfully")
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    db.pendingEventDao().markFailed(event.id, "HTTP ${response.code()}: $errorBody")
                    failCount++
                    Log.e(TAG, "Event ${event.id} sync failed: $errorBody")
                }
                
            } catch (e: Exception) {
                db.pendingEventDao().markFailed(event.id, e.message ?: "Unknown error")
                failCount++
                Log.e(TAG, "Event ${event.id} sync error", e)
            }
        }
        
        // Update last sync time
        settingsRepo.updateLastSyncTime()
        
        // Clean up old synced events (older than 7 days)
        val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)
        db.pendingEventDao().deleteOldSyncedEvents(sevenDaysAgo)
        
        Log.d(TAG, "Sync complete: $successCount success, $failCount failed")
        
        return if (failCount > 0 && successCount == 0) {
            Result.retry()
        } else {
            Result.success()
        }
    }
}
