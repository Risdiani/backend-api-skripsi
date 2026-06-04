package com.skripsi.backend_api.service.role;

import com.skripsi.backend_api.dto.role.request.RoleRequest;
import com.skripsi.backend_api.dto.role.response.RoleResponse;
import com.skripsi.backend_api.entity.Role;
import com.skripsi.backend_api.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;

    private static String normalizeName(String name) {
        if (name == null) return null;
        return name.trim().toUpperCase();
    }

    private RoleResponse toResponse(Role r) {
        return RoleResponse.builder()
                .id(r.getId())
                .name(r.getName())
                .createdAt(r.getCreatedAt())
                .build();
    }

    public List<RoleResponse> findAll() {
        log.info("RoleService.findAll - fetching all roles");
        List<RoleResponse> result = roleRepository.findAll().stream().map(this::toResponse).toList();
        log.info("RoleService.findAll - total={}", result.size());
        return result;
    }

    public RoleResponse findById(Long id) {
        log.info("RoleService.findById - id={}", id);
        Role r = roleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Role not found"));
        return toResponse(r);
    }

    public RoleResponse create(RoleRequest req) {
        String name = normalizeName(req == null ? null : req.getName());
        log.info("RoleService.create - name={}", name);

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Role name is required");
        }
        if (name.length() > 20) {
            throw new IllegalArgumentException("Role name max length is 20");
        }

        if (roleRepository.findByName(name).isPresent()) {
            throw new IllegalArgumentException("Role name already used");
        }

        Role role = Role.builder()
                .name(name)
                .build();

        Role saved = roleRepository.save(role);
        log.info("RoleService.create - created id={}, name={}", saved.getId(), saved.getName());
        return toResponse(saved);
    }

    public RoleResponse update(Long id, RoleRequest req) {
        String name = normalizeName(req == null ? null : req.getName());
        log.info("RoleService.update - id={}, name={}", id, name);

        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Role not found"));

        if (name != null) {
            if (name.isBlank()) {
                throw new IllegalArgumentException("Role name cannot be blank");
            }
            if (name.length() > 20) {
                throw new IllegalArgumentException("Role name max length is 20");
            }

            roleRepository.findByName(name).ifPresent(existing -> {
                if (!existing.getId().equals(role.getId())) {
                    throw new IllegalArgumentException("Role name already used");
                }
            });

            role.setName(name);
        }

        Role saved = roleRepository.save(role);
        log.info("RoleService.update - updated id={}, name={}", saved.getId(), saved.getName());
        return toResponse(saved);
    }

    public void delete(Long id) {
        log.info("RoleService.delete - id={}", id);

        if (!roleRepository.existsById(id)) {
            throw new IllegalArgumentException("Role not found");
        }

        roleRepository.deleteById(id);
        log.info("RoleService.delete - deleted id={}", id);
    }
}