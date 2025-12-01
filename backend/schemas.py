from typing import Optional
from pydantic import BaseModel
from datetime import datetime

class RawEventCreate(BaseModel):
    source_type: str  # SMS or NOTIFICATION
    source_sender: str
    raw_text: str
    device_timestamp: datetime
    
class TransactionResponse(BaseModel):
    id: str
    amount: float
    direction: str
    channel: str
    description: Optional[str]
    transaction_time: datetime
    merchant_display_name: Optional[str]
    category_name: Optional[str]
    account_name: str
    
    class Config:
        from_attributes = True
