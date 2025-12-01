# OwnSpend Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        ANDROID APP (Phone)                       │
│                                                                   │
│  ┌──────────────┐        ┌────────────────────┐                │
│  │ SMS Listener │        │ Notification       │                 │
│  │ (Broadcast   │        │ Listener Service   │                 │
│  │  Receiver)   │        │ (UPI Apps)         │                 │
│  └──────┬───────┘        └────────┬───────────┘                 │
│         │                          │                              │
│         └──────────┬───────────────┘                             │
│                    ▼                                              │
│         ┌─────────────────────┐                                  │
│         │   Local Queue       │                                  │
│         │   (Room/SQLite)     │                                  │
│         └──────────┬──────────┘                                  │
│                    │                                              │
│                    ▼                                              │
│         ┌─────────────────────┐                                  │
│         │  Sync Worker        │                                  │
│         │  (WorkManager)      │                                  │
│         └──────────┬──────────┘                                  │
└────────────────────┼──────────────────────────────────────────┘
                     │ HTTPS (API Key Auth)
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│                    BACKEND API (FastAPI)                         │
│                                                                   │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │              POST /api/events/ingest                      │  │
│  │              (Receives Raw Events)                        │  │
│  └─────────────────────┬────────────────────────────────────┘  │
│                        │                                         │
│                        ▼                                         │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │                Raw Events Table                           │  │
│  │    (SMS text, notification text, metadata)                │  │
│  └─────────────────────┬────────────────────────────────────┘  │
│                        │                                         │
│                        ▼                                         │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │             Transaction Parser                            │  │
│  │  • Kotak Bank Parser                                      │  │
│  │  • UCO Bank Parser                                        │  │
│  │  • GPay Parser                                            │  │
│  │  • More parsers...                                        │  │
│  └─────────────────────┬────────────────────────────────────┘  │
│                        │                                         │
│                        ▼                                         │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │            Deduplication Engine                           │  │
│  │  (Check for existing transactions)                        │  │
│  └─────────────────────┬────────────────────────────────────┘  │
│                        │                                         │
│                        ▼                                         │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │              Transactions Table                           │  │
│  │  (Canonical parsed transactions)                          │  │
│  └─────────────────────┬────────────────────────────────────┘  │
│                        │                                         │
│                        ▼                                         │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │               Rules Engine (TODO)                         │  │
│  │  • Auto-categorization                                    │  │
│  │  • Merchant mapping                                       │  │
│  │  • Internal transfer detection                            │  │
│  └─────────────────────┬────────────────────────────────────┘  │
│                        │                                         │
│                        ├─────────────────────┐                  │
│                        ▼                     ▼                  │
│  ┌──────────────────────────┐   ┌──────────────────────────┐  │
│  │   SQLite Database        │   │  Google Sheets Webhook   │  │
│  │   • Users                │   │  (Apps Script)           │  │
│  │   • Devices              │   │                          │  │
│  │   • Accounts             │   └──────────┬───────────────┘  │
│  │   • Transactions         │              │                   │
│  │   • Merchants            │              ▼                   │
│  │   • Categories           │   ┌──────────────────────────┐  │
│  │   • Rules                │   │    Google Sheet          │  │
│  └──────────────────────────┘   │    (Mirror)              │  │
│                                   └──────────────────────────┘  │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │                 Query APIs                                │  │
│  │  • GET /api/transactions                                  │  │
│  │  • GET /api/merchants                                     │  │
│  │  • GET /api/categories                                    │  │
│  │  • GET /api/accounts                                      │  │
│  │  • GET /api/raw-events (debug)                            │  │
│  └────────────────────┬─────────────────────────────────────┘  │
└─────────────────────────┼────────────────────────────────────┘
                          │ HTTP/REST
                          ▼
┌─────────────────────────────────────────────────────────────────┐
│                    WEB DASHBOARD (React) (TODO)                  │
│                                                                   │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │ Transactions │  │  Merchants   │  │  Categories  │          │
│  │    View      │  │  Management  │  │  Management  │          │
│  └──────────────┘  └──────────────┘  └──────────────┘          │
│                                                                   │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │   Analytics  │  │    Rules     │  │   Settings   │          │
│  │   & Charts   │  │  Management  │  │   & Export   │          │
│  └──────────────┘  └──────────────┘  └──────────────┘          │
└─────────────────────────────────────────────────────────────────┘
```

## Data Flow

### 1. Event Capture (Android)
```
Bank/UPI SMS → SMS Listener → Local Queue
UPI App Notification → Notification Listener → Local Queue
Local Queue → Sync Worker → Backend API
```

### 2. Backend Processing
```
Raw Event → Parser → Extract Fields
  ↓
Check Account → Find/Create Account
  ↓
Generate Dedupe Key → Check Duplicates
  ↓
New Transaction? → Insert Transaction
  ↓
Run Rules (TODO) → Auto-categorize
  ↓
Sync to Google Sheets (TODO)
```

### 3. Query & Display
```
Web Dashboard → API Request → Backend
  ↓
Query Database → Format Response
  ↓
Return JSON → Render in UI
```

## Key Components

### Backend (FastAPI + SQLite)
- **Event Ingestion**: Receives raw SMS/notifications
- **Parser**: Extracts transaction data from text
- **Deduplication**: Prevents duplicate transactions
- **Storage**: SQLite for simplicity and portability
- **APIs**: RESTful endpoints for queries

### Android App (Kotlin) [TODO]
- **SMS Listener**: BroadcastReceiver for SMS
- **Notification Listener**: NotificationListenerService
- **Local Queue**: Room database for offline support
- **Sync Engine**: WorkManager for background sync

### Web Dashboard (React) [TODO]
- **Transaction View**: Browse, filter, search
- **Management**: Merchants, categories, rules
- **Analytics**: Charts, trends, insights
- **Export**: CSV, PDF reports

### Google Sheets [TODO]
- **Mirror**: Read-only copy of transactions
- **Quick View**: Excel-like interface
- **Sharing**: Easy to share with family

## Security

### Device Authentication
```
Android App → API Key Header → Backend
  ↓
Verify API Key → Check Device Active
  ↓
Allow Request → Update Last Seen
```

### Data Privacy
- All data stored locally (SQLite)
- No third-party services (except Google Sheets mirror)
- HTTPS for all communications
- API keys for device authentication

## Scalability

### Current (SQLite)
- Perfect for personal use
- Single user
- Thousands of transactions
- Local hosting

### Future (PostgreSQL)
- Multi-user support
- Remote hosting
- Millions of transactions
- High concurrency

---

## Technology Stack

| Component | Technology | Status |
|-----------|-----------|--------|
| Backend | Python + FastAPI | ✅ Done |
| Database | SQLite | ✅ Done |
| Android App | Kotlin | ⏳ TODO |
| Web Dashboard | React + TypeScript | ⏳ TODO |
| Charts | Recharts/Chart.js | ⏳ TODO |
| Google Sheets | Apps Script | ⏳ TODO |

---

Built following the architecture in `spend_tracker_system_roadmap.pdf`
