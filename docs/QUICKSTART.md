# OwnSpend - Quick Start Guide

## ✅ What's Been Set Up

Your OwnSpend backend is now **running and ready**! Here's what has been initialized:

### 🗄️ Database
- **SQLite database** created at: `backend/ownspend.db`
- All tables created:
  - users, devices, accounts
  - raw_events, transactions
  - merchants, categories, rules

### 👤 Test User
- **Email**: `arka@ownspend.local`
- Created with 10 default categories

### 📱 Device API Key
Your Android app API key has been generated. You can find it by running:
```bash
cd backend
python setup.py
```

### 🚀 Backend Server
Currently running at: **http://localhost:8000**

API Documentation: **http://localhost:8000/docs**

---

## 🎯 Next Steps

### 1. Test the Backend

Update the API key in `backend/test_ingestion.py` and run:
```bash
cd backend
python test_ingestion.py
```

This will:
- Send sample SMS messages to the backend
- Test parsing for Kotak, UCO, and GPay
- Show you the created transactions

### 2. View API Documentation

Visit: http://localhost:8000/docs

You can:
- See all available endpoints
- Test API calls directly from the browser
- View request/response schemas

### 3. Check What's Working

**Endpoints available:**
- `GET /` - API info
- `GET /health` - Health check
- `POST /api/events/ingest` - Receive events from Android (requires API key)
- `GET /api/transactions` - List all transactions
- `GET /api/accounts` - List bank accounts
- `GET /api/merchants` - List merchants
- `GET /api/categories` - List categories
- `GET /api/raw-events` - Debug raw SMS/notifications

### 4. Build the Android App (Next Phase)

The Android app will need to:
- Listen to SMS from banks (Kotak, UCO, etc.)
- Listen to UPI app notifications (GPay, Navi, Kotak811)
- Send events to: `POST http://YOUR_IP:8000/api/events/ingest`
- Use the device API key in the header: `api-key: YOUR_API_KEY`

### 5. Build the Frontend Dashboard (Future)

The React dashboard will connect to:
- `http://localhost:8000/api/*` endpoints
- Show transactions, manage merchants, categories
- Add charts and analytics

---

## 📊 Current Parser Support

The backend can currently parse:

✅ **Kotak Bank SMS**
- Example: "Sent Rs.15.00 from Kotak Bank AC X1415 to amitabh10b26.hts21@okicici via UPI..."

✅ **UCO Bank SMS/UPI**
- Example: "UCO-UPI/CR/434750881179/amitabh10b26.hts21@okicici/UCO BANK/XX3242/15.00"

✅ **Google Pay Notifications**
- Example: "You paid ₹50.00 to Coffee Shop"

---

## 🔧 Common Commands

### Start Backend
```bash
cd /Users/arkaghosh/Documents/GitHub/OwnSpend
source .venv/bin/activate  # or activate the venv
python -m uvicorn backend.main:app --reload --host 0.0.0.0 --port 8000
```

### Check Database
```bash
cd backend
sqlite3 ownspend.db
# Then run SQL queries like:
# SELECT * FROM users;
# SELECT * FROM transactions;
```

### View Logs
The terminal where uvicorn is running shows all API requests in real-time.

---

## 🛠️ What Still Needs to be Built

### Phase 2: Backend Enhancements
- [ ] Rules engine for auto-categorization
- [ ] Google Sheets webhook integration
- [ ] Merchant/Category management APIs (CRUD)
- [ ] Transaction editing API
- [ ] Reparse/rebuild utilities
- [ ] Authentication for dashboard

### Phase 3: Android App
- [ ] SMS listener (BroadcastReceiver)
- [ ] Notification listener (NotificationListenerService)
- [ ] Local queue (Room database)
- [ ] Background sync (WorkManager)
- [ ] Settings UI

### Phase 4: Web Dashboard
- [ ] Authentication
- [ ] Transaction list with filters
- [ ] Merchant management UI
- [ ] Category management UI
- [ ] Charts (spending by category, trends)
- [ ] Export to CSV

### Phase 5: Advanced Features
- [ ] Budgets per category
- [ ] Budget alerts
- [ ] Multi-device support
- [ ] Data import/export

---

## 📝 Database Schema Reference

### Users Table
- id, email, password_hash, created_at

### Devices Table
- id, user_id, device_name, api_key, last_seen_at, is_active

### Accounts Table
- id, user_id, bank_name, account_mask, display_name, type, is_active

### Raw Events Table
- id, user_id, device_id, source_type, source_sender, raw_text
- received_at, inserted_at, parsed_status, error_message

### Transactions Table
- id (UUID), user_id, account_id, direction, amount, currency
- channel, raw_merchant_identifier, merchant_key
- merchant_id, category_id, description
- transaction_time, ingested_at, dedupe_key
- is_internal_transfer, manual_override_flags

### Merchants Table
- id, merchant_key, display_name, default_category_id
- notes, is_personal_contact, is_self_account

### Categories Table
- id, name, parent_id, sort_order

### Rules Table
- id, user_id, match_type, match_value
- action_type, action_value, priority, is_active

---

## 🎉 Success!

Your OwnSpend backend foundation is complete and running! The core ingestion pipeline, parsing logic, and deduplication are all functional.

**API Documentation:** http://localhost:8000/docs

---

**Built following the roadmap in:** `spend_tracker_system_roadmap.pdf`
