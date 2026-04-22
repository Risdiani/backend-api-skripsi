package com.skripsi.backend_api.controller.user;

import com.skripsi.backend_api.dto.user.request.UserRequest;
import com.skripsi.backend_api.dto.user.response.UserResponse;
import com.skripsi.backend_api.entity.User;
import com.skripsi.backend_api.service.user.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

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
    public List<UserResponse> list() {
        return userService.findAll().stream().map(UserController::toResponse).toList();
    }

    @GetMapping("/{id}")
    public UserResponse detail(@PathVariable Long id) {
        return toResponse(userService.findById(id));
    }

    @PostMapping
    public ResponseEntity<UserResponse> create(@RequestBody UserRequest req) {
        User u = userService.create(req);
        return ResponseEntity.ok(toResponse(u));
    }

    @PutMapping("/{id}")
    public UserResponse update(@PathVariable Long id, @RequestBody UserRequest req) {
        return toResponse(userService.update(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}