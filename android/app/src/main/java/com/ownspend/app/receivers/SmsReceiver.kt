package com.ownspend.app.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.ownspend.app.config.AppConfig
import com.ownspend.app.data.local.AppDatabase
import com.ownspend.app.data.local.PendingEvent
import com.ownspend.app.workers.SyncWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver to capture incoming SMS messages from banks.
 */
class SmsReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "SmsReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            return
        }
        
        Log.d(TAG, "SMS received")
        
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        
        for (message in messages) {
            val sender = message.displayOriginatingAddress ?: continue
            val body = message.messageBody ?: continue
            
            Log.d(TAG, "SMS from: $sender")
            
            // Check if it's a bank SMS
            if (isBankSms(sender)) {
                Log.d(TAG, "Bank SMS detected from $sender")
                processAndQueueSms(context, sender, body)
            }
        }
    }
    
    private fun isBankSms(sender: String): Boolean {
        val senderUpper = sender.uppercase().replace("-", "").replace(" ", "")
        return AppConfig.BANK_SMS_SENDERS.any { pattern ->
            senderUpper.contains(pattern)
        }
    }
    
    private fun processAndQueueSms(context: Context, sender: String, body: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getInstance(context)
                
                val event = PendingEvent(
                    sourceType = "SMS",
                    sourceSender = sender,
                    rawText = body,
                    receivedAt = System.currentTimeMillis()
                )
                
                val eventId = db.pendingEventDao().insert(event)
                Log.d(TAG, "SMS queued with ID: $eventId")
                
                // Trigger sync
                SyncWorker.enqueueOneTime(context)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error queuing SMS", e)
            }
        }
    }
}

/**
 * Boot receiver to restart services after device reboot.
 */
class BootReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "BootReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "Device booted, scheduling sync worker")
            SyncWorker.enqueuePeriodic(context)
        }
    }
}
