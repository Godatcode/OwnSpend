from fastapi import FastAPI, Depends, HTTPException, Header
from fastapi.middleware.cors import CORSMiddleware
from sqlalchemy.orm import Session
from datetime import datetime
from typing import Optional, List

from .database import engine, Base, get_db
from . import models, schemas
from .parser import process_raw_event

# Create tables
Base.metadata.create_all(bind=engine)

app = FastAPI(title="OwnSpend API", version="1.0")

# Enable CORS for frontend
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # Configure this properly in production
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

@app.get("/")
def read_root():
    return {"message": "Welcome to OwnSpend API", "version": "1.0"}

@app.get("/health")
def health_check():
    return {"status": "healthy", "timestamp": datetime.now().isoformat()}

# Device authentication helper
def authenticate_device(api_key: str = Header(...), db: Session = Depends(get_db)) -> models.Device:
    """Authenticate device using API key."""
    device = db.query(models.Device).filter(
        models.Device.api_key == api_key,
        models.Device.is_active == True
    ).first()
    
    if not device:
        raise HTTPException(status_code=401, detail="Invalid or inactive device API key")
    
    # Update last seen
    device.last_seen_at = datetime.now()
    db.commit()
    
    return device

@app.post("/api/events/ingest")
def ingest_event(
    event: schemas.RawEventCreate,
    device: models.Device = Depends(authenticate_device),
    db: Session = Depends(get_db)
):
    """Receive raw events from Android app."""
    
    # Create raw event
    raw_event = models.RawEvent(
        user_id=device.user_id,
        device_id=device.id,
        source_type=event.source_type,
        source_sender=event.source_sender,
        raw_text=event.raw_text,
        received_at=event.device_timestamp,
        parsed_status="PENDING"
    )
    
    db.add(raw_event)
    db.commit()
    db.refresh(raw_event)
    
    # Process the event (parse and create transaction)
    try:
        transaction = process_raw_event(db, raw_event)
        
        return {
            "status": "success",
            "raw_event_id": raw_event.id,
            "transaction_id": transaction.id if transaction else None,
            "parsed": transaction is not None
        }
    except Exception as e:
        raw_event.parsed_status = "FAILED"
        raw_event.error_message = str(e)
        db.commit()
        
        return {
            "status": "error",
            "raw_event_id": raw_event.id,
            "error": str(e)
        }

@app.get("/api/transactions", response_model=List[schemas.TransactionResponse])
def get_transactions(
    skip: int = 0,
    limit: int = 100,
    direction: Optional[str] = None,
    db: Session = Depends(get_db)
):
    """Get list of transactions with filters."""
    query = db.query(models.Transaction)
    
    if direction:
        query = query.filter(models.Transaction.direction == direction)
    
    transactions = query.order_by(models.Transaction.transaction_time.desc()).offset(skip).limit(limit).all()
    
    # Format response
    result = []
    for t in transactions:
        account = db.query(models.Account).filter(models.Account.id == t.account_id).first()
        merchant = db.query(models.Merchant).filter(models.Merchant.id == t.merchant_id).first() if t.merchant_id else None
        category = db.query(models.Category).filter(models.Category.id == t.category_id).first() if t.category_id else None
        
        result.append(schemas.TransactionResponse(
            id=t.id,
            amount=t.amount,
            direction=t.direction,
            channel=t.channel,
            description=t.description,
            transaction_time=t.transaction_time,
            merchant_display_name=merchant.display_name if merchant else None,
            category_name=category.name if category else None,
            account_name=account.display_name if account else "Unknown"
        ))
    
    return result

@app.get("/api/accounts")
def get_accounts(db: Session = Depends(get_db)):
    """Get all accounts."""
    accounts = db.query(models.Account).all()
    return accounts

@app.get("/api/merchants")
def get_merchants(db: Session = Depends(get_db)):
    """Get all merchants."""
    merchants = db.query(models.Merchant).all()
    return merchants

@app.get("/api/categories")
def get_categories(db: Session = Depends(get_db)):
    """Get all categories."""
    categories = db.query(models.Category).all()
    return categories

@app.get("/api/raw-events")
def get_raw_events(
    status: Optional[str] = None,
    skip: int = 0,
    limit: int = 100,
    db: Session = Depends(get_db)
):
    """Get raw events for debugging."""
    query = db.query(models.RawEvent)
    
    if status:
        query = query.filter(models.RawEvent.parsed_status == status)
    
    events = query.order_by(models.RawEvent.inserted_at.desc()).offset(skip).limit(limit).all()
    return events

