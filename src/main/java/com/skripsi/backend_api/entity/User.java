package com.skripsi.backend_api.entity;

import com.skripsi.backend_api.utils.Role;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
 
import java.time.LocalDateTime;
 
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
 
    @Column(nullable = false, unique = true, length = 64)
    private String username;
 
    @Column(nullable = false, length = 255)
    private String password;
 
    @Column(nullable = false, unique = true, length = 255)
    private String email;
 
    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;
 
    @Column(length = 20)
    private String phone;
 
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Role role;
 
    @Column(length = 255)
    private String photo;
 
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
 
    @Column(name = "last_login")
    private LocalDateTime lastLogin;
 
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
 
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
 
    // ── Enum Role ────────────────────────────────────────────
    // public enum Role {
    //     admin, staff
    // }
}
