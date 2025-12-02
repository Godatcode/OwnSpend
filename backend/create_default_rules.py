#!/usr/bin/env python3
"""
Create default categorization rules for common Indian merchants and services.
Run this once to populate the rules engine.
"""

import requests

BASE_URL = "http://localhost:8000"

# Category IDs (from existing categories)
CATEGORIES = {
    "food_dining": 1,
    "groceries": 2,
    "transportation": 3,
    "shopping": 4,
    "bills_utilities": 5,
    "entertainment": 6,
    "health_fitness": 7,
    "transfer": 8,
    "salary": 9,
    "other": 10,
}

# Default rules to create
DEFAULT_RULES = [
    # ============ FOOD & DINING (Category 1) ============
    # Food delivery apps
    {"name": "Zomato Orders", "match_type": "text_contains", "match_value": "zomato", 
     "action_type": "set_category", "action_value": str(CATEGORIES["food_dining"]), "priority": 100},
    {"name": "Swiggy Orders", "match_type": "text_contains", "match_value": "swiggy", 
     "action_type": "set_category", "action_value": str(CATEGORIES["food_dining"]), "priority": 100},
    {"name": "EatSure Orders", "match_type": "text_contains", "match_value": "eatsure", 
     "action_type": "set_category", "action_value": str(CATEGORIES["food_dining"]), "priority": 100},
    {"name": "Dominos", "match_type": "text_contains", "match_value": "domino", 
     "action_type": "set_category", "action_value": str(CATEGORIES["food_dining"]), "priority": 90},
    {"name": "Pizza Hut", "match_type": "text_contains", "match_value": "pizzahut", 
     "action_type": "set_category", "action_value": str(CATEGORIES["food_dining"]), "priority": 90},
    {"name": "McDonald's", "match_type": "text_contains", "match_value": "mcdonald", 
     "action_type": "set_category", "action_value": str(CATEGORIES["food_dining"]), "priority": 90},
    {"name": "KFC", "match_type": "text_contains", "match_value": "kfc", 
     "action_type": "set_category", "action_value": str(CATEGORIES["food_dining"]), "priority": 90},
    {"name": "Burger King", "match_type": "text_contains", "match_value": "burger king", 
     "action_type": "set_category", "action_value": str(CATEGORIES["food_dining"]), "priority": 90},
    {"name": "Starbucks", "match_type": "text_contains", "match_value": "starbucks", 
     "action_type": "set_category", "action_value": str(CATEGORIES["food_dining"]), "priority": 90},
    {"name": "Cafe Coffee Day", "match_type": "text_contains", "match_value": "cafe coffee day", 
     "action_type": "set_category", "action_value": str(CATEGORIES["food_dining"]), "priority": 90},
    {"name": "CCD", "match_type": "text_contains", "match_value": "ccd", 
     "action_type": "set_category", "action_value": str(CATEGORIES["food_dining"]), "priority": 85},
    
    # ============ GROCERIES (Category 2) ============
    {"name": "BigBasket", "match_type": "text_contains", "match_value": "bigbasket", 
     "action_type": "set_category", "action_value": str(CATEGORIES["groceries"]), "priority": 100},
    {"name": "Blinkit", "match_type": "text_contains", "match_value": "blinkit", 
     "action_type": "set_category", "action_value": str(CATEGORIES["groceries"]), "priority": 100},
    {"name": "Zepto", "match_type": "text_contains", "match_value": "zepto", 
     "action_type": "set_category", "action_value": str(CATEGORIES["groceries"]), "priority": 100},
    {"name": "Instamart", "match_type": "text_contains", "match_value": "instamart", 
     "action_type": "set_category", "action_value": str(CATEGORIES["groceries"]), "priority": 100},
    {"name": "DMart", "match_type": "text_contains", "match_value": "dmart", 
     "action_type": "set_category", "action_value": str(CATEGORIES["groceries"]), "priority": 95},
    {"name": "D-Mart", "match_type": "text_contains", "match_value": "d-mart", 
     "action_type": "set_category", "action_value": str(CATEGORIES["groceries"]), "priority": 95},
    {"name": "Reliance Fresh", "match_type": "text_contains", "match_value": "reliance fresh", 
     "action_type": "set_category", "action_value": str(CATEGORIES["groceries"]), "priority": 95},
    {"name": "More Supermarket", "match_type": "text_contains", "match_value": "more supermarket", 
     "action_type": "set_category", "action_value": str(CATEGORIES["groceries"]), "priority": 95},
    {"name": "JioMart", "match_type": "text_contains", "match_value": "jiomart", 
     "action_type": "set_category", "action_value": str(CATEGORIES["groceries"]), "priority": 95},
    
    # ============ TRANSPORTATION (Category 3) ============
    {"name": "Uber Rides", "match_type": "text_contains", "match_value": "uber", 
     "action_type": "set_category", "action_value": str(CATEGORIES["transportation"]), "priority": 100},
    {"name": "Ola Cabs", "match_type": "text_contains", "match_value": "ola", 
     "action_type": "set_category", "action_value": str(CATEGORIES["transportation"]), "priority": 100},
    {"name": "Rapido", "match_type": "text_contains", "match_value": "rapido", 
     "action_type": "set_category", "action_value": str(CATEGORIES["transportation"]), "priority": 100},
    {"name": "BluSmart", "match_type": "text_contains", "match_value": "blusmart", 
     "action_type": "set_category", "action_value": str(CATEGORIES["transportation"]), "priority": 100},
    {"name": "Metro Card Recharge", "match_type": "text_contains", "match_value": "metro", 
     "action_type": "set_category", "action_value": str(CATEGORIES["transportation"]), "priority": 80},
    {"name": "IRCTC", "match_type": "text_contains", "match_value": "irctc", 
     "action_type": "set_category", "action_value": str(CATEGORIES["transportation"]), "priority": 100},
    {"name": "Indian Railways", "match_type": "text_contains", "match_value": "railway", 
     "action_type": "set_category", "action_value": str(CATEGORIES["transportation"]), "priority": 90},
    {"name": "MakeMyTrip", "match_type": "text_contains", "match_value": "makemytrip", 
     "action_type": "set_category", "action_value": str(CATEGORIES["transportation"]), "priority": 95},
    {"name": "Yatra", "match_type": "text_contains", "match_value": "yatra", 
     "action_type": "set_category", "action_value": str(CATEGORIES["transportation"]), "priority": 95},
    {"name": "RedBus", "match_type": "text_contains", "match_value": "redbus", 
     "action_type": "set_category", "action_value": str(CATEGORIES["transportation"]), "priority": 95},
    {"name": "Petrol Pump - HP", "match_type": "text_contains", "match_value": "hindustan petroleum", 
     "action_type": "set_category", "action_value": str(CATEGORIES["transportation"]), "priority": 90},
    {"name": "Petrol Pump - BPCL", "match_type": "text_contains", "match_value": "bharat petroleum", 
     "action_type": "set_category", "action_value": str(CATEGORIES["transportation"]), "priority": 90},
    {"name": "Petrol Pump - IOCL", "match_type": "text_contains", "match_value": "indian oil", 
     "action_type": "set_category", "action_value": str(CATEGORIES["transportation"]), "priority": 90},
    {"name": "Petrol Pump Generic", "match_type": "text_contains", "match_value": "petrol", 
     "action_type": "set_category", "action_value": str(CATEGORIES["transportation"]), "priority": 70},
    {"name": "Fuel Generic", "match_type": "text_contains", "match_value": "fuel", 
     "action_type": "set_category", "action_value": str(CATEGORIES["transportation"]), "priority": 70},
    
    # ============ SHOPPING (Category 4) ============
    {"name": "Amazon", "match_type": "text_contains", "match_value": "amazon", 
     "action_type": "set_category", "action_value": str(CATEGORIES["shopping"]), "priority": 100},
    {"name": "Flipkart", "match_type": "text_contains", "match_value": "flipkart", 
     "action_type": "set_category", "action_value": str(CATEGORIES["shopping"]), "priority": 100},
    {"name": "Myntra", "match_type": "text_contains", "match_value": "myntra", 
     "action_type": "set_category", "action_value": str(CATEGORIES["shopping"]), "priority": 100},
    {"name": "Ajio", "match_type": "text_contains", "match_value": "ajio", 
     "action_type": "set_category", "action_value": str(CATEGORIES["shopping"]), "priority": 100},
    {"name": "Meesho", "match_type": "text_contains", "match_value": "meesho", 
     "action_type": "set_category", "action_value": str(CATEGORIES["shopping"]), "priority": 100},
    {"name": "Nykaa", "match_type": "text_contains", "match_value": "nykaa", 
     "action_type": "set_category", "action_value": str(CATEGORIES["shopping"]), "priority": 100},
    {"name": "Tata Cliq", "match_type": "text_contains", "match_value": "tatacliq", 
     "action_type": "set_category", "action_value": str(CATEGORIES["shopping"]), "priority": 100},
    {"name": "Croma", "match_type": "text_contains", "match_value": "croma", 
     "action_type": "set_category", "action_value": str(CATEGORIES["shopping"]), "priority": 95},
    {"name": "Reliance Digital", "match_type": "text_contains", "match_value": "reliance digital", 
     "action_type": "set_category", "action_value": str(CATEGORIES["shopping"]), "priority": 95},
    {"name": "Vijay Sales", "match_type": "text_contains", "match_value": "vijay sales", 
     "action_type": "set_category", "action_value": str(CATEGORIES["shopping"]), "priority": 95},
    
    # ============ BILLS & UTILITIES (Category 5) ============
    {"name": "Electricity Bill", "match_type": "text_contains", "match_value": "electricity", 
     "action_type": "set_category", "action_value": str(CATEGORIES["bills_utilities"]), "priority": 95},
    {"name": "Water Bill", "match_type": "text_contains", "match_value": "water bill", 
     "action_type": "set_category", "action_value": str(CATEGORIES["bills_utilities"]), "priority": 95},
    {"name": "Gas Bill", "match_type": "text_contains", "match_value": "gas bill", 
     "action_type": "set_category", "action_value": str(CATEGORIES["bills_utilities"]), "priority": 95},
    {"name": "Piped Gas", "match_type": "text_contains", "match_value": "piped gas", 
     "action_type": "set_category", "action_value": str(CATEGORIES["bills_utilities"]), "priority": 95},
    {"name": "Indane Gas", "match_type": "text_contains", "match_value": "indane", 
     "action_type": "set_category", "action_value": str(CATEGORIES["bills_utilities"]), "priority": 95},
    {"name": "HP Gas", "match_type": "text_contains", "match_value": "hp gas", 
     "action_type": "set_category", "action_value": str(CATEGORIES["bills_utilities"]), "priority": 95},
    {"name": "Bharatgas", "match_type": "text_contains", "match_value": "bharatgas", 
     "action_type": "set_category", "action_value": str(CATEGORIES["bills_utilities"]), "priority": 95},
    {"name": "Jio Recharge", "match_type": "text_contains", "match_value": "jio recharge", 
     "action_type": "set_category", "action_value": str(CATEGORIES["bills_utilities"]), "priority": 100},
    {"name": "Airtel Recharge", "match_type": "text_contains", "match_value": "airtel", 
     "action_type": "set_category", "action_value": str(CATEGORIES["bills_utilities"]), "priority": 90},
    {"name": "Vi Recharge", "match_type": "text_contains", "match_value": "vodafone", 
     "action_type": "set_category", "action_value": str(CATEGORIES["bills_utilities"]), "priority": 90},
    {"name": "BSNL Recharge", "match_type": "text_contains", "match_value": "bsnl", 
     "action_type": "set_category", "action_value": str(CATEGORIES["bills_utilities"]), "priority": 90},
    {"name": "Broadband Bill", "match_type": "text_contains", "match_value": "broadband", 
     "action_type": "set_category", "action_value": str(CATEGORIES["bills_utilities"]), "priority": 95},
    {"name": "Internet Bill", "match_type": "text_contains", "match_value": "internet", 
     "action_type": "set_category", "action_value": str(CATEGORIES["bills_utilities"]), "priority": 85},
    {"name": "ACT Fibernet", "match_type": "text_contains", "match_value": "act fibernet", 
     "action_type": "set_category", "action_value": str(CATEGORIES["bills_utilities"]), "priority": 95},
    {"name": "Insurance Premium", "match_type": "text_contains", "match_value": "insurance", 
     "action_type": "set_category", "action_value": str(CATEGORIES["bills_utilities"]), "priority": 90},
    {"name": "LIC Premium", "match_type": "text_contains", "match_value": "lic", 
     "action_type": "set_category", "action_value": str(CATEGORIES["bills_utilities"]), "priority": 90},
    
    # ============ ENTERTAINMENT (Category 6) ============
    {"name": "Netflix", "match_type": "text_contains", "match_value": "netflix", 
     "action_type": "set_category", "action_value": str(CATEGORIES["entertainment"]), "priority": 100},
    {"name": "Amazon Prime", "match_type": "text_contains", "match_value": "prime video", 
     "action_type": "set_category", "action_value": str(CATEGORIES["entertainment"]), "priority": 100},
    {"name": "Disney Hotstar", "match_type": "text_contains", "match_value": "hotstar", 
     "action_type": "set_category", "action_value": str(CATEGORIES["entertainment"]), "priority": 100},
    {"name": "Disney Plus", "match_type": "text_contains", "match_value": "disney", 
     "action_type": "set_category", "action_value": str(CATEGORIES["entertainment"]), "priority": 95},
    {"name": "Spotify", "match_type": "text_contains", "match_value": "spotify", 
     "action_type": "set_category", "action_value": str(CATEGORIES["entertainment"]), "priority": 100},
    {"name": "YouTube Premium", "match_type": "text_contains", "match_value": "youtube", 
     "action_type": "set_category", "action_value": str(CATEGORIES["entertainment"]), "priority": 100},
    {"name": "SonyLIV", "match_type": "text_contains", "match_value": "sonyliv", 
     "action_type": "set_category", "action_value": str(CATEGORIES["entertainment"]), "priority": 100},
    {"name": "Zee5", "match_type": "text_contains", "match_value": "zee5", 
     "action_type": "set_category", "action_value": str(CATEGORIES["entertainment"]), "priority": 100},
    {"name": "JioCinema", "match_type": "text_contains", "match_value": "jiocinema", 
     "action_type": "set_category", "action_value": str(CATEGORIES["entertainment"]), "priority": 100},
    {"name": "BookMyShow", "match_type": "text_contains", "match_value": "bookmyshow", 
     "action_type": "set_category", "action_value": str(CATEGORIES["entertainment"]), "priority": 100},
    {"name": "PVR Cinemas", "match_type": "text_contains", "match_value": "pvr", 
     "action_type": "set_category", "action_value": str(CATEGORIES["entertainment"]), "priority": 95},
    {"name": "INOX", "match_type": "text_contains", "match_value": "inox", 
     "action_type": "set_category", "action_value": str(CATEGORIES["entertainment"]), "priority": 95},
    {"name": "Gaming - Steam", "match_type": "text_contains", "match_value": "steam", 
     "action_type": "set_category", "action_value": str(CATEGORIES["entertainment"]), "priority": 95},
    {"name": "Gaming - PlayStation", "match_type": "text_contains", "match_value": "playstation", 
     "action_type": "set_category", "action_value": str(CATEGORIES["entertainment"]), "priority": 95},
    
    # ============ HEALTH & FITNESS (Category 7) ============
    {"name": "Apollo Pharmacy", "match_type": "text_contains", "match_value": "apollo", 
     "action_type": "set_category", "action_value": str(CATEGORIES["health_fitness"]), "priority": 90},
    {"name": "1mg", "match_type": "text_contains", "match_value": "1mg", 
     "action_type": "set_category", "action_value": str(CATEGORIES["health_fitness"]), "priority": 100},
    {"name": "PharmEasy", "match_type": "text_contains", "match_value": "pharmeasy", 
     "action_type": "set_category", "action_value": str(CATEGORIES["health_fitness"]), "priority": 100},
    {"name": "Netmeds", "match_type": "text_contains", "match_value": "netmeds", 
     "action_type": "set_category", "action_value": str(CATEGORIES["health_fitness"]), "priority": 100},
    {"name": "Practo", "match_type": "text_contains", "match_value": "practo", 
     "action_type": "set_category", "action_value": str(CATEGORIES["health_fitness"]), "priority": 100},
    {"name": "Cult.fit", "match_type": "text_contains", "match_value": "cult.fit", 
     "action_type": "set_category", "action_value": str(CATEGORIES["health_fitness"]), "priority": 100},
    {"name": "Cult Fitness", "match_type": "text_contains", "match_value": "cultfit", 
     "action_type": "set_category", "action_value": str(CATEGORIES["health_fitness"]), "priority": 100},
    {"name": "Gym/Fitness", "match_type": "text_contains", "match_value": "gym", 
     "action_type": "set_category", "action_value": str(CATEGORIES["health_fitness"]), "priority": 80},
    {"name": "Hospital", "match_type": "text_contains", "match_value": "hospital", 
     "action_type": "set_category", "action_value": str(CATEGORIES["health_fitness"]), "priority": 85},
    {"name": "Diagnostic Lab", "match_type": "text_contains", "match_value": "diagnostic", 
     "action_type": "set_category", "action_value": str(CATEGORIES["health_fitness"]), "priority": 85},
    {"name": "Pathology", "match_type": "text_contains", "match_value": "pathology", 
     "action_type": "set_category", "action_value": str(CATEGORIES["health_fitness"]), "priority": 85},
    
    # ============ TRANSFERS (Category 8) ============
    {"name": "Self Transfer - UPI", "match_type": "text_contains", "match_value": "self transfer", 
     "action_type": "mark_internal", "action_value": "true", "priority": 100},
    {"name": "Own Account Transfer", "match_type": "text_contains", "match_value": "own account", 
     "action_type": "mark_internal", "action_value": "true", "priority": 100},
    {"name": "NEFT Transfer", "match_type": "text_contains", "match_value": "neft", 
     "action_type": "set_category", "action_value": str(CATEGORIES["transfer"]), "priority": 80},
    {"name": "IMPS Transfer", "match_type": "text_contains", "match_value": "imps", 
     "action_type": "set_category", "action_value": str(CATEGORIES["transfer"]), "priority": 80},
    {"name": "RTGS Transfer", "match_type": "text_contains", "match_value": "rtgs", 
     "action_type": "set_category", "action_value": str(CATEGORIES["transfer"]), "priority": 80},
    
    # ============ SALARY (Category 9) ============
    {"name": "Salary Credit", "match_type": "text_contains", "match_value": "salary", 
     "action_type": "set_category", "action_value": str(CATEGORIES["salary"]), "priority": 100},
    {"name": "Payroll Credit", "match_type": "text_contains", "match_value": "payroll", 
     "action_type": "set_category", "action_value": str(CATEGORIES["salary"]), "priority": 100},
    
    # ============ ATM & CASH ============
    {"name": "ATM Withdrawal", "match_type": "text_contains", "match_value": "atm", 
     "action_type": "set_category", "action_value": str(CATEGORIES["other"]), "priority": 90},
    {"name": "Cash Withdrawal", "match_type": "text_contains", "match_value": "cash withdrawal", 
     "action_type": "set_category", "action_value": str(CATEGORIES["other"]), "priority": 90},
]