# ============================================================================
# RULES MANAGEMENT APIs
# ============================================================================

@app.get("/api/rules")
def get_rules(
    is_active: Optional[bool] = None,
    db: Session = Depends(get_db)
):
    """Get all rules."""
    query = db.query(models.Rule)
    
    if is_active is not None:
        query = query.filter(models.Rule.is_active == is_active)
    
    rules = query.order_by(models.Rule.priority.asc()).all()
    return rules

@app.post("/api/rules")
def create_rule(
    match_type: str,
    match_value: str,
    action_type: str,
    action_value: str,
    priority: int = 100,
    user_id: int = 1,  # TODO: Get from auth
    db: Session = Depends(get_db)
):
    """Create a new rule."""
    rule = models.Rule(
        user_id=user_id,
        match_type=match_type,
        match_value=match_value,
        action_type=action_type,
        action_value=action_value,
        priority=priority,
        is_active=True
    )
    db.add(rule)
    db.commit()
    db.refresh(rule)
    return rule

@app.put("/api/rules/{rule_id}")
def update_rule(
    rule_id: int,
    match_type: Optional[str] = None,
    match_value: Optional[str] = None,
    action_type: Optional[str] = None,
    action_value: Optional[str] = None,
    priority: Optional[int] = None,
    is_active: Optional[bool] = None,
    db: Session = Depends(get_db)
):
    """Update a rule."""
    rule = db.query(models.Rule).filter(models.Rule.id == rule_id).first()
    if not rule:
        raise HTTPException(status_code=404, detail="Rule not found")
    
    if match_type is not None:
        rule.match_type = match_type
    if match_value is not None:
        rule.match_value = match_value
    if action_type is not None:
        rule.action_type = action_type
    if action_value is not None:
        rule.action_value = action_value
    if priority is not None:
        rule.priority = priority
    if is_active is not None:
        rule.is_active = is_active
    
    db.commit()
    db.refresh(rule)
    return rule

@app.delete("/api/rules/{rule_id}")
def delete_rule(rule_id: int, db: Session = Depends(get_db)):
    """Delete a rule."""
    rule = db.query(models.Rule).filter(models.Rule.id == rule_id).first()
    if not rule:
        raise HTTPException(status_code=404, detail="Rule not found")
    
    db.delete(rule)
    db.commit()
    return {"status": "deleted", "id": rule_id}

@app.post("/api/rules/reapply")
def reapply_rules(
    transaction_ids: Optional[List[str]] = None,
    db: Session = Depends(get_db)
):
    """Re-apply rules to existing transactions."""
    from .rules_engine import RulesEngine
    
    query = db.query(models.Transaction)
    
    if transaction_ids:
        query = query.filter(models.Transaction.id.in_(transaction_ids))
    
    transactions = query.all()
    applied_count = 0
    
    for transaction in transactions:
        if RulesEngine.apply_rules(db, transaction):
            applied_count += 1
    
    return {
        "status": "success",
        "transactions_processed": len(transactions),
        "rules_applied": applied_count
    }

# ============================================================================
# MERCHANT MANAGEMENT APIs
# ============================================================================

@app.post("/api/merchants")
def create_merchant(
    merchant_key: str,
    display_name: str,
    default_category_id: Optional[int] = None,
    notes: Optional[str] = None,
    is_personal_contact: bool = False,
    is_self_account: bool = False,
    db: Session = Depends(get_db)
):
    """Create a new merchant."""
    # Check if merchant key already exists
    existing = db.query(models.Merchant).filter(
        models.Merchant.merchant_key == merchant_key
    ).first()
    
    if existing:
        raise HTTPException(status_code=400, detail="Merchant key already exists")
    
    merchant = models.Merchant(
        merchant_key=merchant_key,
        display_name=display_name,
        default_category_id=default_category_id,
        notes=notes,
        is_personal_contact=is_personal_contact,
        is_self_account=is_self_account
    )
    db.add(merchant)
    db.commit()
    db.refresh(merchant)
    return merchant

@app.put("/api/merchants/{merchant_id}")
def update_merchant(
    merchant_id: int,
    display_name: Optional[str] = None,
    default_category_id: Optional[int] = None,
    notes: Optional[str] = None,
    is_personal_contact: Optional[bool] = None,
    is_self_account: Optional[bool] = None,
    db: Session = Depends(get_db)
):
    """Update a merchant."""
    merchant = db.query(models.Merchant).filter(models.Merchant.id == merchant_id).first()
    if not merchant:
        raise HTTPException(status_code=404, detail="Merchant not found")
    
    if display_name is not None:
        merchant.display_name = display_name
    if default_category_id is not None:
        merchant.default_category_id = default_category_id
    if notes is not None:
        merchant.notes = notes
    if is_personal_contact is not None:
        merchant.is_personal_contact = is_personal_contact
    if is_self_account is not None:
        merchant.is_self_account = is_self_account
    
    db.commit()
    db.refresh(merchant)
    return merchant

