#!/usr/bin/env python3
"""
Test the new bank parsers with sample SMS messages.
"""

import sys
sys.path.insert(0, '/Users/arkaghosh/Documents/GitHub/OwnSpend')

from backend.parser import TransactionParser

# Test messages for each bank
TEST_MESSAGES = {
    "HDFC - UPI Debit": {
        "text": "Rs.500.00 debited from HDFC Bank A/c XX1234 on 01-12-25. Info: UPI/john@okaxis. Avl bal: Rs.5000.00",
        "expected": {"amount": 500.0, "direction": "DEBIT", "bank_name": "HDFC", "channel": "UPI"}
    },
    "HDFC - Credit": {
        "text": "HDFC Bank: Rs 1500.00 credited to A/c XX1234 on 01-12-25 by UPI Ref No 123456789012.",
        "expected": {"amount": 1500.0, "direction": "CREDIT", "bank_name": "HDFC", "channel": "UPI"}
    },
    "HDFC - ATM": {
        "text": "Alert: INR 2000.00 debited from A/c XX1234 on 01-Dec-25 by ATM-WDL. Avl Bal: INR 5000.00",
        "expected": {"amount": 2000.0, "direction": "DEBIT", "bank_name": "HDFC", "channel": "ATM"}
    },
    "ICICI - Debit": {
        "text": "ICICI Bank Acct XX5678 debited for Rs 750.00 on 01-Dec-25; john@upi credited. UPI:123456789012",
        "expected": {"amount": 750.0, "direction": "DEBIT", "bank_name": "ICICI", "channel": "UPI"}
    },
    "ICICI - Credit Card": {
        "text": "Dear Customer, your ICICI Bank Credit Card XX9999 has been used for Rs.1299.00 at AMAZON on 01-Dec-25",
        "expected": {"amount": 1299.0, "direction": "DEBIT", "bank_name": "ICICI", "channel": "CARD"}
    },
    "SBI - UPI Debit": {
        "text": "Your a/c no. XXXXXXXX1234 is debited for Rs.500.00 on 01Dec25 by transfer to VPA john@upi (UPI Ref No 123456789012)",
        "expected": {"amount": 500.0, "direction": "DEBIT", "bank_name": "SBI", "channel": "UPI"}
    },
    "SBI - Credit": {
        "text": "Your a/c XX1234 credited by Rs.2500.00 on 01Dec25 by transfer from VPA seller@upi (UPI Ref No 123456789012)",
        "expected": {"amount": 2500.0, "direction": "CREDIT", "bank_name": "SBI", "channel": "UPI"}
    },
    "SBI - ATM": {
        "text": "ATM-SBI: Rs.1000 withdrawn from A/c XX1234 on 01Dec25. Avl bal: Rs.5000.00",
        "expected": {"amount": 1000.0, "direction": "DEBIT", "bank_name": "SBI", "channel": "ATM"}
    },
    "Axis - Debit": {
        "text": "Rs.850.00 debited from A/c no. XX1234 on 01-12-2025. Info- UPI/swiggy@axisbank/UPI. Avl Bal- Rs.5000.00",
        "expected": {"amount": 850.0, "direction": "DEBIT", "bank_name": "Axis", "channel": "UPI"}
    },
    "Axis - Credit Card": {
        "text": "INR 2499.00 spent on Axis Bank Credit Card XX1234 at FLIPKART on 01-Dec-25. Avl Limit: INR 50000",
        "expected": {"amount": 2499.0, "direction": "DEBIT", "bank_name": "Axis", "channel": "CARD"}
    },
    "PhonePe - Paid": {
        "text": "Paid ₹500 to john@upi from HDFC XX1234",
        "expected": {"amount": 500.0, "direction": "DEBIT", "channel": "UPI"}
    },
    "PhonePe - Received": {
        "text": "Received ₹1500 from seller@upi to HDFC XX1234",
        "expected": {"amount": 1500.0, "direction": "CREDIT", "channel": "UPI"}
    },
    "Paytm - Paid": {
        "text": "You paid Rs.299 to netflix@paytm",
        "expected": {"amount": 299.0, "direction": "DEBIT", "channel": "UPI"}
    },
    "Generic - UPI": {
        "text": "Rs.100.00 debited from your account XX1234 via UPI. Ref: 123456789012",
        "expected": {"amount": 100.0, "direction": "DEBIT", "bank_name": "Unknown", "channel": "UPI"}
    },
    "Kotak - UPI": {
        "text": "Sent Rs.250.00 from Kotak Bank AC X1415 to zomato@hdfcbank via UPI Ref no 434750881179",
        "expected": {"amount": 250.0, "direction": "DEBIT", "bank_name": "Kotak", "channel": "UPI"}
    }
}

def test_parsers():
    """Test all parsers with sample messages."""
    print("🧪 Testing Bank Parsers\n" + "=" * 50)
    
    passed = 0
    failed = 0
    
    for name, test_case in TEST_MESSAGES.items():
        print(f"\n📝 {name}")
        print(f"   Message: {test_case['text'][:80]}...")
        
        # Determine which parser to use based on message content
        text_lower = test_case['text'].lower()
        
        if 'hdfc' in text_lower:
            result = TransactionParser.parse_hdfc_sms(test_case['text'])
        elif 'icici' in text_lower:
            result = TransactionParser.parse_icici_sms(test_case['text'])
        elif 'sbi' in text_lower:
            result = TransactionParser.parse_sbi_sms(test_case['text'])
        elif 'axis' in text_lower:
            result = TransactionParser.parse_axis_sms(test_case['text'])
        elif 'phonepe' in name.lower() or 'paid ₹' in text_lower or 'received ₹' in text_lower:
            result = TransactionParser.parse_phonepe_notification(test_case['text'])
        elif 'paytm' in name.lower() or 'you paid rs' in text_lower:
            result = TransactionParser.parse_paytm_notification(test_case['text'])
        elif 'kotak' in text_lower:
            result = TransactionParser.parse_kotak_sms(test_case['text'])
        else:
            result = TransactionParser.parse_generic_bank_sms(test_case['text'])
        
        if result:
            # Check expected values
            all_match = True
            for key, expected_value in test_case['expected'].items():
                actual_value = result.get(key)
                if actual_value != expected_value:
                    print(f"   ❌ {key}: expected {expected_value}, got {actual_value}")
                    all_match = False
            
            if all_match:
                print(f"   ✅ Parsed correctly: amount={result.get('amount')}, direction={result.get('direction')}, channel={result.get('channel')}")
                passed += 1
            else:
                print(f"   ⚠️  Some fields didn't match")
                failed += 1
        else:
            print(f"   ❌ Failed to parse")
            failed += 1
    
    print("\n" + "=" * 50)
    print(f"📊 Results: {passed} passed, {failed} failed out of {len(TEST_MESSAGES)}")
    
    return passed, failed

if __name__ == "__main__":
    test_parsers()
