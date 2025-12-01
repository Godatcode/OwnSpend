"""
Rules Engine for auto-categorization and merchant mapping.
"""
from typing import List, Optional
from sqlalchemy.orm import Session
from . import models
import re


class RulesEngine:
    """Apply rules to transactions for auto-categorization."""
    
    @staticmethod
    def apply_rules(db: Session, transaction: models.Transaction) -> bool:
        """
        Apply all active rules to a transaction.
        Returns True if any rules were applied.
        """
        # Get all active rules for this user, ordered by priority
        rules = db.query(models.Rule).filter(
            models.Rule.user_id == transaction.user_id,
            models.Rule.is_active == True
        ).order_by(models.Rule.priority.asc()).all()
        
        applied_count = 0
        
        for rule in rules:
            if RulesEngine._rule_matches(transaction, rule):
                if RulesEngine._apply_rule_action(db, transaction, rule):
                    applied_count += 1
        
        if applied_count > 0:
            db.commit()
            db.refresh(transaction)
            return True
        
        return False
    
    @staticmethod
    def _rule_matches(transaction: models.Transaction, rule: models.Rule) -> bool:
        """Check if a rule matches a transaction."""
        match_type = rule.match_type
        match_value = rule.match_value
        
        if match_type == "MERCHANT_KEY":
            # Exact match on merchant key
            return transaction.merchant_key == match_value
        
        elif match_type == "MERCHANT_KEY_CONTAINS":
            # Partial match on merchant key
            return match_value.lower() in (transaction.merchant_key or "").lower()
        
        elif match_type == "TEXT_CONTAINS":
            # Match in description or raw merchant identifier
            text = f"{transaction.description or ''} {transaction.raw_merchant_identifier or ''}".lower()
            return match_value.lower() in text
        
        elif match_type == "UPI_ID_PREFIX":
            # Match UPI ID prefix (e.g., "amitabh" matches "amitabh10b26.hts21@okicici")
            if transaction.raw_merchant_identifier:
                return transaction.raw_merchant_identifier.lower().startswith(match_value.lower())
        
        elif match_type == "UPI_ID_SUFFIX":
            # Match UPI ID suffix (e.g., "@okicici" matches "amitabh10b26.hts21@okicici")
            if transaction.raw_merchant_identifier:
                return transaction.raw_merchant_identifier.lower().endswith(match_value.lower())
        
        elif match_type == "AMOUNT_EQUALS":
            # Exact amount match
            try:
                target_amount = float(match_value)
                return abs(transaction.amount - target_amount) < 0.01
            except ValueError:
                return False
        
        elif match_type == "AMOUNT_RANGE":
            # Amount in range (format: "min-max", e.g., "100-500")
            try:
                min_amt, max_amt = match_value.split("-")
                return float(min_amt) <= transaction.amount <= float(max_amt)
            except (ValueError, AttributeError):
                return False
        
        elif match_type == "CHANNEL":
            # Match payment channel
            return transaction.channel == match_value
        
        elif match_type == "DIRECTION":
            # Match transaction direction
            return transaction.direction == match_value
        
        elif match_type == "ACCOUNT_ID":
            # Match specific account
            try:
                return transaction.account_id == int(match_value)
            except ValueError:
                return False
        
        return False
    
    @staticmethod
    def _apply_rule_action(db: Session, transaction: models.Transaction, rule: models.Rule) -> bool:
        """Apply rule action to transaction. Returns True if applied."""
        action_type = rule.action_type
        action_value = rule.action_value
        
        # Check manual override flags to avoid overwriting user edits
        # Bit flags: 1=merchant, 2=category, 4=internal_transfer
        
        if action_type == "SET_MERCHANT":
            if transaction.manual_override_flags & 1:  # Merchant manually set
                return False
            
            # Find or create merchant
            merchant = db.query(models.Merchant).filter(
                models.Merchant.id == int(action_value)
            ).first()
            
            if merchant:
                transaction.merchant_id = merchant.id
                return True
        
        elif action_type == "SET_MERCHANT_BY_KEY":
            if transaction.manual_override_flags & 1:
                return False
            
            # Find merchant by key
            merchant = db.query(models.Merchant).filter(
                models.Merchant.merchant_key == action_value
            ).first()
            
            if merchant:
                transaction.merchant_id = merchant.id
                return True
        
        elif action_type == "SET_CATEGORY":
            if transaction.manual_override_flags & 2:  # Category manually set
                return False
            
            try:
                category_id = int(action_value)
                category = db.query(models.Category).filter(
                    models.Category.id == category_id
                ).first()
                
                if category:
                    transaction.category_id = category_id
                    return True
            except ValueError:
                return False
        
        elif action_type == "SET_CATEGORY_BY_NAME":
            if transaction.manual_override_flags & 2:
                return False
            
            category = db.query(models.Category).filter(
                models.Category.name == action_value
            ).first()
            
            if category:
                transaction.category_id = category.id
                return True
        
        elif action_type == "MARK_INTERNAL":
            if transaction.manual_override_flags & 4:  # Internal flag manually set
                return False
            
            transaction.is_internal_transfer = True
            return True
        
        elif action_type == "SET_DESCRIPTION":
            # Always allow description updates (not protected by override flags)
            transaction.description = action_value
            return True
        
        return False


