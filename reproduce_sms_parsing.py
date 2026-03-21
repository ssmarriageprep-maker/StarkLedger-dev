import re
import math
from datetime import datetime

class ParsedSms:
    def __init__(self, is_transaction, amount=None, currency=None, transaction_type=None, bank=None, account_last_4=None, merchant=None, balance=None, date=None, reference=None, raw_message=None, reason=None):
        self.is_transaction = is_transaction
        self.amount = amount
        self.currency = currency
        self.transaction_type = transaction_type
        self.bank = bank
        self.account_last_4 = account_last_4
        self.merchant = merchant
        self.balance = balance
        self.date = date
        self.reference = reference
        self.raw_message = raw_message
        self.reason = reason

    def __str__(self):
        return f"ParsedSms(is_transaction={self.is_transaction}, amount={self.amount}, merchant={self.merchant}, type={self.transaction_type}, reason={self.reason})"

class SmsParser:
    REJECT_KEYWORDS = [
        "offer", "recharge", "data", "validity", "ott", "xstream", "bill reminder", "bill due", "statement", "minimum due",
        "emi", "loan", "credit card offer", "cashback offer", "promo", "otp", "verification code",
        "due by", "min payment", "minimum payment"
    ]

    REJECT_PATTERN = re.compile(r"(?i)\b(?:" + "|".join(map(re.escape, REJECT_KEYWORDS)) + r")\b")

    INDICATOR_ACTION = ["sent", "paid", "debited", "spent", "transferred", "transfer", "credited", "received", "payment", "withdrawn", "deducted"]
    INDICATOR_BANK = ["HDFC", "SBI", "ICICI", "AXIS", "KOTAK", "PNB", "IDFC", "YES", "CANARA", "FEDERAL"]
    INDICATOR_ACCOUNT = ["A/C", "Acct", "Account", "card"]
    INDICATOR_ACCOUNT_PATTERN = re.compile(r"(?i)(?:[*X]{1,}|XX|card ending\s+|card\s+[*X]*)[0-9]{2,}")
    INDICATOR_METHOD = ["UPI", "IMPS", "NEFT", "RTGS"]
    INDICATOR_REF = ["Ref", "Txn", "UTR", "Reference"]
    INDICATOR_CURRENCY = ["Rs", "INR", "₹"]

    DEBIT_KEYWORDS = ["sent", "paid", "debited", "spent", "transfer", "transferred", "withdrawn", "deducted", "made a payment", "done"]
    CREDIT_KEYWORDS = ["credited", "received", "refund"]

    AMOUNT_PATTERN = re.compile(r"(?:Rs\.?|INR|₹)\s*([0-9]+(?:,[0-9]+)*(?:\.[0-9]{1,2})?)", re.IGNORECASE)
    BANK_PATTERN = re.compile(r"\b(HDFC|SBI|ICICI|AXIS|KOTAK|PNB|IDFC|YES|CANARA|FEDERAL)\b", re.IGNORECASE)
    ACCOUNT_PATTERN = re.compile(r"(?:A/C|Acct|Account|Card ending|Card)\s+([*X]*[0-9]{2,})", re.IGNORECASE)
    MERCHANT_PATTERN = re.compile(r"(?:To|Paid to|Transferred to|Sent to|at|for|spent at|spent on)\s+([A-Za-z0-9\s&'.\-/:]{2,60})", re.IGNORECASE)
    DATE_PATTERN = re.compile(r"(?:On\s+)?([0-9]{2}[/-][0-9]{2}[/-](?:[0-9]{4}|[0-9]{2})|[0-9]{2}-[A-Za-z]{3}-[0-9]{2})", re.IGNORECASE)
    REF_PATTERN = re.compile(r"(?:Ref|Txn ID|Txn|UTR|Reference)[:\s\-]*([A-Za-z0-9]+)", re.IGNORECASE)
    BALANCE_PATTERN = re.compile(r"(?:Avl Bal|Available Balance|Bal|Balance)[:\s\-]*(?:Rs\.?|INR|₹)?\s*([0-9]+(?:,[0-9]+)*(?:\.[0-9]{1,2})?)", re.IGNORECASE)

    @staticmethod
    def parse_sms(sender, body, timestamp):
        lower_body = body.lower()

        # STEP 1 — HARD FILTER
        if SmsParser.REJECT_PATTERN.search(body):
            return ParsedSms(is_transaction=False, reason="Promotional or non-bank message", raw_message=body)

        # STEP 2 — TRANSACTION CONFIRMATION RULE
        indicators_found = 0
        if any(keyword.lower() in lower_body for keyword in SmsParser.INDICATOR_ACTION): indicators_found += 1
        if any(keyword.lower() in lower_body for keyword in SmsParser.INDICATOR_BANK) or any(keyword.lower() in sender.lower() for keyword in SmsParser.INDICATOR_BANK): indicators_found += 1
        if any(keyword.lower() in lower_body for keyword in SmsParser.INDICATOR_ACCOUNT) or SmsParser.INDICATOR_ACCOUNT_PATTERN.search(body): indicators_found += 1
        if any(keyword.lower() in lower_body for keyword in SmsParser.INDICATOR_METHOD) or "upi" in lower_body: indicators_found += 1
        if any(keyword.lower() in lower_body for keyword in SmsParser.INDICATOR_REF) or SmsParser.REF_PATTERN.search(body): indicators_found += 1
        if any(keyword.lower() in lower_body for keyword in SmsParser.INDICATOR_CURRENCY): indicators_found += 1 # New: Currency symbol is a strong indicator
        if SmsParser.AMOUNT_PATTERN.search(body): indicators_found += 1 # New: Finding a money pattern is an indicator

        if indicators_found < 2:
            return ParsedSms(is_transaction=False, reason=f"Insufficient transaction confidence ({indicators_found})", raw_message=body)

        # STEP 3 — AMOUNT EXTRACTION
        balance_ranges = []
        for match in SmsParser.BALANCE_PATTERN.finditer(body):
            balance_ranges.append((match.start(), match.end()))

        amount_str = None
        for match in SmsParser.AMOUNT_PATTERN.finditer(body):
            start, end = match.start(), match.end()
            is_balance = False
            for b_start, b_end in balance_ranges:
                if start >= b_start and end <= b_end:
                    is_balance = True
                    break
            if not is_balance:
                amount_str = match.group(1)
                break

        if amount_str:
            clean_amount_str = amount_str.replace(",", "")
            try:
                amount = float(clean_amount_str)
            except ValueError:
                amount = 0.0
        else:
            return ParsedSms(is_transaction=False, reason="Invalid or missing amount", raw_message=body)

        if amount <= 0.0:
             return ParsedSms(is_transaction=False, reason="Amount cannot be 0.0", raw_message=body)

        # STEP 6 — TRANSACTION TYPE
        type_ = None
        first_credit = min([lower_body.find(keyword.lower()) for keyword in SmsParser.CREDIT_KEYWORDS if keyword.lower() in lower_body] or [float('inf')])
        first_debit = min([lower_body.find(keyword.lower()) for keyword in SmsParser.DEBIT_KEYWORDS if keyword.lower() in lower_body] or [float('inf')])

        if first_credit < first_debit:
            type_ = "credit"
        elif first_debit < first_credit:
            type_ = "debit"
        
        if type_ is None:
             return ParsedSms(is_transaction=False, reason="Unclear transaction type", raw_message=body)

        # STEP 5 — BANK & ACCOUNT EXTRACTION
        bank = None
        bank_match = SmsParser.BANK_PATTERN.search(body)
        if bank_match:
            bank = bank_match.group(1).upper()
        else:
            for b_name in SmsParser.INDICATOR_BANK:
                if b_name.lower() in sender.lower():
                    bank = b_name.upper()
                    break

        account_last_4 = None
        account_match = SmsParser.ACCOUNT_PATTERN.search(body)
        if account_match:
            full_acc = account_match.group(1)
            account_last_4 = "".join(filter(str.isdigit, full_acc))[-4:]
            if len(account_last_4) < 2: account_last_4 = None

        # STEP 4 — SENDER / MERCHANT EXTRACTION
        merchant = None
        merchant_match = SmsParser.MERCHANT_PATTERN.search(body)
        if merchant_match:
            candidate = merchant_match.group(1).strip()
            
            delimiters_regex = re.compile(r"(?i)\s+(?:via|on|from|a/c|account|ref|txn|utr|to|card ending|at|for)\b")
            match = delimiters_regex.search(candidate)
            if match:
                candidate = candidate[:match.start()].strip()

            candidate = re.sub(r"\s+", " ", candidate).strip()
            candidate = re.sub(r"[.,:\-]+$", "", candidate).strip()

            is_phone = bool(re.match(r".*\d{10,}.*", candidate))
            is_url = "http" in candidate or ".com" in candidate or ".in" in candidate
            is_bank = False
            for b in SmsParser.INDICATOR_BANK:
                if any(part.lower() == b.lower() for part in candidate.split()):
                    is_bank = True
                    break

            if not is_phone and not is_url and not is_bank and len(candidate) > 2:
                merchant = candidate

        # STEP 7 — DATE EXTRACTION
        date = None
        date_match = SmsParser.DATE_PATTERN.search(body)
        if date_match:
            date = date_match.group(1)

        # OTHER EXTRACTIONS
        reference = None
        ref_match = SmsParser.REF_PATTERN.search(body)
        if ref_match:
            reference = ref_match.group(1)

        balance = None
        b_match = SmsParser.BALANCE_PATTERN.search(body)
        if b_match:
            balance_str = b_match.group(1)
            if balance_str:
                try:
                    balance = float(balance_str.replace(",", ""))
                except ValueError:
                    pass

        return ParsedSms(is_transaction=True, amount=amount, currency="INR", transaction_type=type_, bank=bank, account_last_4=account_last_4, merchant=merchant, balance=balance, date=date, reference=reference, raw_message=body)


