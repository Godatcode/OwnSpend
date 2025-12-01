/**
 * Google Apps Script for OwnSpend Transactions Sheet
 * 
 * Setup Instructions:
 * 1. Create a new Google Sheet
 * 2. Go to Extensions > Apps Script
 * 3. Copy this code into Code.gs
 * 4. Click Deploy > New Deployment
 * 5. Choose "Web app"
 * 6. Set "Execute as" to "Me"
 * 7. Set "Who has access" to "Anyone" (or your preference)
 * 8. Click Deploy and copy the Web App URL
 * 9. Add the URL to backend/.env as GOOGLE_SHEETS_WEBHOOK_URL
 */

// Configuration - Update this to your sheet name
const SHEET_NAME = 'Transactions';

/**
 * Initialize the sheet with headers
 */
function setupSheet() {
  var sheet = getOrCreateSheet();
  
  // Clear existing content
  sheet.clear();
  
  // Set headers
  var headers = [
    'Transaction ID',
    'Transaction Time',
    'Ingested At',
    'Amount',
    'Direction',
    'Channel',
    'Account Name',
    'Bank Name',
    'Account Mask',
    'Merchant',
    'Category',
    'Raw Merchant ID',
    'Description',
    'Internal Transfer',
    'Dedupe Key'
  ];
  
  sheet.getRange(1, 1, 1, headers.length).setValues([headers]);
  
  // Format header row
  sheet.getRange(1, 1, 1, headers.length)
    .setBackground('#4285f4')
    .setFontColor('#ffffff')
    .setFontWeight('bold')
    .setHorizontalAlignment('center');
  
  // Freeze header row
  sheet.setFrozenRows(1);
  
  // Auto-resize columns
  sheet.autoResizeColumns(1, headers.length);
  
  Logger.log('Sheet setup complete!');
}

/**
 * Get or create the transactions sheet
 */
function getOrCreateSheet() {
  var spreadsheet = SpreadsheetApp.getActiveSpreadsheet();
  var sheet = spreadsheet.getSheetByName(SHEET_NAME);
  
  if (!sheet) {
    sheet = spreadsheet.insertSheet(SHEET_NAME);
    Logger.log('Created new sheet: ' + SHEET_NAME);
  }
  
  return sheet;
}

/**
 * Handle POST requests from the backend
 */
function doPost(e) {
  try {
    var data = JSON.parse(e.postData.contents);
    
    // Append transaction to sheet
    appendTransaction(data);
    
    return ContentService.createTextOutput(
      JSON.stringify({
        'status': 'success',
        'transaction_id': data.transaction_id
      })
    ).setMimeType(ContentService.MimeType.JSON);
    
  } catch (error) {
    Logger.log('Error: ' + error.toString());
    
    return ContentService.createTextOutput(
      JSON.stringify({
        'status': 'error',
        'message': error.toString()
      })
    ).setMimeType(ContentService.MimeType.JSON);
  }
}

/**
 * Handle GET requests (for testing)
 */
function doGet(e) {
  return ContentService.createTextOutput(
    JSON.stringify({
      'status': 'OwnSpend Google Sheets Integration is active',
      'sheet': SHEET_NAME,
      'timestamp': new Date().toISOString()
    })
  ).setMimeType(ContentService.MimeType.JSON);
}

/**
 * Append a transaction to the sheet
 */
function appendTransaction(data) {
  var sheet = getOrCreateSheet();
  
  // Check if headers exist, if not, set them up
  if (sheet.getLastRow() === 0) {
    setupSheet();
  }
  
  // Format transaction time
  var transactionTime = data.transaction_time ? 
    new Date(data.transaction_time).toLocaleString() : '';
  
  var ingestedAt = data.ingested_at ?
    new Date(data.ingested_at).toLocaleString() : '';
  
  // Prepare row data
  var rowData = [
    data.transaction_id || '',
    transactionTime,
    ingestedAt,
    data.amount || 0,
    data.direction || '',
    data.channel || '',
    data.account_name || '',
    data.bank_name || '',
    data.account_mask || '',
    data.merchant_display_name || '',
    data.category_name || '',
    data.raw_merchant_identifier || '',
    data.description || '',
    data.is_internal_transfer ? 'Yes' : 'No',
    data.dedupe_key || ''
  ];
  
  // Append row
  sheet.appendRow(rowData);
  
  // Format amount cell based on direction
  var lastRow = sheet.getLastRow();
  var amountCell = sheet.getRange(lastRow, 4); // Amount column
  
  if (data.direction === 'DEBIT') {
    amountCell.setNumberFormat('-#,##0.00').setFontColor('#d32f2f');
  } else {
    amountCell.setNumberFormat('#,##0.00').setFontColor('#388e3c');
  }
  
  Logger.log('Transaction appended: ' + data.transaction_id);
}

/**
 * Clear all transactions (keep headers)
 */
function clearTransactions() {
  var sheet = getOrCreateSheet();
  
  if (sheet.getLastRow() > 1) {
    sheet.deleteRows(2, sheet.getLastRow() - 1);
    Logger.log('Cleared all transactions');
  }
}

/**
 * Test function to add sample data
 */
function testAddTransaction() {
  var sampleData = {
    transaction_id: 'test-' + new Date().getTime(),
    transaction_time: new Date().toISOString(),
    ingested_at: new Date().toISOString(),
    amount: 150.50,
    direction: 'DEBIT',
    channel: 'UPI',
    account_name: 'Kotak X1415',
    bank_name: 'Kotak',
    account_mask: 'X1415',
    merchant_display_name: 'Test Merchant',
    category_name: 'Food & Dining',
    raw_merchant_identifier: 'test@upi',
    description: 'Test transaction',
    is_internal_transfer: false,
    dedupe_key: 'test-dedupe-key'
  };
  
  appendTransaction(sampleData);
  Logger.log('Test transaction added');
}
