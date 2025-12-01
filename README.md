# OwnSpend - Personal Spend Tracking System

A self-hosted personal expense tracking system that automatically captures transaction data from SMS and UPI app notifications on Android.

## Architecture

### Components
1. **Backend (FastAPI + SQLite)** - Core API and transaction processing
2. **Frontend (React)** - Web dashboard for viewing and managing transactions
3. **Android App** - Captures SMS and notifications, sends to backend
4. **Google Sheets Integration** - Mirror transactions for quick viewing

## Features

- 🔄 Automatic transaction capture from SMS and UPI notifications
- 📊 Full-featured web dashboard
- 🏦 Multi-bank/UPI app support (Kotak, UCO, GPay, Navi, etc.)
- 🔍 Smart parsing and deduplication
- 🏷️ Merchant and category management
- 📝 Transaction editing and categorization
- 📈 Analytics and reporting
- 🔄 Google Sheets sync

## Project Structure

```
OwnSpend/
├── backend/           # FastAPI backend
│   ├── main.py       # Main API endpoints
│   ├── models.py     # Database models
│   ├── parser.py     # SMS/notification parser
│   ├── database.py   # Database configuration
│   ├── schemas.py    # Pydantic schemas
│   ├── setup.py      # Initial setup script
│   └── test_ingestion.py  # Testing script
├── frontend/         # React web dashboard (TODO)
├── android/          # Android app (TODO)
├── docs/             # Documentation
│   ├── QUICKSTART.md    # Getting started guide
│   ├── ARCHITECTURE.md  # System architecture
│   └── PROGRESS.md      # Development roadmap
└── README.md
```

## 📚 Documentation

- **[Quick Start Guide](docs/QUICKSTART.md)** - Get up and running
- **[Architecture](docs/ARCHITECTURE.md)** - System design and data flow
- **[Progress Tracker](docs/PROGRESS.md)** - Development roadmap and status

## Backend Setup

### Prerequisites
- Python 3.10+
- Virtual environment (recommended)

### Installation

1. Navigate to backend directory:
```bash
cd backend
```

2. Install dependencies:
```bash
pip install -r requirements.txt
```

3. Create `.env` file:
```bash
cp .env.example .env
# Edit .env with your configuration
```

4. Run the server:
```bash
cd backend
uvicorn main:app --reload --host 0.0.0.0 --port 8000
```

The API will be available at `http://localhost:8000`

API documentation: `http://localhost:8000/docs`

## Database Schema

### Core Tables
- **users** - User accounts
- **devices** - Registered Android devices
- **accounts** - Bank/wallet accounts
- **raw_events** - Raw SMS/notification data
- **transactions** - Parsed canonical transactions
- **merchants** - Normalized merchant records
- **categories** - Transaction categories
- **rules** - Auto-categorization rules

## API Endpoints

### Health & Info
- `GET /` - API info
- `GET /health` - Health check

### Event Ingestion
- `POST /api/events/ingest` - Receive raw events from Android (requires device API key)

### Transactions
- `GET /api/transactions` - List transactions with filters
- `GET /api/accounts` - List accounts
- `GET /api/merchants` - List merchants
- `GET /api/categories` - List categories

### Debug
- `GET /api/raw-events` - View raw events (for debugging parsing)

## Parser Support

Currently supports parsing for:
- **Kotak Bank** SMS
- **UCO Bank** SMS/UPI notifications
- **Google Pay** notifications

Parser can be extended in `backend/parser.py` to support additional banks/UPI apps.

## Development Roadmap

### Phase 1: Backend Core ✅
- [x] Database schema
- [x] Basic API structure
- [x] Event ingestion endpoint
- [x] Transaction parser (Kotak, UCO, GPay)
- [x] Deduplication logic

### Phase 2: Backend Features (In Progress)
- [ ] Rules engine for auto-categorization
- [ ] Google Sheets webhook integration
- [ ] Merchant management API
- [ ] Category management API
- [ ] Transaction editing API
- [ ] Reparse/rebuild utilities

### Phase 3: Android App
- [ ] SMS listener
- [ ] Notification listener
- [ ] Local queue and sync
- [ ] Settings and configuration

### Phase 4: Web Dashboard
- [ ] Authentication
- [ ] Transaction list/filters
- [ ] Merchant management UI
- [ ] Category management UI
- [ ] Charts and analytics
- [ ] Export functionality

### Phase 5: Advanced Features
- [ ] Budgets
- [ ] Alerts
- [ ] Multi-device support
- [ ] Data export/import

## Security Considerations

- Device authentication via API keys
- HTTPS for all communications
- SQLite database encryption (optional)
- Regular backups recommended

## Contributing

This is a personal project, but contributions and suggestions are welcome!

## License

MIT License - Use freely for personal or commercial purposes.

## 📖 Additional Resources

- [Quick Start Guide](docs/QUICKSTART.md) - Detailed setup instructions
- [Architecture Documentation](docs/ARCHITECTURE.md) - System design
- [Development Progress](docs/PROGRESS.md) - Roadmap and status

## Author

Built by ARKA with assistance from ChatGPT

Based on the comprehensive roadmap in `spend_tracker_system_roadmap.pdf`