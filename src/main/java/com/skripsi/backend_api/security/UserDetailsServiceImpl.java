package com.skripsi.backend_api.security;

import com.skripsi.backend_api.entity.User;
import com.skripsi.backend_api.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    // @Override
    // public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    //     User u = userRepository.findByUsername(username)
    //             .orElseThrow(() -> new UsernameNotFoundException("User not found"));

    //     if (Boolean.FALSE.equals(u.getIsActive())) {
    //         throw new DisabledException("User is inactive");
    //     }

    //     String roleName = (u.getRole() == null || u.getRole().getName() == null)
    //             ? "STAFF"
    //             : u.getRole().getName().toUpperCase();

    //     return new org.springframework.security.core.userdetails.User(
    //             u.getUsername(),
    //             u.getPassword(),
    //             List.of(new SimpleGrantedAuthority("ROLE_" + roleName))
    //     );
    // }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User u = userRepository.findWithRoleByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (Boolean.FALSE.equals(u.getIsActive())) {
            throw new DisabledException("User is inactive");
        }

        String roleName = (u.getRole() == null || u.getRole().getName() == null)
                ? "STAFF"
                : u.getRole().getName().toUpperCase();

        return new org.springframework.security.core.userdetails.User(
                u.getUsername(),
                u.getPassword(),
                List.of(new SimpleGrantedAuthority("ROLE_" + roleName))
        );
    }
}