package com.printwithai.util;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class ReferenceGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String ALPHANUMERIC = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    public static String generateOrderReference() {
        int year = LocalDate.now().getYear();
        String randomPart = randomString(6);
        return "PAI-" + year + "-" + randomPart;
    }

    public static String generateTrackingToken() {
        return "TRK-" + randomString(4) + "-" + randomString(4) + "-" + randomString(4);
    }

    public static String generateTransactionReference() {
        String timestamp = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        return "TXN-DEMO-" + timestamp + "-" + randomString(6);
    }

    private static String randomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = RANDOM.nextInt(ALPHANUMERIC.length());
            sb.append(ALPHANUMERIC.charAt(index));
        }
        return sb.toString();
    }
}
