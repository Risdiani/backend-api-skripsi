package com.skripsi.backend_api.dto.user.request;

import com.skripsi.backend_api.utils.Role;
import lombok.Data;

@Data
public class UserRequest {
    private String username;
    private String password; // optional saat update
    private String email;
    private String fullName;
    private String phone;
    private Role role;
    private Boolean isActive;
}
