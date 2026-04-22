package com.skripsi.backend_api.utils;

import java.math.BigDecimal;

public final class HitungTotal {

    private HitungTotal() {
        // utility class
    }

    public static BigDecimal hitungSubtotal(BigDecimal hargaSatuan, Integer jumlah) {
        if (hargaSatuan == null || jumlah == null) {
            return BigDecimal.ZERO;
        }
        return hargaSatuan.multiply(BigDecimal.valueOf(jumlah));
    }
}