# TEST CASES
test_messages = [
    # Valid
    ("HDFCBK", "Rs.318.00 sent to Dreamplug Service Private Limited from HDFC Bank A/C *3263 on 25-10-2025. Ref: 566413406309"),
    ("SBIUPI", "₹1,250.50 debited from SBI A/C XX4567 for AMAZON INDIA on 17-OCT-25. UPI Ref: UPI123456"),
    ("ICICIB", "Rs 5000.00 credited to your ICICI Bank account *1234 on 01/01/2026. Salary transfer."),
    # Edge Case: Missing Merchant
    ("AXISBK", "Rs 943.00 spent using AXIS Bank card *8899 on 15/12/2025"), 
    # Invalid
    ("AIRTEL", "Recharge with Rs.318 and get 2GB/day + OTT benefits. Valid for 28 days. Call 9606XXX997"),
    ("HDFCBK", "Your HDFC credit card bill of Rs 5000 is due on 15-Jan-2026. Minimum due: Rs 500"),
    # New failed case (Hypothetical)
    ("BANK", "Txn of INR 200.00 done on 12-12-25. Avl Bal: INR 5000.00"),
    ("GPAY", "You paid ₹350 to Swiggy")
]

print("Running SMS Parsing Test...")
for sender, body in test_messages:
    result = SmsParser.parse_sms(sender, body, 0)
    print(f"\nMsg: {body}")
    print(f"Result: {result}")