def create_rules():
    """Create all default rules via API."""
    print("🚀 Creating default categorization rules...\n")
    
    created = 0
    failed = 0
    
    for rule in DEFAULT_RULES:
        try:
            # API expects query parameters, not JSON body
            params = {
                "match_type": rule["match_type"],
                "match_value": rule["match_value"],
                "action_type": rule["action_type"],
                "action_value": rule["action_value"],
                "priority": rule["priority"]
            }
            response = requests.post(f"{BASE_URL}/api/rules", params=params)
            if response.status_code == 200:
                result = response.json()
                print(f"✅ Created: {rule['name']} (ID: {result['id']})")
                created += 1
            else:
                print(f"❌ Failed: {rule['name']} - {response.text}")
                failed += 1
        except Exception as e:
            print(f"❌ Error: {rule['name']} - {str(e)}")
            failed += 1
    
    print(f"\n📊 Summary: {created} created, {failed} failed")
    print(f"Total rules: {len(DEFAULT_RULES)}")
    
    return created, failed

def reapply_rules():
    """Re-apply rules to existing transactions."""
    print("\n🔄 Re-applying rules to existing transactions...")
    try:
        response = requests.post(f"{BASE_URL}/api/rules/reapply")
        if response.status_code == 200:
            result = response.json()
            print(f"✅ Rules re-applied!")
            print(f"   - Transactions processed: {result.get('transactions_processed', 'N/A')}")
            print(f"   - Transactions updated: {result.get('transactions_updated', 'N/A')}")
        else:
            print(f"❌ Failed to re-apply rules: {response.text}")
    except Exception as e:
        print(f"❌ Error re-applying rules: {str(e)}")

def show_stats():
    """Show current statistics."""
    print("\n📈 Current Statistics:")
    try:
        response = requests.get(f"{BASE_URL}/api/admin/stats")
        if response.status_code == 200:
            stats = response.json()
            print(f"   - Total transactions: {stats['transactions']['total']}")
            print(f"   - Categorized: {stats['transactions']['categorized']}")
            print(f"   - Categorization rate: {stats['transactions']['categorization_rate']}")
            print(f"   - Total rules: {stats['entities']['rules']}")
        else:
            print(f"❌ Failed to get stats: {response.text}")
    except Exception as e:
        print(f"❌ Error getting stats: {str(e)}")

if __name__ == "__main__":
    # Create rules
    create_rules()
    
    # Re-apply to existing transactions
    reapply_rules()
    
    # Show updated stats
    show_stats()
