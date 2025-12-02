#!/usr/bin/env python3
"""Test Google Sheets sync manually."""

import sys
sys.path.insert(0, '/Users/arkaghosh/Documents/GitHub/OwnSpend/backend')

from database import SessionLocal
import sheets_sync

def main():
    db = SessionLocal()
    try:
        print("🔄 Starting Google Sheets sync...")
        sync_instance = sheets_sync.get_sheets_sync()
        print(f"📝 Webhook URL: {sync_instance.webhook_url}")
        
        result = sync_instance.sync_all_transactions(db)
        print(f"\n✅ Sync complete!")
        print(f"   Total transactions: {result.get('total_transactions', 0)}")
        print(f"   Synced successfully: {result.get('synced', 0)}")
        print(f"   Failed: {result.get('failed', 0)}")
        
        if not result.get('success'):
            print(f"\n❌ Error: {result.get('error')}")
            
    finally:
        db.close()

if __name__ == "__main__":
    main()
