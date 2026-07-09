package com.skripsi.backend_api.utils;

// ── Enum Korelasi ─────────────────────────────────────────
// positif  : lift > 1  → A dan B saling mendorong
// negatif  : lift < 1  → A dan B saling menghambat
// independen: lift = 1 → tidak ada hubungan
public enum Korelasi {
    POSITIF("positif"),
    NEGATIF("negatif"),
    INDEPENDEN("independen");

    private final String dbValue;

    Korelasi(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public static Korelasi fromDbValue(String value) {
        if (value == null) {
            return null;
        }
        for (Korelasi korelasi : values()) {
            if (korelasi.dbValue.equalsIgnoreCase(value)) {
                return korelasi;
            }
        }
        throw new IllegalArgumentException("Unknown korelasi: " + value);
    }
}