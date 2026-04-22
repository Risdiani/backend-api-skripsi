package com.skripsi.backend_api.controller.user;

import com.skripsi.backend_api.utils.BaseResponse;
import com.skripsi.backend_api.dto.user.request.LoginRequest;
import com.skripsi.backend_api.dto.user.response.LoginResponse;
import com.skripsi.backend_api.entity.User;
import com.skripsi.backend_api.repository.user.UserRepository;
import com.skripsi.backend_api.security.JwtService;
import com.skripsi.backend_api.security.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final TokenBlacklistService tokenBlacklistService;

    @PostMapping("/login")
    public ResponseEntity<BaseResponse<Object>> login(@RequestBody LoginRequest req) {
        log.info("Login attempt untuk username: {}", req.getUsername());
        
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword())
            );

            String token = jwtService.generateToken(auth.getName());
            User u = userRepository.findByUsername(auth.getName()).orElseThrow();

            log.info("Login berhasil untuk user: {} (ID: {})", u.getUsername(), u.getId());

            LoginResponse loginResponse = LoginResponse.builder()
                    .token(token)
                    .tokenType("Bearer")
                    .userId(u.getId())
                    .username(u.getUsername())
                    .role(u.getRole() == null ? null : u.getRole().name())
                    .build();

            return ResponseEntity.ok(BaseResponse.ok("Login berhasil", loginResponse));
        } catch (BadCredentialsException e) {
            log.warn("Login gagal: kredensial tidak valid untuk username: {}", req.getUsername());
            throw e;
        } catch (Exception e) {
            log.error("Error saat login untuk username: {}", req.getUsername(), e);
            throw e;
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<BaseResponse<String>> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        log.info("Logout attempt");
        
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            log.warn("Logout gagal: token tidak valid atau tidak ada");
            return ResponseEntity.ok(BaseResponse.info("Logout berhasil"));
        }

        try {
            String token = authorization.substring(7);
            String jti = jwtService.extractJti(token);
            tokenBlacklistService.revoke(jti, jwtService.extractExpiration(token));

            log.info("Logout berhasil, token JTI: {} telah di-blacklist", jti);
            return ResponseEntity.ok(BaseResponse.info("Logout berhasil"));
        } catch (Exception e) {
            log.error("Error saat logout", e);
            throw e;
        }
    }
}