def create_default_rules(db: Session, user_id: int):
    """Create some sensible default rules."""
    
    default_rules = [
        # Internal transfers
        {
            "match_type": "TEXT_CONTAINS",
            "match_value": "transfer",
            "action_type": "SET_CATEGORY_BY_NAME",
            "action_value": "Transfer",
            "priority": 10
        },
        # Salary
        {
            "match_type": "TEXT_CONTAINS",
            "match_value": "salary",
            "action_type": "SET_CATEGORY_BY_NAME",
            "action_value": "Salary",
            "priority": 20
        },
        # Food delivery - Zomato
        {
            "match_type": "TEXT_CONTAINS",
            "match_value": "zomato",
            "action_type": "SET_CATEGORY_BY_NAME",
            "action_value": "Food & Dining",
            "priority": 30
        },
        # Food delivery - Swiggy
        {
            "match_type": "TEXT_CONTAINS",
            "match_value": "swiggy",
            "action_type": "SET_CATEGORY_BY_NAME",
            "action_value": "Food & Dining",
            "priority": 30
        },
        # Groceries
        {
            "match_type": "TEXT_CONTAINS",
            "match_value": "grocery",
            "action_type": "SET_CATEGORY_BY_NAME",
            "action_value": "Groceries",
            "priority": 40
        },
        # Amazon
        {
            "match_type": "TEXT_CONTAINS",
            "match_value": "amazon",
            "action_type": "SET_CATEGORY_BY_NAME",
            "action_value": "Shopping",
            "priority": 50
        },
        # Flipkart
        {
            "match_type": "TEXT_CONTAINS",
            "match_value": "flipkart",
            "action_type": "SET_CATEGORY_BY_NAME",
            "action_value": "Shopping",
            "priority": 50
        },
        # Uber/Ola
        {
            "match_type": "TEXT_CONTAINS",
            "match_value": "uber",
            "action_type": "SET_CATEGORY_BY_NAME",
            "action_value": "Transportation",
            "priority": 60
        },
        {
            "match_type": "TEXT_CONTAINS",
            "match_value": "ola",
            "action_type": "SET_CATEGORY_BY_NAME",
            "action_value": "Transportation",
            "priority": 60
        },
    ]
    
    for rule_data in default_rules:
        rule = models.Rule(
            user_id=user_id,
            match_type=rule_data["match_type"],
            match_value=rule_data["match_value"],
            action_type=rule_data["action_type"],
            action_value=rule_data["action_value"],
            priority=rule_data["priority"],
            is_active=True
        )
        db.add(rule)
    
    db.commit()
