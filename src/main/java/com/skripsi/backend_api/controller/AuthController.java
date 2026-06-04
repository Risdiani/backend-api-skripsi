package com.skripsi.backend_api.controller;

import com.skripsi.backend_api.dto.BaseResponse;
import com.skripsi.backend_api.dto.user.request.LoginRequest;
import com.skripsi.backend_api.dto.user.request.UserRequest;
import com.skripsi.backend_api.dto.user.response.LoginResponse;
import com.skripsi.backend_api.dto.user.response.UserResponse;
import com.skripsi.backend_api.entity.User;
import com.skripsi.backend_api.repository.UserRepository;
import com.skripsi.backend_api.security.JwtService;
import com.skripsi.backend_api.security.TokenBlacklistService;
import com.skripsi.backend_api.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final TokenBlacklistService tokenBlacklistService;
    private final UserService userService;

    @PostMapping("/login")
    public ResponseEntity<BaseResponse<Object>> login(@RequestBody LoginRequest req) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword())
        );

        String token = jwtService.generateToken(auth.getName());
        User u = userRepository.findByUsername(auth.getName()).orElseThrow();

        LoginResponse payload = LoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(u.getId())
                .username(u.getUsername())
                .role(u.getRole() == null ? null : u.getRole().getName())
                .build();

        return ResponseEntity.ok(BaseResponse.ok(payload));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return ResponseEntity.noContent().build();
        }

        String token = authorization.substring(7);
        String jti = jwtService.extractJti(token);
        tokenBlacklistService.revoke(jti, jwtService.extractExpiration(token));

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/register")
    public ResponseEntity<BaseResponse<Object>> register(@RequestBody UserRequest req) {
        UserResponse created = userService.create(req);
        return ResponseEntity.ok(BaseResponse.ok(created));
    }
}