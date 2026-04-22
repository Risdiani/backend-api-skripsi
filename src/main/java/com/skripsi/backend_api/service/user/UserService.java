package com.skripsi.backend_api.service.user;

import com.skripsi.backend_api.dto.user.request.UserRequest;
import com.skripsi.backend_api.entity.User;
import com.skripsi.backend_api.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public Page<User> findAll(Pageable pageable) {
        log.debug("Fetching all users dari database dengan pagination");
        return userRepository.findAll(pageable);
    }

    public User findByUsername(String username) {
        log.debug("Fetching user dengan username: {}", username);
        return userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("User tidak ditemukan dengan username: {}", username);
                    return new IllegalArgumentException("User not found");
                });
    }

    public User findById(Long id) {
        log.debug("Fetching user dengan ID: {}", id);
        return userRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("User tidak ditemukan dengan ID: {}", id);
                    return new IllegalArgumentException("User not found");
                });
    }

    public User create(UserRequest req) {
        log.info("Creating new user, username: {}", req.getUsername());

        if (userRepository.existsByUsername(req.getUsername())) {
            log.warn("Username already used: {}", req.getUsername());
            throw new IllegalArgumentException("Username already used");
        }
        if (userRepository.existsByEmail(req.getEmail())) {
            log.warn("Email already used: {}", req.getEmail());
            throw new IllegalArgumentException("Email already used");
        }
        if (req.getPassword() == null || req.getPassword().isBlank()) {
            log.warn("Password is required untuk user: {}", req.getUsername());
            throw new IllegalArgumentException("Password is required");
        }

        User u = User.builder()
                .username(req.getUsername())
                .password(passwordEncoder.encode(req.getPassword()))
                .email(req.getEmail())
                .fullName(req.getFullName())
                .phone(req.getPhone())
                .role(req.getRole())
                .isActive(req.getIsActive() == null ? true : req.getIsActive())
                .build();

        User savedUser = userRepository.save(u);
        log.info("User berhasil dibuat dengan ID: {}", savedUser.getId());
        return savedUser;
    }

    public User update(Long id, UserRequest req) {
        log.info("Updating user dengan ID: {}", id);
        User u = findById(id);

        if (req.getEmail() != null && !req.getEmail().equals(u.getEmail())
                && userRepository.existsByEmail(req.getEmail())) {
            log.warn("Email already used: {}", req.getEmail());
            throw new IllegalArgumentException("Email already used");
        }

        if (req.getUsername() != null) {
            log.debug("Update username dari {} menjadi {}", u.getUsername(), req.getUsername());
            u.setUsername(req.getUsername());
        }
        if (req.getEmail() != null) {
            log.debug("Update email dari {} menjadi {}", u.getEmail(), req.getEmail());
            u.setEmail(req.getEmail());
        }
        if (req.getFullName() != null) u.setFullName(req.getFullName());
        if (req.getPhone() != null) u.setPhone(req.getPhone());
        if (req.getRole() != null) u.setRole(req.getRole());
        if (req.getIsActive() != null) u.setIsActive(req.getIsActive());

        if (req.getPassword() != null && !req.getPassword().isBlank()) {
            log.debug("Update password untuk user ID: {}", id);
            u.setPassword(passwordEncoder.encode(req.getPassword()));
        }

        User updatedUser = userRepository.save(u);
        log.info("User dengan ID: {} berhasil diupdate", id);
        return updatedUser;
    }

    public void delete(Long id) {
        log.info("Deleting user dengan ID: {}", id);
        findById(id); // Validasi user ada
        userRepository.deleteById(id);
        log.info("User dengan ID: {} berhasil dihapus", id);
    }
}