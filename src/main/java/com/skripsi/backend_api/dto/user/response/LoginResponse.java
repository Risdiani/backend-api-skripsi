package com.skripsi.backend_api.dto.user.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponse {
    private String token;
    private String tokenType; // "Bearer"
    private Long userId;
    private String username;
    private String role;
}