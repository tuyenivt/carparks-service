package com.example.carpark.util;

public class NumberUtil {
    private NumberUtil() {
    }

    public static int parseIntQuietly(String str) {
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
