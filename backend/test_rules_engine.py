"""
Enhanced test script to demonstrate rules engine and auto-categorization.
"""
import requests
from datetime import datetime

# Configuration
BASE_URL = "http://localhost:8000"
API_KEY = "YOUR_API_KEY_HERE"  # Replace with actual API key from setup.py

# Sample messages with various patterns
SAMPLE_MESSAGES = [
    # Zomato - should auto-categorize to "Food & Dining"
    {
        "source_type": "SMS",
        "source_sender": "VM-KOTAKB",
        "raw_text": "Sent Rs.350.00 from Kotak Bank AC X1415 to zomato@paytm via UPI Ref no 434750881180",
        "device_timestamp": datetime.now().isoformat()
    },
    # Swiggy - should auto-categorize to "Food & Dining"
    {
        "source_type": "NOTIFICATION",
        "source_sender": "com.google.android.apps.nbu.paisa.user",
        "raw_text": "You paid ₹425.00 to Swiggy",
        "device_timestamp": datetime.now().isoformat()
    },
    # Amazon - should auto-categorize to "Shopping"
    {
        "source_type": "SMS",
        "source_sender": "UCO-BANK",
        "raw_text": "UCO-UPI/DR/434750881181/amazonpay@icici/UCO BANK/XX3242/1250.00",
        "device_timestamp": datetime.now().isoformat()
    },
    # Uber - should auto-categorize to "Transportation"
    {
        "source_type": "SMS",
        "source_sender": "VM-KOTAKB",
        "raw_text": "Sent Rs.180.00 from Kotak Bank AC X1415 to uber.india@paytm via UPI Ref no 434750881182",
        "device_timestamp": datetime.now().isoformat()
    },
    # Grocery - should auto-categorize to "Groceries"
    {
        "source_type": "NOTIFICATION",
        "source_sender": "com.google.android.apps.nbu.paisa.user",
        "raw_text": "You paid ₹850.00 to BigBasket Grocery Store",
        "device_timestamp": datetime.now().isoformat()
    },
    # Salary - should auto-categorize to "Salary"
    {
        "source_type": "SMS",
        "source_sender": "UCO-BANK",
        "raw_text": "UCO-UPI/CR/434750881183/company.payroll@hdfc/UCO BANK/XX3242/45000.00 - Salary Credit",
        "device_timestamp": datetime.now().isoformat()
    },
    # Personal transfer
    {
        "source_type": "SMS",
        "source_sender": "VM-KOTAKB",
        "raw_text": "Sent Rs.500.00 from Kotak Bank AC X1415 to rahul.sharma@okicici via UPI Ref no 434750881184",
        "device_timestamp": datetime.now().isoformat()
    },
]

def test_rules_engine():
    """Test the rules engine with various transaction patterns."""
    
    headers = {
        "api-key": API_KEY,
        "Content-Type": "application/json"
    }
    
    print("=" * 80)
    print("TESTING RULES ENGINE & AUTO-CATEGORIZATION")
    print("=" * 80)
    
    # First, check current rules
    print("\n📋 Current Rules:")
    print("-" * 80)
    try:
        response = requests.get(f"{BASE_URL}/api/rules")
        if response.status_code == 200:
            rules = response.json()
            print(f"\n✅ Found {len(rules)} active rules:")
            for rule in rules:
                print(f"\n   Priority {rule['priority']}: {rule['match_type']} = '{rule['match_value']}'")
                print(f"   Action: {rule['action_type']} → '{rule['action_value']}'")
        else:
            print(f"⚠️  Could not fetch rules: {response.status_code}")
    except Exception as e:
        print(f"❌ Error fetching rules: {e}")
    
    print("\n" + "=" * 80)
    print("INGESTING TEST TRANSACTIONS")
    print("=" * 80)
    
    transaction_ids = []
    
    for idx, message in enumerate(SAMPLE_MESSAGES, 1):
        print(f"\n🔄 Test {idx}/{len(SAMPLE_MESSAGES)}")
        print(f"   Source: {message['source_sender']}")
        print(f"   Text: {message['raw_text'][:70]}...")
        
        try:
            response = requests.post(
                f"{BASE_URL}/api/events/ingest",
                json=message,
                headers=headers
            )
            
            if response.status_code == 200:
                result = response.json()
                print(f"   ✅ Ingested successfully")
                if result.get('transaction_id'):
                    transaction_ids.append(result['transaction_id'])
                    print(f"   Transaction ID: {result['transaction_id']}")
                    print(f"   Parsed: {result.get('parsed')}")
            else:
                print(f"   ❌ Error: {response.status_code}")
                print(f"   {response.text[:200]}")
                
        except Exception as e:
            print(f"   ❌ Exception: {e}")
    
    print("\n" + "=" * 80)
    print("VERIFYING AUTO-CATEGORIZATION")
    print("=" * 80)
    
    try:
        response = requests.get(f"{BASE_URL}/api/transactions")
        if response.status_code == 200:
            transactions = response.json()
            
            print(f"\n✅ Retrieved {len(transactions)} transactions")
            print("\nTransaction Details:")
            print("-" * 80)
            
            categorized_count = 0
            for t in transactions[:10]:  # Show last 10
                category = t.get('category_name', 'None')
                if category and category != 'None':
                    categorized_count += 1
                    status = "✅"
                else:
                    status = "⚠️ "
                
                print(f"\n{status} {t['transaction_time'][:19]}")
                print(f"   Amount: {t['direction']} ₹{t['amount']:.2f}")
                print(f"   Account: {t['account_name']}")
                print(f"   Category: {category or 'Not categorized'}")
                print(f"   Channel: {t['channel']}")
            
            print("\n" + "-" * 80)
            print(f"\n📊 Summary:")
            print(f"   Total transactions: {len(transactions)}")
            print(f"   Auto-categorized: {categorized_count}")
            print(f"   Success rate: {(categorized_count/len(transactions)*100) if transactions else 0:.1f}%")
        else:
            print(f"❌ Error fetching transactions: {response.status_code}")
    except Exception as e:
        print(f"❌ Exception: {e}")
    
    print("\n" + "=" * 80)
    print("TEST COMPLETE")
    print("=" * 80)
    print("\n💡 Tips:")
    print("   • Visit http://localhost:8000/docs to explore all APIs")
    print("   • Use GET /api/rules to see all rules")
    print("   • Use POST /api/rules to create custom rules")
    print("   • Use POST /api/rules/reapply to re-run rules on old transactions")
    print("=" * 80)

if __name__ == "__main__":
    if API_KEY == "YOUR_API_KEY_HERE":
        print("⚠️  Please update API_KEY in this script with your actual API key")
        print("   (Run: cd backend && python setup.py)")
    else:
        test_rules_engine()
