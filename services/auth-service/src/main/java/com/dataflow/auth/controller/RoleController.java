package com.dataflow.auth.controller;

import com.dataflow.auth.dto.RolePermissionDto;
import com.dataflow.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/api/roles", "/api/v1/roles"})
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class RoleController {

    private final AuthService authService;

    @GetMapping
    public ResponseEntity<List<RolePermissionDto>> getAllRoles() {
        return ResponseEntity.ok(authService.getAllRoles());
    }
}
