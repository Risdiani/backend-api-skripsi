package com.skripsi.backend_api.controller.user;

import com.skripsi.backend_api.dto.user.request.LoginRequest;
import com.skripsi.backend_api.dto.user.response.LoginResponse;
import com.skripsi.backend_api.entity.User;
import com.skripsi.backend_api.repository.user.UserRepository;
import com.skripsi.backend_api.security.JwtService;
import com.skripsi.backend_api.security.TokenBlacklistService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final TokenBlacklistService tokenBlacklistService;

    public AuthController(AuthenticationManager authenticationManager,
                          JwtService jwtService,
                          UserRepository userRepository,
                          TokenBlacklistService tokenBlacklistService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest req) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword())
        );

        String token = jwtService.generateToken(auth.getName());
        User u = userRepository.findByUsername(auth.getName()).orElseThrow();

        return ResponseEntity.ok(LoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(u.getId())
                .username(u.getUsername())
                .role(u.getRole() == null ? null : u.getRole().name())
                .build());
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
}