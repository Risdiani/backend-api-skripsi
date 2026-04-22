package com.skripsi.backend_api.dto.user.request;

import lombok.Data;

@Data
public class LoginRequest {
    private String username;
    private String password;
}