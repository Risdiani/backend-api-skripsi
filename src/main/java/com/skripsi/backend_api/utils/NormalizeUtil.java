package com.skripsi.backend_api.utils;

import org.springframework.stereotype.Component;

@Component
public class NormalizeUtil {

    public static String normalizeCode(String value) {
        if (value == null) return null;
        return value.trim().toUpperCase();
    }

    public static String normalizeText(String value) {
        if (value == null) return null;
        return value.trim();
    }
}
