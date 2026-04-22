package com.skripsi.backend_api.controller.user;

import com.skripsi.backend_api.utils.BaseResponse;
import com.skripsi.backend_api.dto.user.request.UserRequest;
import com.skripsi.backend_api.dto.user.response.UserResponse;
import com.skripsi.backend_api.utils.PageResponse;
import com.skripsi.backend_api.entity.User;
import com.skripsi.backend_api.service.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    private static UserResponse toResponse(User u) {
        return UserResponse.builder()
                .id(u.getId())
                .username(u.getUsername())
                .email(u.getEmail())
                .fullName(u.getFullName())
                .phone(u.getPhone())
                .role(u.getRole() == null ? null : u.getRole().name())
                .isActive(u.getIsActive())
                .build();
    }

    @GetMapping
    public ResponseEntity<BaseResponse<Object>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        log.info("Fetch all users with pagination, page: {}, size: {}", page, size);
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<User> userPage = userService.findAll(pageable);
            PageResponse<UserResponse> result = PageResponse.from(
                userPage.map(UserController::toResponse)
            );
            log.info("Berhasil fetch {} user(s) pada page {}", result.getContent().size(), page);
            return ResponseEntity.ok(BaseResponse.ok("Data pengguna berhasil diambil", result));
        } catch (Exception e) {
            log.error("Error saat fetch all users", e);
            throw e;
        }
    }

    @GetMapping("/profile")
    public ResponseEntity<BaseResponse<Object>> profile() {
        log.info("Fetch current user profile");
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            log.debug("Current logged-in user: {}", username);

            UserResponse user = toResponse(userService.findByUsername(username));
            log.info("Berhasil fetch profile untuk user: {}", username);
            return ResponseEntity.ok(BaseResponse.ok("Profile user berhasil diambil", user));
        } catch (IllegalArgumentException e) {
            log.warn("User profile tidak ditemukan");
            throw e;
        } catch (Exception e) {
            log.error("Error saat fetch user profile", e);
            throw e;
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<BaseResponse<Object>> detail(@PathVariable Long id) {
        log.info("Fetch user detail dengan ID: {}", id);
        try {
            UserResponse user = toResponse(userService.findById(id));
            log.info("Berhasil fetch user detail, username: {}", user.getUsername());
            return ResponseEntity.ok(BaseResponse.ok("Data pengguna berhasil diambil", user));
        } catch (IllegalArgumentException e) {
            log.warn("User tidak ditemukan dengan ID: {}", id);
            throw e;
        } catch (Exception e) {
            log.error("Error saat fetch user detail dengan ID: {}", id, e);
            throw e;
        }
    }

    @PostMapping
    public ResponseEntity<BaseResponse<Object>> create(@RequestBody UserRequest req) {
        log.info("Create user baru, username: {}, email: {}", req.getUsername(), req.getEmail());
        try {
            User u = userService.create(req);
            log.info("User berhasil dibuat dengan ID: {}, username: {}", u.getId(), u.getUsername());
            return ResponseEntity.ok(BaseResponse.ok("Pengguna berhasil dibuat", toResponse(u)));
        } catch (IllegalArgumentException e) {
            log.warn("Gagal create user: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error saat create user", e);
            throw e;
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<BaseResponse<Object>> update(@PathVariable Long id, @RequestBody UserRequest req) {
        log.info("Update user dengan ID: {}", id);
        try {
            UserResponse user = toResponse(userService.update(id, req));
            log.info("User berhasil diupdate, ID: {}, username: {}", id, user.getUsername());
            return ResponseEntity.ok(BaseResponse.ok("Pengguna berhasil diperbarui", user));
        } catch (IllegalArgumentException e) {
            log.warn("Gagal update user dengan ID: {}, reason: {}", id, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error saat update user dengan ID: {}", id, e);
            throw e;
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<BaseResponse<String>> delete(@PathVariable Long id) {
        log.info("Delete user dengan ID: {}", id);
        try {
            userService.delete(id);
            log.info("User dengan ID: {} berhasil dihapus", id);
            return ResponseEntity.ok(BaseResponse.info("Pengguna berhasil dihapus"));
        } catch (Exception e) {
            log.error("Error saat delete user dengan ID: {}", id, e);
            throw e;
        }
    }
}