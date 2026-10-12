package com.printwithai.util;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UpiNotificationParser {

    private static final Pattern AMOUNT_PATTERN = Pattern.compile(
            "(?:₹|rs\\.?|inr|credited\\s+(?:by|with)?\\s*(?:rs\\.?|inr|₹)?)\\s*([0-9]+(?:\\.[0-9]{1,2})?)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern ALT_AMOUNT_PATTERN = Pattern.compile(
            "(?:received|paid|transferred)\\s+(?:₹|rs\\.?|inr)?\\s*([0-9]+(?:\\.[0-9]{1,2})?)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern UTR_EXPLICIT_PATTERN = Pattern.compile(
            "(?:upi\\s*ref(?:\\s*no)?|utr(?:\\s*no)?|ref(?:\\s*no)?|txn(?:\\s*id)?|transaction\\s*id|reference|upi\\/|ref\\s*[:\\-])[:\\s\\/-]*([0-9A-Za-z]{8,20})",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern TWELVE_DIGIT_PATTERN = Pattern.compile("\\b([0-9]{12})\\b");

    public static class ParsedNotification {
        private final BigDecimal amount;
        private final String utr;

        public ParsedNotification(BigDecimal amount, String utr) {
            this.amount = amount;
            this.utr = utr;
        }

        public BigDecimal getAmount() { return amount; }
        public String getUtr() { return utr; }
        public boolean isValid() { return amount != null && utr != null && !utr.isBlank(); }
    }

    public static ParsedNotification parse(String text) {
        if (text == null || text.isBlank()) {
            return new ParsedNotification(null, null);
        }

        BigDecimal amount = extractAmount(text);
        String utr = extractUtr(text);

        return new ParsedNotification(amount, utr);
    }

    public static BigDecimal extractAmount(String text) {
        if (text == null) return null;
        Matcher m = AMOUNT_PATTERN.matcher(text);
        if (m.find()) {
            try {
                return new BigDecimal(m.group(1));
            } catch (Exception ignored) {}
        }

        Matcher altMatcher = ALT_AMOUNT_PATTERN.matcher(text);
        if (altMatcher.find()) {
            try {
                return new BigDecimal(altMatcher.group(1));
            } catch (Exception ignored) {}
        }
        return null;
    }

    public static String extractUtr(String text) {
        if (text == null) return null;
        Matcher m = UTR_EXPLICIT_PATTERN.matcher(text);
        if (m.find()) {
            return m.group(1).trim().toUpperCase();
        }

        // Fallback: look for 12-digit standard UPI UTR
        Matcher twelveMatcher = TWELVE_DIGIT_PATTERN.matcher(text);
        if (twelveMatcher.find()) {
            return twelveMatcher.group(1).trim().toUpperCase();
        }

        return null;
    }
}
