# OwnSpend Android App

Android companion app for OwnSpend that captures bank SMS and UPI app notifications.

## Features

- 📱 **SMS Capture**: Automatically detects bank transaction SMS
- 🔔 **Notification Listener**: Captures UPI app notifications (GPay, PhonePe, Paytm)
- 📤 **Auto-Sync**: Sends transactions to your OwnSpend backend
- 🔐 **Secure**: Uses API key authentication

## Prerequisites

- Android Studio (latest version)
- Android device/emulator with API 26+ (Android 8.0+)
- OwnSpend backend running

## Setup Instructions

### 1. Open Project in Android Studio

1. Open Android Studio
2. Select "Open an existing project"
3. Navigate to `OwnSpend/android` folder
4. Wait for Gradle sync to complete

### 2. Configure Backend URL

Edit `app/src/main/java/com/ownspend/app/config/AppConfig.kt`:

```kotlin
object AppConfig {
    const val BASE_URL = "http://YOUR_SERVER_IP:8000"
}
```

Or configure in app settings after installation.

### 3. Get API Key

From your backend, create a device entry:

```bash
# The test API key from setup
API_KEY=CJWnWGnhFOM_3bBpPBWF0tZIlwzrOtQmXGtj4b9fyIc
```

Or create a new one via the API.

### 4. Build & Install

1. Connect your Android device (with USB debugging enabled)
2. Click "Run" in Android Studio
3. Select your device
4. App will install and launch

### 5. Grant Permissions

The app requires:
- **SMS Permission**: To read bank SMS messages
- **Notification Access**: To capture UPI app notifications

Grant these when prompted.

## Project Structure

```
android/
├── app/
│   ├── src/main/
│   │   ├── java/com/ownspend/app/
│   │   │   ├── MainActivity.kt
│   │   │   ├── config/
│   │   │   │   └── AppConfig.kt
│   │   │   ├── data/
│   │   │   │   ├── local/
│   │   │   │   │   ├── AppDatabase.kt
│   │   │   │   │   └── EventEntity.kt
│   │   │   │   └── remote/
│   │   │   │       └── ApiService.kt
│   │   │   ├── receivers/
│   │   │   │   └── SmsReceiver.kt
│   │   │   ├── services/
│   │   │   │   └── NotificationListenerService.kt
│   │   │   ├── workers/
│   │   │   │   └── SyncWorker.kt
│   │   │   └── ui/
│   │   │       ├── screens/
│   │   │       └── components/
│   │   ├── res/
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

## Key Components

### SmsReceiver
- Listens for incoming SMS using BroadcastReceiver
- Filters bank SMS by sender patterns
- Queues events for sync

### NotificationListenerService
- Listens for notifications from UPI apps
- Extracts transaction details
- Queues events for sync

### SyncWorker
- WorkManager-based background sync
- Handles network availability
- Retry logic with exponential backoff

### Room Database
- Local queue for pending events
- Survives app restarts
- Clears after successful sync

## Supported Banks

SMS parsing supports:
- Kotak Mahindra Bank
- HDFC Bank
- ICICI Bank
- State Bank of India
- Axis Bank
- UCO Bank

## Supported UPI Apps

Notification capture supports:
- Google Pay
- PhonePe
- Paytm
- Amazon Pay

## Troubleshooting

### SMS not being captured
1. Check SMS permission is granted
2. Ensure the bank SMS sender is in the filter list
3. Check Logcat for receiver logs

### Notifications not captured
1. Go to Settings > Apps > Special access > Notification access
2. Enable for OwnSpend
3. Restart the app

### Sync failing
1. Check backend URL is correct
2. Verify API key is valid
3. Check network connectivity
4. See sync logs in app

## Development

### Building
```bash
./gradlew assembleDebug
```

### Testing
```bash
./gradlew test
```

### Release Build
```bash
./gradlew assembleRelease
```

## License

MIT
