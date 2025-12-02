package com.ownspend.app.services

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.ownspend.app.config.AppConfig
import com.ownspend.app.data.local.AppDatabase
import com.ownspend.app.data.local.PendingEvent
import com.ownspend.app.workers.SyncWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * NotificationListenerService to capture UPI app notifications.
 */
class TransactionNotificationListener : NotificationListenerService() {
    
    companion object {
        private const val TAG = "NotificationListener"
    }
    
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        
        // Check if it's from a UPI app
        if (!AppConfig.UPI_APP_PACKAGES.contains(packageName)) {
            return
        }
        
        Log.d(TAG, "UPI notification from: $packageName")
        
        try {
            val notification = sbn.notification
            val extras = notification.extras
            
            // Extract notification text
            val title = extras.getCharSequence("android.title")?.toString() ?: ""
            val text = extras.getCharSequence("android.text")?.toString() ?: ""
            val bigText = extras.getCharSequence("android.bigText")?.toString() ?: ""
            
            // Combine all text
            val fullText = buildString {
                if (title.isNotEmpty()) append("$title ")
                if (text.isNotEmpty()) append("$text ")
                if (bigText.isNotEmpty() && bigText != text) append(bigText)
            }.trim()
            
            if (fullText.isEmpty()) {
                return
            }
            
            // Check if it looks like a transaction notification
            if (isTransactionNotification(fullText)) {
                Log.d(TAG, "Transaction notification detected: $fullText")
                processAndQueueNotification(packageName, fullText)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing notification", e)
        }
    }
    
    private fun isTransactionNotification(text: String): Boolean {
        val textLower = text.lowercase()
        
        // Keywords that indicate a transaction
        val transactionKeywords = listOf(
            "paid", "received", "sent", "payment", "credited", "debited",
            "₹", "rs.", "rs ", "inr", "successful", "completed",
            "transaction", "transferred"
        )
        
        return transactionKeywords.any { textLower.contains(it) }
    }
    
    private fun processAndQueueNotification(packageName: String, text: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getInstance(applicationContext)
                
                // Get app name from package
                val appName = when (packageName) {
                    "com.google.android.apps.nbu.paisa.user" -> "Google Pay"
                    "com.phonepe.app" -> "PhonePe"
                    "net.one97.paytm" -> "Paytm"
                    "in.amazon.mShop.android.shopping" -> "Amazon Pay"
                    "com.whatsapp" -> "WhatsApp Pay"
                    "com.truecaller" -> "Truecaller"
                    "com.google.android.apps.messaging" -> "Google Messages"
                    "com.samsung.android.messaging" -> "Samsung Messages"
                    "com.android.mms" -> "Messages"
                    else -> packageName
                }
                
                val event = PendingEvent(
                    sourceType = "NOTIFICATION",
                    sourceSender = appName,
                    sourcePackage = packageName,
                    rawText = text,
                    receivedAt = System.currentTimeMillis()
                )
                
                val eventId = db.pendingEventDao().insert(event)
                Log.d(TAG, "Notification queued with ID: $eventId")
                
                // Trigger sync
                SyncWorker.enqueueOneTime(applicationContext)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error queuing notification", e)
            }
        }
    }
    
    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // Not needed for our use case
    }
}
