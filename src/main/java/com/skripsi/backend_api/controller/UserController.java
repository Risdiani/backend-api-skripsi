package com.skripsi.backend_api.controller;

import com.skripsi.backend_api.dto.BaseResponse;
import com.skripsi.backend_api.dto.user.request.UserRequest;
import com.skripsi.backend_api.dto.user.response.UserResponse;
import com.skripsi.backend_api.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<BaseResponse<Object>> list() {
        List<UserResponse> data = userService.findAll();
        return ResponseEntity.ok(BaseResponse.ok(data));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BaseResponse<Object>> detail(@PathVariable Long id) {
        UserResponse data = userService.findById(id);
        return ResponseEntity.ok(BaseResponse.ok(data));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BaseResponse<Object>> update(@PathVariable Long id, @RequestBody UserRequest req) {
        UserResponse data = userService.update(id, req);
        return ResponseEntity.ok(BaseResponse.ok(data));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<BaseResponse<Object>> delete(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.ok(BaseResponse.create(200, true, "User deleted", null));
    }
}