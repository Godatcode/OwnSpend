"""
Test script to simulate SMS ingestion.
Sends sample SMS events to the backend for testing parsing.
"""
import requests
from datetime import datetime

# Configuration
BASE_URL = "http://localhost:8000"
API_KEY = "YOUR_API_KEY_HERE"  # Replace with actual API key from setup.py

# Sample SMS messages
SAMPLE_MESSAGES = [
    {
        "source_type": "SMS",
        "source_sender": "VM-KOTAKB",
        "raw_text": "Sent Rs.15.00 from Kotak Bank AC X1415 to amitabh10b26.hts21@okicici via UPI Ref no 434750881179",
        "device_timestamp": datetime.now().isoformat()
    },
    {
        "source_type": "SMS",
        "source_sender": "UCO-BANK",
        "raw_text": "UCO-UPI/CR/434750881179/amitabh10b26.hts21@okicici/UCO BANK/XX3242/15.00",
        "device_timestamp": datetime.now().isoformat()
    },
    {
        "source_type": "NOTIFICATION",
        "source_sender": "com.google.android.apps.nbu.paisa.user",
        "raw_text": "You paid ₹50.00 to Coffee Shop",
        "device_timestamp": datetime.now().isoformat()
    }
]

def test_ingestion():
    """Send sample messages to the backend."""
    
    headers = {
        "api-key": API_KEY,
        "Content-Type": "application/json"
    }
    
    print("Testing event ingestion...\n")
    print("="*60)
    
    for idx, message in enumerate(SAMPLE_MESSAGES, 1):
        print(f"\nTest {idx}: {message['source_sender']}")
        print(f"Text: {message['raw_text'][:60]}...")
        
        try:
            response = requests.post(
                f"{BASE_URL}/api/events/ingest",
                json=message,
                headers=headers
            )
            
            if response.status_code == 200:
                result = response.json()
                print(f"✅ Success!")
                print(f"   Raw Event ID: {result.get('raw_event_id')}")
                print(f"   Transaction ID: {result.get('transaction_id')}")
                print(f"   Parsed: {result.get('parsed')}")
            else:
                print(f"❌ Error: {response.status_code}")
                print(f"   {response.text}")
                
        except Exception as e:
            print(f"❌ Exception: {e}")
    
    print("\n" + "="*60)
    print("\nFetching transactions...")
    
    try:
        response = requests.get(f"{BASE_URL}/api/transactions")
        if response.status_code == 200:
            transactions = response.json()
            print(f"\n✅ Found {len(transactions)} transactions:")
            for t in transactions:
                print(f"\n   {t['transaction_time']}")
                print(f"   {t['direction']}: ₹{t['amount']}")
                print(f"   Account: {t['account_name']}")
                print(f"   Channel: {t['channel']}")
        else:
            print(f"❌ Error fetching transactions: {response.status_code}")
    except Exception as e:
        print(f"❌ Exception: {e}")

if __name__ == "__main__":
    if API_KEY == "YOUR_API_KEY_HERE":
        print("⚠️  Please update API_KEY in this script with your actual API key")
        print("   (Run setup.py first to get the API key)")
    else:
        test_ingestion()
