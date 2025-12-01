import re
from datetime import datetime
from typing import Optional, Dict, Any
from sqlalchemy.orm import Session
from . import models

class TransactionParser:
    """Parse SMS and notification texts to extract transaction details."""
    
    @staticmethod
    def parse_kotak_sms(raw_text: str) -> Optional[Dict[str, Any]]:
        """
        Parse Kotak Bank SMS messages.
        Examples:
        - "Sent Rs.15.00 from Kotak Bank AC X1415 to amitabh10b26.hts21@okicici via UPI Ref no 434750881179"
        - "Rs 123.45 debited from a/c X1415 on 01-Dec-25..."
        """
        result = {}
        
        # Extract amount
        amount_match = re.search(r'Rs\.?\s*(\d+\.?\d*)', raw_text, re.IGNORECASE)
        if amount_match:
            result['amount'] = float(amount_match.group(1))
        
        # Determine direction
        if any(word in raw_text.lower() for word in ['sent', 'debited', 'paid', 'withdrawn']):
            result['direction'] = 'DEBIT'
        elif any(word in raw_text.lower() for word in ['received', 'credited', 'deposited']):
            result['direction'] = 'CREDIT'
        
        # Extract account mask
        account_match = re.search(r'[Aa]/[Cc]\s*([A-Z0-9]+)|AC\s*([A-Z0-9]+)', raw_text)
        if account_match:
            result['account_mask'] = account_match.group(1) or account_match.group(2)
        
        # Extract UPI ID if present
        upi_match = re.search(r'to\s+([\w.]+@[\w]+)', raw_text)
        if upi_match:
            result['raw_merchant_identifier'] = upi_match.group(1)
            result['channel'] = 'UPI'
        
        # Check if it's a UPI transaction
        if 'upi' in raw_text.lower():
            result['channel'] = 'UPI'
        
        result['bank_name'] = 'Kotak'
        
        return result if result else None
    
    @staticmethod
    def parse_uco_sms(raw_text: str) -> Optional[Dict[str, Any]]:
        """
        Parse UCO Bank SMS messages.
        Example: "UCO-UPI/CR/434750881179/amitabh10b26.hts21@okicici/UCO BANK/XX3242/15.00"
        """
        result = {}
        
        # UCO UPI pattern
        uco_pattern = r'UCO-UPI/(CR|DR)/(\d+)/([\w.@]+)/([^/]+)/([^/]+)/([\d.]+)'
        match = re.search(uco_pattern, raw_text)
        
        if match:
            result['direction'] = 'CREDIT' if match.group(1) == 'CR' else 'DEBIT'
            result['raw_merchant_identifier'] = match.group(3)
            result['account_mask'] = match.group(5)
            result['amount'] = float(match.group(6))
            result['channel'] = 'UPI'
            result['bank_name'] = 'UCO'
        
        return result if result else None
    
    @staticmethod
    def parse_gpay_notification(raw_text: str) -> Optional[Dict[str, Any]]:
        """Parse Google Pay notification."""
        result = {}
        
        # Extract amount
        amount_match = re.search(r'₹\s*(\d+\.?\d*)', raw_text)
        if amount_match:
            result['amount'] = float(amount_match.group(1))
        
        # Determine direction
        if any(word in raw_text.lower() for word in ['sent', 'paid']):
            result['direction'] = 'DEBIT'
        elif any(word in raw_text.lower() for word in ['received']):
            result['direction'] = 'CREDIT'
        
        result['channel'] = 'UPI'
        
        return result if result else None
    
    @staticmethod
    def parse_event(raw_event: models.RawEvent) -> Optional[Dict[str, Any]]:
        """Route to appropriate parser based on source."""
        source_lower = raw_event.source_sender.lower()
        
        if 'kotak' in source_lower:
            return TransactionParser.parse_kotak_sms(raw_event.raw_text)
        elif 'uco' in source_lower:
            return TransactionParser.parse_uco_sms(raw_event.raw_text)
        elif 'gpay' in source_lower or 'google' in source_lower:
            return TransactionParser.parse_gpay_notification(raw_event.raw_text)
        
        return None


def process_raw_event(db: Session, raw_event: models.RawEvent) -> Optional[models.Transaction]:
    """Parse raw event and create/update transaction."""
    
    # Parse the event
    parsed_data = TransactionParser.parse_event(raw_event)
    
    if not parsed_data or 'amount' not in parsed_data or 'direction' not in parsed_data:
        # Mark as failed
        raw_event.parsed_status = "FAILED"
        raw_event.error_message = "Could not extract required fields (amount, direction)"
        db.commit()
        return None
    
    # Find or create account
    account = db.query(models.Account).filter(
        models.Account.user_id == raw_event.user_id,
        models.Account.bank_name == parsed_data.get('bank_name', 'Unknown'),
        models.Account.account_mask == parsed_data.get('account_mask', 'Unknown')
    ).first()
    
    if not account:
        account = models.Account(
            user_id=raw_event.user_id,
            bank_name=parsed_data.get('bank_name', 'Unknown'),
            account_mask=parsed_data.get('account_mask', 'Unknown'),
            display_name=f"{parsed_data.get('bank_name', 'Unknown')} {parsed_data.get('account_mask', 'Unknown')}",
            type='SAVINGS',
            is_active=True
        )
        db.add(account)
        db.flush()
    
    # Generate dedupe key
    merchant_key = normalize_merchant_key(parsed_data.get('raw_merchant_identifier', ''))
    dedupe_key = generate_dedupe_key(
        raw_event.user_id,
        account.id,
        parsed_data['direction'],
        parsed_data['amount'],
        raw_event.received_at,
        merchant_key
    )
    
    # Check for duplicate
    existing = db.query(models.Transaction).filter(
        models.Transaction.dedupe_key == dedupe_key
    ).first()
    
    if existing:
        # Link this event to existing transaction
        raw_event.related_transaction_id = existing.id
        raw_event.parsed_status = "PARSED"
        db.commit()
        return existing
    
    # Create new transaction
    transaction = models.Transaction(
        user_id=raw_event.user_id,
        account_id=account.id,
        direction=parsed_data['direction'],
        amount=parsed_data['amount'],
        currency='INR',
        channel=parsed_data.get('channel', 'OTHER'),
        raw_merchant_identifier=parsed_data.get('raw_merchant_identifier'),
        merchant_key=merchant_key,
        transaction_time=raw_event.received_at,
        dedupe_key=dedupe_key
    )
    
    db.add(transaction)
    raw_event.related_transaction_id = transaction.id
    raw_event.parsed_status = "PARSED"
    db.commit()
    db.refresh(transaction)
    
    return transaction


def normalize_merchant_key(raw_merchant: str) -> str:
    """Normalize merchant identifier for matching."""
    if not raw_merchant:
        return ""
    return raw_merchant.lower().strip()


def generate_dedupe_key(user_id: int, account_id: int, direction: str, 
                       amount: float, timestamp: datetime, merchant_key: str) -> str:
    """Generate a unique key for deduplication."""
    # Round timestamp to nearest minute
    rounded_time = timestamp.replace(second=0, microsecond=0)
    
    # Create key from components
    key = f"{user_id}_{account_id}_{direction}_{amount:.2f}_{rounded_time.isoformat()}_{merchant_key}"
    return key
