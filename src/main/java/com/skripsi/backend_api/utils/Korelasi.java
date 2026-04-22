package com.skripsi.backend_api.utils;

// ── Enum Korelasi ─────────────────────────────────────────
// positif  : lift > 1  → A dan B saling mendorong
// negatif  : lift < 1  → A dan B saling menghambat
// independen: lift = 1 → tidak ada hubungan
public enum Korelasi {
    POSITIF, 
    NEGATIF, 
    INDEPENDEN
}