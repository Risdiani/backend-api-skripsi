package com.skripsi.backend_api.security;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TokenBlacklistService {

    // key: jti, value: expiryEpochMillis
    private final Map<String, Long> revoked = new ConcurrentHashMap<>();

    public void revoke(String jti, Date expiresAt) {
        if (jti == null || expiresAt == null) return;
        revoked.put(jti, expiresAt.getTime());
    }

    public boolean isRevoked(String jti) {
        if (jti == null) return false;

        Long exp = revoked.get(jti);
        if (exp == null) return false;

        // cleanup token yang sudah expired
        if (exp < Instant.now().toEpochMilli()) {
            revoked.remove(jti);
            return false;
        }
        return true;
    }
}