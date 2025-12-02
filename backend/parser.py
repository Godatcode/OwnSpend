import re
from datetime import datetime
from typing import Optional, Dict, Any
from sqlalchemy.orm import Session
import models

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
    def parse_hdfc_sms(raw_text: str) -> Optional[Dict[str, Any]]:
        """
        Parse HDFC Bank SMS messages.
        Examples:
        - "Rs.500.00 debited from HDFC Bank A/c XX1234 on 01-12-25. Info: UPI/JOHN@UPI. Avl bal: Rs.5000.00"
        - "HDFC Bank: Rs 500.00 credited to A/c XX1234 on 01-12-25 by UPI Ref No 123456789012."
        - "You've done a UPI txn of Rs.100.00 from HDFC A/c XX1234 to JOHN@okaxis. Ref 123456789012"
        - "Alert: INR 1500.00 debited from A/c XX1234 on 01-Dec-25 by ATM-WDL. Avl Bal: INR 5000.00"
        """
        result = {}
        
        # Extract amount - multiple patterns
        amount_patterns = [
            r'Rs\.?\s*(\d+(?:,\d+)*\.?\d*)',
            r'INR\s*(\d+(?:,\d+)*\.?\d*)',
            r'₹\s*(\d+(?:,\d+)*\.?\d*)'
        ]
        for pattern in amount_patterns:
            amount_match = re.search(pattern, raw_text, re.IGNORECASE)
            if amount_match:
                result['amount'] = float(amount_match.group(1).replace(',', ''))
                break
        
        # Determine direction
        if any(word in raw_text.lower() for word in ['debited', 'debit', 'withdrawn', 'sent', 'paid', 'txn of rs']):
            result['direction'] = 'DEBIT'
        elif any(word in raw_text.lower() for word in ['credited', 'credit', 'received', 'deposited']):
            result['direction'] = 'CREDIT'
        
        # Extract account mask
        account_match = re.search(r'A/c\s*(?:no\.?)?\s*[Xx]*(\d{4})|[Xx]{2}(\d{4})', raw_text, re.IGNORECASE)
        if account_match:
            result['account_mask'] = f"XX{account_match.group(1) or account_match.group(2)}"
        
        # Extract UPI ID/merchant
        upi_match = re.search(r'(?:to|from|Info:)\s*(?:UPI/?)?\s*([\w.]+@[\w]+)', raw_text, re.IGNORECASE)
        if upi_match:
            result['raw_merchant_identifier'] = upi_match.group(1)
            result['channel'] = 'UPI'
        
        # Detect channel
        if 'upi' in raw_text.lower():
            result['channel'] = 'UPI'
        elif 'atm' in raw_text.lower():
            result['channel'] = 'ATM'
        elif 'neft' in raw_text.lower():
            result['channel'] = 'NEFT'
        elif 'imps' in raw_text.lower():
            result['channel'] = 'IMPS'
        elif 'pos' in raw_text.lower():
            result['channel'] = 'POS'
        
        result['bank_name'] = 'HDFC'
        
        return result if result else None
    
    @staticmethod
    def parse_icici_sms(raw_text: str) -> Optional[Dict[str, Any]]:
        """
        Parse ICICI Bank SMS messages.
        Examples:
        - "ICICI Bank Acct XX1234 debited for Rs 500.00 on 01-Dec-25; john@upi credited. UPI:123456789012"
        - "ICICI Bank Acct XX1234 credited with Rs 500.00 on 01-Dec-25. IMPS Ref No:123456789012."
        - "Rs.500 sent from ICICI Bank Acc XX1234 to john@okaxis. UPI Ref:123456789012"
        - "Dear Customer, your ICICI Bank Credit Card XX1234 has been used for Rs.500.00 at AMAZON"
        """
        result = {}
        
        # Extract amount
        amount_patterns = [
            r'Rs\.?\s*(\d+(?:,\d+)*\.?\d*)',
            r'INR\s*(\d+(?:,\d+)*\.?\d*)',
            r'for\s+(\d+(?:,\d+)*\.?\d*)',
            r'with\s+Rs\.?\s*(\d+(?:,\d+)*\.?\d*)'
        ]
        for pattern in amount_patterns:
            amount_match = re.search(pattern, raw_text, re.IGNORECASE)
            if amount_match:
                result['amount'] = float(amount_match.group(1).replace(',', ''))
                break
        
        # Determine direction
        if any(word in raw_text.lower() for word in ['debited', 'sent', 'used for', 'paid']):
            result['direction'] = 'DEBIT'
        elif any(word in raw_text.lower() for word in ['credited', 'received', 'deposited']):
            result['direction'] = 'CREDIT'
        
        # Extract account mask
        account_match = re.search(r'(?:Acct?|Acc|A/c|Card)\s*[Xx]*(\d{4})', raw_text, re.IGNORECASE)
        if account_match:
            result['account_mask'] = f"XX{account_match.group(1)}"
        
        # Extract UPI ID/merchant
        upi_match = re.search(r'(?:to|from)\s+([\w.]+@[\w]+)', raw_text, re.IGNORECASE)
        if upi_match:
            result['raw_merchant_identifier'] = upi_match.group(1)
            result['channel'] = 'UPI'
        
        # Extract merchant name for card transactions
        merchant_match = re.search(r'at\s+([A-Za-z0-9\s]+?)(?:\s+on|\.|$)', raw_text)
        if merchant_match and 'raw_merchant_identifier' not in result:
            result['raw_merchant_identifier'] = merchant_match.group(1).strip()
        
        # Detect channel
        if 'upi' in raw_text.lower():
            result['channel'] = 'UPI'
        elif 'imps' in raw_text.lower():
            result['channel'] = 'IMPS'
        elif 'neft' in raw_text.lower():
            result['channel'] = 'NEFT'
        elif 'credit card' in raw_text.lower():
            result['channel'] = 'CARD'
        elif 'debit card' in raw_text.lower():
            result['channel'] = 'CARD'
        
        result['bank_name'] = 'ICICI'
        
        return result if result else None
    
    @staticmethod
    def parse_sbi_sms(raw_text: str) -> Optional[Dict[str, Any]]:
        """
        Parse SBI (State Bank of India) SMS messages.
        Examples:
        - "Your a/c no. XXXXXXXX1234 is debited for Rs.500.00 on 01Dec25 by transfer to VPA john@upi (UPI Ref No 123456789012)"
        - "Your a/c XX1234 credited by Rs.500.00 on 01Dec25 by transfer from VPA john@upi (UPI Ref No 123456789012)"
        - "SBI UPI: A/c X1234 debited Rs.500.00 on 01Dec Ref 123456789012 credited to VPA john@upi"
        - "ATM-SBI: Rs.500 withdrawn from A/c XX1234 on 01Dec25. Avl bal: Rs.5000.00"
        """
        result = {}
        
        # Extract amount
        amount_patterns = [
            r'Rs\.?\s*(\d+(?:,\d+)*\.?\d*)',
            r'INR\s*(\d+(?:,\d+)*\.?\d*)'
        ]
        for pattern in amount_patterns:
            amount_match = re.search(pattern, raw_text, re.IGNORECASE)
            if amount_match:
                result['amount'] = float(amount_match.group(1).replace(',', ''))
                break
        
        # Determine direction
        if any(word in raw_text.lower() for word in ['debited', 'withdrawn', 'sent', 'paid', 'debit']):
            result['direction'] = 'DEBIT'
        elif any(word in raw_text.lower() for word in ['credited', 'received', 'deposited', 'credit']):
            result['direction'] = 'CREDIT'
        
        # Extract account mask
        account_match = re.search(r'(?:a/c\s*(?:no\.?)?\s*)?[Xx]+(\d{4})', raw_text, re.IGNORECASE)
        if account_match:
            result['account_mask'] = f"XX{account_match.group(1)}"
        
        # Extract VPA/UPI ID
        vpa_match = re.search(r'(?:VPA|to|from)\s+([\w.]+@[\w]+)', raw_text, re.IGNORECASE)
        if vpa_match:
            result['raw_merchant_identifier'] = vpa_match.group(1)
            result['channel'] = 'UPI'
        
        # Detect channel
        if 'upi' in raw_text.lower() or 'vpa' in raw_text.lower():
            result['channel'] = 'UPI'
        elif 'atm' in raw_text.lower():
            result['channel'] = 'ATM'
        elif 'neft' in raw_text.lower():
            result['channel'] = 'NEFT'
        elif 'imps' in raw_text.lower():
            result['channel'] = 'IMPS'
        
        result['bank_name'] = 'SBI'
        
        return result if result else None
    
    @staticmethod
    def parse_axis_sms(raw_text: str) -> Optional[Dict[str, Any]]:
        """
        Parse Axis Bank SMS messages.
        Examples:
        - "Rs.500.00 debited from A/c no. XX1234 on 01-12-2025. Info- UPI/john@upi/UPI. Avl Bal- Rs.5000.00"
        - "Your Axis Bank A/c XX1234 is credited with Rs.500.00 on 01-Dec-25. Info: UPI/john@upi."
        - "INR 500.00 spent on Axis Bank Credit Card XX1234 at AMAZON on 01-Dec-25. Avl Limit: INR 50000"
        - "Rs 500 transferred from Axis Bank A/c XX1234 to john@okaxis. UPI Ref: 123456789012"
        """
        result = {}
        
        # Extract amount
        amount_patterns = [
            r'Rs\.?\s*(\d+(?:,\d+)*\.?\d*)',
            r'INR\s*(\d+(?:,\d+)*\.?\d*)'
        ]
        for pattern in amount_patterns:
            amount_match = re.search(pattern, raw_text, re.IGNORECASE)
            if amount_match:
                result['amount'] = float(amount_match.group(1).replace(',', ''))
                break
        
        # Determine direction
        if any(word in raw_text.lower() for word in ['debited', 'spent', 'transferred', 'sent', 'paid']):
            result['direction'] = 'DEBIT'
        elif any(word in raw_text.lower() for word in ['credited', 'received', 'deposited']):
            result['direction'] = 'CREDIT'
        
        # Extract account mask
        account_match = re.search(r'(?:A/c\s*(?:no\.?)?\s*|Card\s*)[Xx]*(\d{4})', raw_text, re.IGNORECASE)
        if account_match:
            result['account_mask'] = f"XX{account_match.group(1)}"
        
        # Extract UPI ID from Info field or direct mention
        upi_match = re.search(r'(?:Info[:\-]?\s*UPI/|to\s+)([\w.]+@[\w]+)', raw_text, re.IGNORECASE)
        if upi_match:
            result['raw_merchant_identifier'] = upi_match.group(1)
            result['channel'] = 'UPI'
        
        # Extract merchant name for card transactions
        merchant_match = re.search(r'at\s+([A-Za-z0-9\s]+?)\s+on', raw_text)
        if merchant_match and 'raw_merchant_identifier' not in result:
            result['raw_merchant_identifier'] = merchant_match.group(1).strip()
        
        # Detect channel
        if 'upi' in raw_text.lower():
            result['channel'] = 'UPI'
        elif 'credit card' in raw_text.lower():
            result['channel'] = 'CARD'
        elif 'debit card' in raw_text.lower():
            result['channel'] = 'CARD'
        elif 'neft' in raw_text.lower():
            result['channel'] = 'NEFT'
        elif 'imps' in raw_text.lower():
            result['channel'] = 'IMPS'
        elif 'atm' in raw_text.lower():
            result['channel'] = 'ATM'
        
        result['bank_name'] = 'Axis'
        
        return result if result else None
    
    @staticmethod
    def parse_phonepe_notification(raw_text: str) -> Optional[Dict[str, Any]]:
        """
        Parse PhonePe notification messages.
        Examples:
        - "Paid ₹500 to john@upi from HDFC XX1234"
        - "Received ₹500 from john@upi to HDFC XX1234"
        - "Payment of ₹500 to AMAZON successful"
        """
        result = {}
        
        # Extract amount
        amount_match = re.search(r'₹\s*(\d+(?:,\d+)*\.?\d*)', raw_text)
        if amount_match:
            result['amount'] = float(amount_match.group(1).replace(',', ''))
        
        # Determine direction
        if any(word in raw_text.lower() for word in ['paid', 'payment of', 'sent']):
            result['direction'] = 'DEBIT'
        elif any(word in raw_text.lower() for word in ['received']):
            result['direction'] = 'CREDIT'
        
        # Extract UPI ID or merchant
        upi_match = re.search(r'(?:to|from)\s+([\w.]+@[\w]+)', raw_text, re.IGNORECASE)
        if upi_match:
            result['raw_merchant_identifier'] = upi_match.group(1)
        else:
            # Try to extract merchant name
            merchant_match = re.search(r'(?:to|from)\s+([A-Za-z0-9\s]+?)(?:\s+from|\s+to|\s+successful|$)', raw_text, re.IGNORECASE)
            if merchant_match:
                result['raw_merchant_identifier'] = merchant_match.group(1).strip()
        
        # Extract account mask
        account_match = re.search(r'[Xx]{2}(\d{4})', raw_text)
        if account_match:
            result['account_mask'] = f"XX{account_match.group(1)}"
        
        result['channel'] = 'UPI'
        
        return result if result else None
    
    @staticmethod
    def parse_paytm_notification(raw_text: str) -> Optional[Dict[str, Any]]:
        """
        Parse Paytm notification messages.
        Examples:
        - "You paid Rs.500 to john@paytm"
        - "Rs.500 received from john@paytm"
        - "Paid Rs.500 at AMAZON using Paytm"
        """
        result = {}
        
        # Extract amount
        amount_match = re.search(r'Rs\.?\s*(\d+(?:,\d+)*\.?\d*)', raw_text, re.IGNORECASE)
        if amount_match:
            result['amount'] = float(amount_match.group(1).replace(',', ''))
        
        # Determine direction
        if any(word in raw_text.lower() for word in ['paid', 'sent', 'you paid']):
            result['direction'] = 'DEBIT'
        elif any(word in raw_text.lower() for word in ['received']):
            result['direction'] = 'CREDIT'
        
        # Extract UPI ID or merchant
        upi_match = re.search(r'(?:to|from)\s+([\w.]+@[\w]+)', raw_text, re.IGNORECASE)
        if upi_match:
            result['raw_merchant_identifier'] = upi_match.group(1)
        else:
            # Try to extract merchant name
            merchant_match = re.search(r'at\s+([A-Za-z0-9\s]+?)(?:\s+using|$)', raw_text, re.IGNORECASE)
            if merchant_match:
                result['raw_merchant_identifier'] = merchant_match.group(1).strip()
        
        result['channel'] = 'UPI'
        
        return result if result else None
    
    @staticmethod
    def parse_generic_bank_sms(raw_text: str) -> Optional[Dict[str, Any]]:
        """
        Generic parser for unknown banks. Tries to extract basic info.
        Works as a fallback for less common banks.
        """
        result = {}
        
        # Extract amount - try multiple patterns
        amount_patterns = [
            r'Rs\.?\s*(\d+(?:,\d+)*\.?\d*)',
            r'INR\s*(\d+(?:,\d+)*\.?\d*)',
            r'₹\s*(\d+(?:,\d+)*\.?\d*)',
            r'(?:debited|credited|paid|received)\s*(?:Rs\.?|INR)?\s*(\d+(?:,\d+)*\.?\d*)'
        ]
        for pattern in amount_patterns:
            amount_match = re.search(pattern, raw_text, re.IGNORECASE)
            if amount_match:
                result['amount'] = float(amount_match.group(1).replace(',', ''))
                break
        
        # Determine direction
        debit_words = ['debited', 'debit', 'withdrawn', 'sent', 'paid', 'spent', 'transferred']
        credit_words = ['credited', 'credit', 'received', 'deposited']
        
        text_lower = raw_text.lower()
        if any(word in text_lower for word in debit_words):
            result['direction'] = 'DEBIT'
        elif any(word in text_lower for word in credit_words):
            result['direction'] = 'CREDIT'
        
        # Extract account mask
        account_match = re.search(r'(?:A/c|Acct?|Account|Card)\s*(?:no\.?)?\s*[Xx]*(\d{4})', raw_text, re.IGNORECASE)
        if account_match:
            result['account_mask'] = f"XX{account_match.group(1)}"
        
        # Extract UPI ID if present
        upi_match = re.search(r'([\w.]+@[\w]+)', raw_text)
        if upi_match:
            result['raw_merchant_identifier'] = upi_match.group(1)
            result['channel'] = 'UPI'
        
        # Detect channel
        if 'upi' in text_lower:
            result['channel'] = 'UPI'
        elif 'atm' in text_lower:
            result['channel'] = 'ATM'
        elif 'neft' in text_lower:
            result['channel'] = 'NEFT'
        elif 'imps' in text_lower:
            result['channel'] = 'IMPS'
        elif 'card' in text_lower:
            result['channel'] = 'CARD'
        
        result['bank_name'] = 'Unknown'
        
        return result if result else None
    
    @staticmethod
    def parse_event(raw_event: models.RawEvent) -> Optional[Dict[str, Any]]:
        """Route to appropriate parser based on source."""
        source_lower = raw_event.source_sender.lower()
        raw_text_lower = raw_event.raw_text.lower()
        
        # Bank-specific parsers
        if 'kotak' in source_lower or 'kotak' in raw_text_lower:
            return TransactionParser.parse_kotak_sms(raw_event.raw_text)
        elif 'uco' in source_lower or 'uco-upi' in raw_text_lower:
            return TransactionParser.parse_uco_sms(raw_event.raw_text)
        elif 'hdfc' in source_lower or 'hdfc' in raw_text_lower:
            return TransactionParser.parse_hdfc_sms(raw_event.raw_text)
        elif 'icici' in source_lower or 'icici' in raw_text_lower:
            return TransactionParser.parse_icici_sms(raw_event.raw_text)
        elif 'sbi' in source_lower or ('sbi' in raw_text_lower and 'possible' not in raw_text_lower):
            return TransactionParser.parse_sbi_sms(raw_event.raw_text)
        elif 'axis' in source_lower or 'axis' in raw_text_lower:
            return TransactionParser.parse_axis_sms(raw_event.raw_text)
        
        # UPI app parsers
        elif 'gpay' in source_lower or 'google' in source_lower:
            return TransactionParser.parse_gpay_notification(raw_event.raw_text)
        elif 'phonepe' in source_lower:
            return TransactionParser.parse_phonepe_notification(raw_event.raw_text)
        elif 'paytm' in source_lower:
            return TransactionParser.parse_paytm_notification(raw_event.raw_text)
        
        # Fallback to generic parser
        else:
            return TransactionParser.parse_generic_bank_sms(raw_event.raw_text)


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
    
    # Apply rules engine for auto-categorization
    try:
        from .rules_engine import RulesEngine
        RulesEngine.apply_rules(db, transaction)
    except Exception as e:
        # Don't fail transaction creation if rules fail
        print(f"Rules engine error: {e}")
    
    # Sync to Google Sheets
    try:
        from .sheets_sync import get_sheets_sync
        sheets_sync = get_sheets_sync()
        sheets_sync.sync_transaction(db, transaction)
    except Exception as e:
        # Don't fail transaction creation if sheets sync fails
        print(f"Google Sheets sync error: {e}")
    
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