@app.delete("/api/merchants/{merchant_id}")
def delete_merchant(merchant_id: int, db: Session = Depends(get_db)):
    """Delete a merchant."""
    merchant = db.query(models.Merchant).filter(models.Merchant.id == merchant_id).first()
    if not merchant:
        raise HTTPException(status_code=404, detail="Merchant not found")
    
    # Check if merchant is used in transactions
    transaction_count = db.query(models.Transaction).filter(
        models.Transaction.merchant_id == merchant_id
    ).count()
    
    if transaction_count > 0:
        raise HTTPException(
            status_code=400,
            detail=f"Cannot delete merchant with {transaction_count} transactions. Update transactions first."
        )
    
    db.delete(merchant)
    db.commit()
    return {"status": "deleted", "id": merchant_id}

# ============================================================================
# CATEGORY MANAGEMENT APIs
# ============================================================================

@app.post("/api/categories")
def create_category(
    name: str,
    parent_id: Optional[int] = None,
    sort_order: int = 0,
    db: Session = Depends(get_db)
):
    """Create a new category."""
    # Check if category name already exists
    existing = db.query(models.Category).filter(models.Category.name == name).first()
    if existing:
        raise HTTPException(status_code=400, detail="Category name already exists")
    
    category = models.Category(
        name=name,
        parent_id=parent_id,
        sort_order=sort_order
    )
    db.add(category)
    db.commit()
    db.refresh(category)
    return category

@app.put("/api/categories/{category_id}")
def update_category(
    category_id: int,
    name: Optional[str] = None,
    parent_id: Optional[int] = None,
    sort_order: Optional[int] = None,
    db: Session = Depends(get_db)
):
    """Update a category."""
    category = db.query(models.Category).filter(models.Category.id == category_id).first()
    if not category:
        raise HTTPException(status_code=404, detail="Category not found")
    
    if name is not None:
        # Check if new name conflicts
        existing = db.query(models.Category).filter(
            models.Category.name == name,
            models.Category.id != category_id
        ).first()
        if existing:
            raise HTTPException(status_code=400, detail="Category name already exists")
        category.name = name
    
    if parent_id is not None:
        category.parent_id = parent_id
    if sort_order is not None:
        category.sort_order = sort_order
    
    db.commit()
    db.refresh(category)
    return category

@app.delete("/api/categories/{category_id}")
def delete_category(category_id: int, db: Session = Depends(get_db)):
    """Delete a category."""
    category = db.query(models.Category).filter(models.Category.id == category_id).first()
    if not category:
        raise HTTPException(status_code=404, detail="Category not found")
    
    # Check if category is used
    transaction_count = db.query(models.Transaction).filter(
        models.Transaction.category_id == category_id
    ).count()
    
    if transaction_count > 0:
        raise HTTPException(
            status_code=400,
            detail=f"Cannot delete category with {transaction_count} transactions"
        )
    
    db.delete(category)
    db.commit()
    return {"status": "deleted", "id": category_id}

# ============================================================================
# TRANSACTION EDITING APIs
# ============================================================================

@app.put("/api/transactions/{transaction_id}")
def update_transaction(
    transaction_id: str,
    merchant_id: Optional[int] = None,
    category_id: Optional[int] = None,
    description: Optional[str] = None,
    is_internal_transfer: Optional[bool] = None,
    db: Session = Depends(get_db)
):
    """Update a transaction."""
    transaction = db.query(models.Transaction).filter(
        models.Transaction.id == transaction_id
    ).first()
    
    if not transaction:
        raise HTTPException(status_code=404, detail="Transaction not found")
    
    # Track manual overrides with bit flags
    if merchant_id is not None:
        transaction.merchant_id = merchant_id
        transaction.manual_override_flags |= 1  # Set merchant override bit
    
    if category_id is not None:
        transaction.category_id = category_id
        transaction.manual_override_flags |= 2  # Set category override bit
    
    if description is not None:
        transaction.description = description
    
    if is_internal_transfer is not None:
        transaction.is_internal_transfer = is_internal_transfer
        transaction.manual_override_flags |= 4  # Set internal transfer override bit
    
    db.commit()
    db.refresh(transaction)
    
    # Sync updated transaction to Google Sheets
    try:
        from .sheets_sync import get_sheets_sync
        sheets_sync = get_sheets_sync()
        sheets_sync.sync_transaction(db, transaction)
    except Exception as e:
        print(f"Google Sheets sync error: {e}")
    
    return transaction

