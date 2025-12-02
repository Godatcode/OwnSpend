package com.ownspend.app

import android.app.Application
import androidx.work.Configuration
import androidx.work.WorkManager

class OwnSpendApp : Application(), Configuration.Provider {
    
    override fun onCreate() {
        super.onCreate()
        instance = this
    }
    
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
    
    companion object {
        lateinit var instance: OwnSpendApp
            private set
    }
}
