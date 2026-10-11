package com.dataflow.auth.controller;

import com.dataflow.auth.dto.RolePermissionDto;
import com.dataflow.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/api/roles", "/api/v1/roles"})
@RequiredArgsConstructor
public class RoleController {

    private final AuthService authService;

    @GetMapping
    public ResponseEntity<List<RolePermissionDto>> getAllRoles() {
        return ResponseEntity.ok(authService.getAllRoles());
    }

    @PutMapping("/{id}/permissions")
    public ResponseEntity<RolePermissionDto> updateRolePermissions(
            @PathVariable Long id,
            @RequestBody List<String> permissions) {
        return ResponseEntity.ok(authService.updateRolePermissions(id, permissions));
    }
}
