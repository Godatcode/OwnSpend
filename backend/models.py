from sqlalchemy import Column, Integer, String, Boolean, ForeignKey, DateTime, Numeric, Text, Float
from sqlalchemy.orm import relationship
from sqlalchemy.sql import func
from database import Base
import uuid

class User(Base):
    __tablename__ = "users"

    id = Column(Integer, primary_key=True, index=True)
    email = Column(String, unique=True, index=True)
    password_hash = Column(String)
    created_at = Column(DateTime(timezone=True), server_default=func.now())

    devices = relationship("Device", back_populates="user")
    accounts = relationship("Account", back_populates="user")
    transactions = relationship("Transaction", back_populates="user")
    rules = relationship("Rule", back_populates="user")

class Device(Base):
    __tablename__ = "devices"

    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id"))
    device_name = Column(String)
    api_key = Column(String, unique=True, index=True)
    last_seen_at = Column(DateTime(timezone=True))
    is_active = Column(Boolean, default=True)

    user = relationship("User", back_populates="devices")

class Account(Base):
    __tablename__ = "accounts"

    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id"))
    bank_name = Column(String)
    account_mask = Column(String)
    display_name = Column(String)
    type = Column(String) # SAVINGS, WALLET, CREDIT_CARD
    is_active = Column(Boolean, default=True)
    created_at = Column(DateTime(timezone=True), server_default=func.now())

    user = relationship("User", back_populates="accounts")
    transactions = relationship("Transaction", back_populates="account")

class RawEvent(Base):
    __tablename__ = "raw_events"

    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id"))
    device_id = Column(Integer, ForeignKey("devices.id"))
    source_type = Column(String) # SMS, NOTIFICATION
    source_sender = Column(String)
    raw_text = Column(Text)
    received_at = Column(DateTime(timezone=True))
    inserted_at = Column(DateTime(timezone=True), server_default=func.now())
    parsed_status = Column(String, default="PENDING") # PENDING, PARSED, FAILED
    error_message = Column(Text, nullable=True)
    related_transaction_id = Column(String, ForeignKey("transactions.id"), nullable=True)

class Transaction(Base):
    __tablename__ = "transactions"

    id = Column(String, primary_key=True, default=lambda: str(uuid.uuid4()))
    user_id = Column(Integer, ForeignKey("users.id"))
    account_id = Column(Integer, ForeignKey("accounts.id"))
    direction = Column(String) # DEBIT, CREDIT
    amount = Column(Float)
    currency = Column(String, default="INR")
    channel = Column(String) # UPI, CARD, NETBANKING, ATM, OTHER
    raw_merchant_identifier = Column(String, nullable=True)
    merchant_key = Column(String, nullable=True)
    merchant_id = Column(Integer, ForeignKey("merchants.id"), nullable=True)
    category_id = Column(Integer, ForeignKey("categories.id"), nullable=True)
    description = Column(String, nullable=True)
    transaction_time = Column(DateTime(timezone=True))
    ingested_at = Column(DateTime(timezone=True), server_default=func.now())
    dedupe_key = Column(String, unique=True, index=True)
    is_internal_transfer = Column(Boolean, default=False)
    manual_override_flags = Column(Integer, default=0)

    user = relationship("User", back_populates="transactions")
    account = relationship("Account", back_populates="transactions")
    merchant = relationship("Merchant", back_populates="transactions")
    category = relationship("Category", back_populates="transactions")

class Merchant(Base):
    __tablename__ = "merchants"

    id = Column(Integer, primary_key=True, index=True)
    merchant_key = Column(String, unique=True, index=True)
    display_name = Column(String)
    default_category_id = Column(Integer, ForeignKey("categories.id"), nullable=True)
    notes = Column(Text, nullable=True)
    is_personal_contact = Column(Boolean, default=False)
    is_self_account = Column(Boolean, default=False)

    transactions = relationship("Transaction", back_populates="merchant")

class Category(Base):
    __tablename__ = "categories"

    id = Column(Integer, primary_key=True, index=True)
    name = Column(String, unique=True)
    parent_id = Column(Integer, ForeignKey("categories.id"), nullable=True)
    sort_order = Column(Integer, default=0)

    transactions = relationship("Transaction", back_populates="category")

class Rule(Base):
    __tablename__ = "rules"

    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id"))
    match_type = Column(String) # MERCHANT_KEY, TEXT_CONTAINS, UPI_ID_PREFIX, AMOUNT_RANGE
    match_value = Column(String)
    action_type = Column(String) # SET_MERCHANT, SET_CATEGORY, MARK_INTERNAL
    action_value = Column(String)
    priority = Column(Integer, default=0)
    is_active = Column(Boolean, default=True)

    user = relationship("User", back_populates="rules")
