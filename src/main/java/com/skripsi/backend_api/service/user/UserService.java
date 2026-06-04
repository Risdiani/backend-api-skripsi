package com.skripsi.backend_api.service.user;

import com.skripsi.backend_api.dto.user.request.UserRequest;
import com.skripsi.backend_api.dto.user.response.UserResponse;
import com.skripsi.backend_api.entity.Role;
import com.skripsi.backend_api.entity.User;
import com.skripsi.backend_api.repository.RoleRepository;
import com.skripsi.backend_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    private UserResponse toResponse(User u) {
        return UserResponse.builder()
                .id(u.getId())
                .username(u.getUsername())
                .email(u.getEmail())
                .fullName(u.getFullName())
                .phone(u.getPhone())
                .role(u.getRole() == null ? null : u.getRole().getName())
                .isActive(u.getIsActive())
                .build();
    }

    public List<UserResponse> findAll() {
        log.info("UserService.findAll - fetching all users");
        List<UserResponse> result = userRepository.findAll().stream().map(this::toResponse).toList();
        log.info("UserService.findAll - total={}", result.size());
        return result;
    }

    public UserResponse findById(Long id) {
        log.info("UserService.findById - id={}", id);
        User u = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        log.info("UserService.findById - found username={}", u.getUsername());
        return toResponse(u);
    }

    public UserResponse create(UserRequest req) {
        log.info("UserService.create - username={}, email={}, roleId={}, isActive={}",
                req.getUsername(), req.getEmail(), req.getRoleId(), req.getIsActive());

        if (userRepository.existsByUsername(req.getUsername())) {
            log.warn("UserService.create - username already used: {}", req.getUsername());
            throw new IllegalArgumentException("Username already used");
        }
        if (userRepository.existsByEmail(req.getEmail())) {
            log.warn("UserService.create - email already used: {}", req.getEmail());
            throw new IllegalArgumentException("Email already used");
        }
        if (req.getPassword() == null || req.getPassword().isBlank()) {
            log.warn("UserService.create - password is missing for username={}", req.getUsername());
            throw new IllegalArgumentException("Password is required");
        }
        if (req.getRoleId() == null) {
            log.warn("UserService.create - roleId is missing for username={}", req.getUsername());
            throw new IllegalArgumentException("roleId is required");
        }

        Role role = roleRepository.findById(req.getRoleId())
                .orElseThrow(() -> new IllegalArgumentException("Role not found"));

        User u = User.builder()
                .username(req.getUsername())
                .password(passwordEncoder.encode(req.getPassword())) // jangan pernah log password
                .email(req.getEmail())
                .fullName(req.getFullName())
                .phone(req.getPhone())
                .role(role)
                .isActive(req.getIsActive() == null ? true : req.getIsActive())
                .build();

        User saved = userRepository.save(u);
        log.info("UserService.create - created id={}, username={}", saved.getId(), saved.getUsername());
        return toResponse(saved);
    }

    public UserResponse update(Long id, UserRequest req) {
        log.info("UserService.update - id={}, username={}, email={}, roleId={}, isActive={}",
                id, req.getUsername(), req.getEmail(), req.getRoleId(), req.getIsActive());

        User u = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (req.getEmail() != null && !req.getEmail().equals(u.getEmail())
                && userRepository.existsByEmail(req.getEmail())) {
            log.warn("UserService.update - email already used: {}", req.getEmail());
            throw new IllegalArgumentException("Email already used");
        }

        if (req.getUsername() != null) u.setUsername(req.getUsername());
        if (req.getEmail() != null) u.setEmail(req.getEmail());
        if (req.getFullName() != null) u.setFullName(req.getFullName());
        if (req.getPhone() != null) u.setPhone(req.getPhone());
        if (req.getIsActive() != null) u.setIsActive(req.getIsActive());

        if (req.getRoleId() != null) {
            Role role = roleRepository.findById(req.getRoleId())
                    .orElseThrow(() -> new IllegalArgumentException("Role not found"));
            u.setRole(role);
        }

        if (req.getPassword() != null && !req.getPassword().isBlank()) {
            u.setPassword(passwordEncoder.encode(req.getPassword()));
        }

        User saved = userRepository.save(u);
        log.info("UserService.update - updated id={}, username={}", saved.getId(), saved.getUsername());
        return toResponse(saved);
    }

    public void delete(Long id) {
        log.info("UserService.delete - id={}", id);
        userRepository.deleteById(id);
        log.info("UserService.delete - deleted id={}", id);
    }
}