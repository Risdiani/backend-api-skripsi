package com.skripsi.backend_api.dto.user.request;

import lombok.Data;

@Data
public class UserRequest {
    private String username;
    private String password;
    private String email;
    private String fullName;
    private String phone;

    private Long roleId;      // <-- baru
    private Boolean isActive;
}