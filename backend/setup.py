"""
Setup script for OwnSpend backend.
Creates initial database, test user, and device API key.
"""
import sys
import secrets
from sqlalchemy import Column, Integer, String, Boolean, ForeignKey, DateTime, Float, Text
from sqlalchemy.orm import Session, relationship
from sqlalchemy.sql import func
from database import engine, Base, SessionLocal
import uuid

# Import models inline to avoid relative import issues
class User(Base):
    __tablename__ = "users"
    id = Column(Integer, primary_key=True, index=True)
    email = Column(String, unique=True, index=True)
    password_hash = Column(String)
    created_at = Column(DateTime(timezone=True), server_default=func.now())

class Device(Base):
    __tablename__ = "devices"
    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id"))
    device_name = Column(String)
    api_key = Column(String, unique=True, index=True)
    last_seen_at = Column(DateTime(timezone=True))
    is_active = Column(Boolean, default=True)

class Category(Base):
    __tablename__ = "categories"
    id = Column(Integer, primary_key=True, index=True)
    name = Column(String, unique=True)
    parent_id = Column(Integer, ForeignKey("categories.id"), nullable=True)
    sort_order = Column(Integer, default=0)

def create_initial_data():
    """Create initial test user and device."""
    Base.metadata.create_all(bind=engine)
    
    db = SessionLocal()
    
    try:
        # Check if user already exists
        existing_user = db.query(User).first()
        if existing_user:
            print("✅ Database already initialized")
            print(f"   User: {existing_user.email}")
            
            # Show devices
            devices = db.query(Device).filter(Device.user_id == existing_user.id).all()
            if devices:
                print(f"\n📱 Devices:")
                for device in devices:
                    print(f"   - {device.device_name}")
                    print(f"     API Key: {device.api_key}")
                    print(f"     Active: {device.is_active}")
            return
        
        # Create test user
        user = User(
            email="arka@ownspend.local",
            password_hash="dummy_hash_for_now"  # TODO: Implement proper auth
        )
        db.add(user)
        db.flush()
        
        print("✅ Created test user: arka@ownspend.local")
        
        # Create test device with API key
        api_key = secrets.token_urlsafe(32)
        device = Device(
            user_id=user.id,
            device_name="Test Android Device",
            api_key=api_key,
            is_active=True
        )
        db.add(device)
        
        # Create some default categories
        categories = [
            "Food & Dining",
            "Groceries",
            "Transportation",
            "Shopping",
            "Bills & Utilities",
            "Entertainment",
            "Health & Fitness",
            "Transfer",
            "Salary",
            "Other"
        ]
        
        for idx, cat_name in enumerate(categories):
            category = Category(
                name=cat_name,
                sort_order=idx
            )
            db.add(category)
        
        db.commit()
        
        print(f"\n✅ Created test device: {device.device_name}")
        print(f"\n🔑 API Key for Android app:")
        print(f"   {api_key}")
        print(f"\n📝 Save this API key - you'll need it to configure the Android app!")
        print(f"\n✅ Created {len(categories)} default categories")
        
        print("\n" + "="*60)
        print("Backend setup complete!")
        print("="*60)
        print(f"\nYou can now:")
        print(f"1. Start the backend: uvicorn main:app --reload")
        print(f"2. Visit API docs: http://localhost:8000/docs")
        print(f"3. Configure Android app with API key above")
        print("="*60)
        
    except Exception as e:
        print(f"❌ Error: {e}")
        db.rollback()
        raise
    finally:
        db.close()

if __name__ == "__main__":
    create_initial_data()
