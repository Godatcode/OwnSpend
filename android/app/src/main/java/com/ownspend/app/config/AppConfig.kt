package com.ownspend.app.config

/**
 * App configuration constants.
 * Update BASE_URL with your server address.
 */
object AppConfig {
    // Default server URL - update this or configure in app settings
    const val DEFAULT_BASE_URL = "http://192.168.1.100:8000"
    
    // SMS sender patterns to capture (case-insensitive)
    val BANK_SMS_SENDERS = listOf(
        "KOTAK", "KOTAKB", "KOTAKBNK",
        "HDFCBK", "HDFCBANK", "HDFC",
        "ICICIB", "ICICIBANK", "ICICI",
        "SBIBNK", "SBINB", "SBI", "ATMSBI",
        "AXISBK", "AXISBANK", "AXIS",
        "UCOBNK", "UCOBANK", "UCO",
        "PNBSMS", "PNBBANK", "PNB",
        "BOIIND", "BANKOFINDIA", "BOI",
        "CANBNK", "CANARABANK",
        "INDBNK", "INDIANBANK",
        "UNIONB", "UNIONBANK",
        "IDBIBK", "IDBI",
        "YESBK", "YESBANK",
        "FEDBK", "FEDERALBANK"
    )
    
    // UPI app package names to monitor notifications
    val UPI_APP_PACKAGES = listOf(
        "com.google.android.apps.nbu.paisa.user",  // Google Pay
        "com.phonepe.app",                          // PhonePe
        "net.one97.paytm",                          // Paytm
        "in.amazon.mShop.android.shopping",         // Amazon Pay
        "com.whatsapp",                             // WhatsApp Pay
        "com.truecaller",                           // Truecaller (SMS notifications)
        "com.google.android.apps.messaging",        // Google Messages
        "com.samsung.android.messaging",            // Samsung Messages
        "com.android.mms"                           // Default Android Messages
    )
    
    // Sync configuration
    const val SYNC_INTERVAL_MINUTES = 15L
    const val SYNC_RETRY_DELAY_SECONDS = 30L
    const val MAX_SYNC_RETRIES = 3
}
