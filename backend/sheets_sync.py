"""
Google Sheets integration for mirroring transactions.
"""
import requests
from typing import Optional, Dict, Any
from datetime import datetime
from sqlalchemy.orm import Session
from . import models


class GoogleSheetsSync:
    """Handle syncing transactions to Google Sheets."""
    
    def __init__(self, webhook_url: Optional[str] = None):
        """
        Initialize with Google Apps Script webhook URL.
        Get this URL after deploying the Apps Script.
        """
        self.webhook_url = webhook_url or self._get_webhook_url_from_env()
    
    def _get_webhook_url_from_env(self) -> Optional[str]:
        """Get webhook URL from environment variables."""
        import os
        from dotenv import load_dotenv
        
        # Load .env from the backend directory
        env_path = os.path.join(os.path.dirname(os.path.abspath(__file__)), '.env')
        load_dotenv(env_path)
        
        return os.getenv('GOOGLE_SHEETS_WEBHOOK_URL')
    
    def sync_transaction(self, db: Session, transaction: models.Transaction) -> bool:
        """
        Sync a single transaction to Google Sheets.
        Returns True if successful, False otherwise.
        """
        if not self.webhook_url:
            print("Warning: Google Sheets webhook URL not configured")
            return False
        
        try:
            # Fetch related data
            account = db.query(models.Account).filter(
                models.Account.id == transaction.account_id
            ).first()
            
            merchant = None
            if transaction.merchant_id:
                merchant = db.query(models.Merchant).filter(
                    models.Merchant.id == transaction.merchant_id
                ).first()
            
            category = None
            if transaction.category_id:
                category = db.query(models.Category).filter(
                    models.Category.id == transaction.category_id
                ).first()
            
            # Build payload for Google Sheets
            payload = {
                "transaction_id": transaction.id,
                "transaction_time": transaction.transaction_time.isoformat() if transaction.transaction_time else "",
                "ingested_at": transaction.ingested_at.isoformat() if transaction.ingested_at else "",
                "amount": float(transaction.amount),
                "direction": transaction.direction,
                "channel": transaction.channel,
                "account_name": account.display_name if account else "Unknown",
                "bank_name": account.bank_name if account else "",
                "account_mask": account.account_mask if account else "",
                "merchant_display_name": merchant.display_name if merchant else "",
                "category_name": category.name if category else "",
                "raw_merchant_identifier": transaction.raw_merchant_identifier or "",
                "description": transaction.description or "",
                "is_internal_transfer": transaction.is_internal_transfer,
                "dedupe_key": transaction.dedupe_key
            }
            
            # Send to Google Sheets webhook
            response = requests.post(
                self.webhook_url,
                json=payload,
                timeout=10
            )
            
            if response.status_code == 200:
                return True
            else:
                print(f"Google Sheets sync failed: {response.status_code} - {response.text}")
                return False
                
        except Exception as e:
            print(f"Error syncing to Google Sheets: {e}")
            return False
    
    def sync_all_transactions(self, db: Session) -> Dict[str, Any]:
        """
        Sync all transactions to Google Sheets (full rebuild).
        Returns summary of sync operation.
        """
        if not self.webhook_url:
            return {
                "success": False,
                "error": "Google Sheets webhook URL not configured"
            }
        
        transactions = db.query(models.Transaction).order_by(
            models.Transaction.transaction_time.asc()
        ).all()
        
        success_count = 0
        failed_count = 0
        
        for transaction in transactions:
            if self.sync_transaction(db, transaction):
                success_count += 1
            else:
                failed_count += 1
        
        return {
            "success": True,
            "total_transactions": len(transactions),
            "synced": success_count,
            "failed": failed_count
        }


# Singleton instance
_sheets_sync = None

def get_sheets_sync() -> GoogleSheetsSync:
    """Get or create GoogleSheetsSync instance."""
    global _sheets_sync
    if _sheets_sync is None:
        _sheets_sync = GoogleSheetsSync()
    return _sheets_sync
