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
