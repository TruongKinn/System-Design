package com.dataflow.auth.controller;

import com.dataflow.auth.dto.CreateUserRequest;
import com.dataflow.auth.dto.UserProfileDto;
import com.dataflow.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UserController {

    private final AuthService authService;

    @GetMapping
    public ResponseEntity<List<UserProfileDto>> getAllUsers() {
        return ResponseEntity.ok(authService.getAllUsers());
    }

    @PostMapping
    public ResponseEntity<UserProfileDto> createUser(@RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.createUser(request));
    }

    @PutMapping("/{id}/toggle-status")
    public ResponseEntity<UserProfileDto> toggleUserStatus(@PathVariable Long id) {
        return ResponseEntity.ok(authService.toggleUserStatus(id));
    }
}