# ============================================================================
# GOOGLE SHEETS INTEGRATION APIs
# ============================================================================

@app.post("/api/admin/sheets/sync-all")
def sync_all_to_sheets(db: Session = Depends(get_db)):
    """Sync all transactions to Google Sheets (full rebuild)."""
    from .sheets_sync import get_sheets_sync
    
    sheets_sync = get_sheets_sync()
    result = sheets_sync.sync_all_transactions(db)
    
    return result

@app.post("/api/admin/sheets/sync-transaction/{transaction_id}")
def sync_transaction_to_sheets(transaction_id: str, db: Session = Depends(get_db)):
    """Sync a specific transaction to Google Sheets."""
    from .sheets_sync import get_sheets_sync
    
    transaction = db.query(models.Transaction).filter(
        models.Transaction.id == transaction_id
    ).first()
    
    if not transaction:
        raise HTTPException(status_code=404, detail="Transaction not found")
    
    sheets_sync = get_sheets_sync()
    success = sheets_sync.sync_transaction(db, transaction)
    
    return {
        "success": success,
        "transaction_id": transaction_id
    }

@app.get("/api/admin/sheets/config")
def get_sheets_config():
    """Get Google Sheets configuration status."""
    from .sheets_sync import get_sheets_sync
    
    sheets_sync = get_sheets_sync()
    
    return {
        "configured": sheets_sync.webhook_url is not None,
        "webhook_url_set": bool(sheets_sync.webhook_url)
    }

# ============================================================================
# ADMIN UTILITIES
# ============================================================================

@app.post("/api/admin/reparse")
def reparse_events(
    status: Optional[str] = None,
    date_from: Optional[str] = None,
    date_to: Optional[str] = None,
    db: Session = Depends(get_db)
):
    """Re-parse raw events (useful after improving parser logic)."""
    from .parser import process_raw_event
    
    query = db.query(models.RawEvent)
    
    if status:
        query = query.filter(models.RawEvent.parsed_status == status)
    
    if date_from:
        query = query.filter(models.RawEvent.inserted_at >= date_from)
    
    if date_to:
        query = query.filter(models.RawEvent.inserted_at <= date_to)
    
    events = query.all()
    
    success_count = 0
    failed_count = 0
    
    for event in events:
        try:
            transaction = process_raw_event(db, event)
            if transaction:
                success_count += 1
            else:
                failed_count += 1
        except Exception as e:
            failed_count += 1
            print(f"Reparse error for event {event.id}: {e}")
    
    return {
        "status": "complete",
        "total_events": len(events),
        "successful": success_count,
        "failed": failed_count
    }

@app.get("/api/admin/stats")
def get_stats(db: Session = Depends(get_db)):
    """Get system statistics."""
    
    total_transactions = db.query(models.Transaction).count()
    total_raw_events = db.query(models.RawEvent).count()
    total_merchants = db.query(models.Merchant).count()
    total_categories = db.query(models.Category).count()
    total_rules = db.query(models.Rule).count()
    total_accounts = db.query(models.Account).count()
    
    # Parsing stats
    parsed_events = db.query(models.RawEvent).filter(
        models.RawEvent.parsed_status == "PARSED"
    ).count()
    failed_events = db.query(models.RawEvent).filter(
        models.RawEvent.parsed_status == "FAILED"
    ).count()
    pending_events = db.query(models.RawEvent).filter(
        models.RawEvent.parsed_status == "PENDING"
    ).count()
    
    # Categorization stats
    categorized_transactions = db.query(models.Transaction).filter(
        models.Transaction.category_id.isnot(None)
    ).count()
    
    internal_transfers = db.query(models.Transaction).filter(
        models.Transaction.is_internal_transfer == True
    ).count()
    
    return {
        "transactions": {
            "total": total_transactions,
            "categorized": categorized_transactions,
            "categorization_rate": f"{(categorized_transactions/total_transactions*100) if total_transactions > 0 else 0:.1f}%",
            "internal_transfers": internal_transfers
        },
        "raw_events": {
            "total": total_raw_events,
            "parsed": parsed_events,
            "failed": failed_events,
            "pending": pending_events
        },
        "entities": {
            "merchants": total_merchants,
            "categories": total_categories,
            "rules": total_rules,
            "accounts": total_accounts
        }
    }
