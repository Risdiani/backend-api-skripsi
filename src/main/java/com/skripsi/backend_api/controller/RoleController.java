package com.skripsi.backend_api.controller;

import com.skripsi.backend_api.dto.BaseResponse;
import com.skripsi.backend_api.dto.role.request.RoleRequest;
import com.skripsi.backend_api.dto.role.response.RoleResponse;
import com.skripsi.backend_api.service.role.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    public ResponseEntity<BaseResponse<Object>> list() {
        List<RoleResponse> data = roleService.findAll();
        return ResponseEntity.ok(BaseResponse.ok(data));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BaseResponse<Object>> detail(@PathVariable Long id) {
        RoleResponse data = roleService.findById(id);
        return ResponseEntity.ok(BaseResponse.ok(data));
    }

    @PostMapping
    public ResponseEntity<BaseResponse<Object>> create(@RequestBody RoleRequest req) {
        RoleResponse data = roleService.create(req);
        return ResponseEntity.ok(BaseResponse.ok("Role created", data));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BaseResponse<Object>> update(@PathVariable Long id, @RequestBody RoleRequest req) {
        RoleResponse data = roleService.update(id, req);
        return ResponseEntity.ok(BaseResponse.ok("Role updated", data));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<BaseResponse<Object>> delete(@PathVariable Long id) {
        roleService.delete(id);
        return ResponseEntity.ok(BaseResponse.create(200, true, "Role deleted", null));
    }
}