package com.skripsi.backend_api.dto.kafka;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data 
@RequiredArgsConstructor 
@AllArgsConstructor 
public class UserDto {
    private Long userId;
    private String fullName;
    private String roleName;
    private Boolean isActive;
}
