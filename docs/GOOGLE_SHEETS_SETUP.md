# Google Sheets Integration Setup Guide

## Overview

This guide will help you set up Google Sheets integration so all your transactions are automatically mirrored to a Google Sheet for easy viewing and analysis.

## Prerequisites

- Google account
- Backend server running
- Basic familiarity with Google Sheets

---

## Step 1: Create a New Google Sheet

1. Go to [Google Sheets](https://sheets.google.com)
2. Click **"Blank"** to create a new spreadsheet
3. Rename it to something like "OwnSpend Transactions"

---

## Step 2: Open Apps Script Editor

1. In your Google Sheet, click **Extensions** → **Apps Script**
2. This will open the Google Apps Script editor in a new tab
3. Delete any existing code in the editor

---

## Step 3: Add the Script

1. Copy the entire contents of `backend/google_sheets_script.js`
2. Paste it into the Apps Script editor (Code.gs)
3. **Optional**: Update the `SHEET_NAME` constant if you want to use a different sheet name
   ```javascript
   const SHEET_NAME = 'Transactions';  // Change this if needed
   ```
4. Click the **Save** icon (💾) or press `Ctrl+S` / `Cmd+S`
5. Name your project (e.g., "OwnSpend Integration")

---

## Step 4: Deploy as Web App

1. In the Apps Script editor, click **Deploy** → **New deployment**
2. Click the gear icon (⚙️) next to "Select type"
3. Choose **Web app**
4. Configure the deployment:
   - **Description**: "OwnSpend Webhook v1" (or any description)
   - **Execute as**: Select **"Me"** (your Google account)
   - **Who has access**: Select **"Anyone"**
     - ⚠️ Don't worry, this doesn't make your data public. The URL is secret and acts as the authentication
5. Click **Deploy**
6. You may need to authorize the script:
   - Click **Authorize access**
   - Choose your Google account
   - Click **Advanced** → **Go to [Project Name] (unsafe)**
   - Click **Allow**
7. **Copy the Web App URL** that appears - you'll need this!
   - It looks like: `https://script.google.com/macros/s/XXXXXXXX/exec`

---

## Step 5: Configure Backend

1. Open `backend/.env` file (create it if it doesn't exist)
2. Add the webhook URL:
   ```env
   GOOGLE_SHEETS_WEBHOOK_URL=https://script.google.com/macros/s/XXXXXXXX/exec
   ```
3. Replace `XXXXXXXX` with your actual script ID from the URL you copied
4. Save the file

**If `.env` doesn't exist, you can copy from the example:**
```bash
cd backend
cp .env.example .env
# Then edit .env with your webhook URL
```

---

## Step 6: Initialize the Sheet

Back in Google Sheets, you have two options:

### Option A: Let the Script Initialize Automatically
- Just send your first transaction and the sheet will auto-initialize with headers

### Option B: Initialize Manually
1. In the Apps Script editor, select the `setupSheet` function from the dropdown
2. Click **Run** (▶️)
3. Go back to your Google Sheet - you should see formatted headers

---

## Step 7: Test the Integration

### Test from Apps Script (Optional)
1. In Apps Script editor, select `testAddTransaction` function
2. Click **Run** (▶️)
3. Check your Google Sheet - you should see a test transaction

### Test from Backend
1. Make sure your backend is running
2. Send a test transaction or use the test script:
   ```bash
   cd backend
   python test_ingestion.py
   ```
3. Check your Google Sheet - transactions should appear automatically!

---

## Verification

✅ **Your integration is working if:**
- New transactions appear in Google Sheet automatically
- Amounts are color-coded (red for DEBIT, green for CREDIT)
- All columns are populated with transaction details

---

## Column Reference

Your Google Sheet will have these columns:

| Column | Description |
|--------|-------------|
| Transaction ID | Unique identifier (UUID) |
| Transaction Time | When the transaction occurred |
| Ingested At | When backend received it |
| Amount | Transaction amount |
| Direction | DEBIT or CREDIT |
| Channel | UPI, CARD, NETBANKING, etc. |
| Account Name | Your account display name |
| Bank Name | Bank name |
| Account Mask | Last few digits of account |
| Merchant | Merchant display name |
| Category | Transaction category |
| Raw Merchant ID | UPI ID or merchant identifier |
| Description | Transaction description |
| Internal Transfer | Yes/No |
| Dedupe Key | Key used for deduplication |

---

## Backend API Endpoints

Once configured, you can use these endpoints:

### Check Configuration
```bash
GET http://localhost:8000/api/admin/sheets/config
```

### Sync All Transactions (Full Rebuild)
```bash
POST http://localhost:8000/api/admin/sheets/sync-all
```

### Sync Single Transaction
```bash
POST http://localhost:8000/api/admin/sheets/sync-transaction/{transaction_id}
```

---

## Troubleshooting

### "Webhook URL not configured" Error
- Make sure `GOOGLE_SHEETS_WEBHOOK_URL` is set in `backend/.env`
- Restart your backend server after updating `.env`

### Transactions Not Appearing in Sheet
1. Check if webhook URL is correct in `.env`
2. Check backend logs for errors
3. Test the webhook URL in browser - it should return JSON
4. Make sure the Apps Script is deployed as "Anyone" can access

### Authorization Issues
- Re-deploy the Apps Script
- Make sure you clicked "Allow" when authorizing
- Check that "Execute as" is set to "Me"

### Duplicate Transactions
- The backend has deduplication, but if you manually sync multiple times, you'll see duplicates
- Use the `clearTransactions()` function in Apps Script and then run sync-all

---

## Advanced Usage

### Clear Sheet and Rebuild
```javascript
// In Apps Script, run:
clearTransactions();  // Removes all data except headers
```

Then from backend:
```bash
POST http://localhost:8000/api/admin/sheets/sync-all
```

### Custom Sheet Name
Edit the script:
```javascript
const SHEET_NAME = 'MyCustomSheetName';
```
Then re-deploy.

### Multiple Sheets (Future)
You can modify the script to write to different sheets based on:
- Date (monthly sheets)
- Account
- Category
- Direction (Income vs Expenses)

---

## Security Notes

⚠️ **Important:**
- The webhook URL acts as your "password" - keep it secret
- Don't share the URL publicly
- Anyone with the URL can write to your sheet
- The sheet can be shared normally with Google Sheets permissions

🔒 **To Revoke Access:**
1. Go to Apps Script editor
2. Click Deploy → Manage deployments
3. Click the archive icon (📦) to disable the deployment
4. Or create a new deployment with a new URL

---

## What Happens Automatically

Once configured, transactions are automatically synced when:
- ✅ New transaction is ingested from Android app
- ✅ Transaction is updated via API
- ✅ Admin triggers sync-all or sync-transaction

❌ Sync does NOT happen when:
- Rules are re-applied (use manual sync after)
- Database is directly modified (use manual sync)

---

## Next Steps

After setup:
1. Test with `backend/test_ingestion.py` or `test_rules_engine.py`
2. Watch transactions appear in your sheet in real-time
3. Use Google Sheets features:
   - Filters
   - Pivot tables
   - Charts
   - Conditional formatting
4. Share the sheet with family members (view-only recommended)

---

**Need Help?** Check backend logs for error messages related to Google Sheets sync.
