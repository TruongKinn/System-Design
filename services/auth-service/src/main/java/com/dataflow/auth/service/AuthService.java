package com.dataflow.auth.service;

import com.dataflow.auth.domain.Permission;
import com.dataflow.auth.domain.Role;
import com.dataflow.auth.domain.User;
import com.dataflow.auth.dto.*;
import com.dataflow.auth.repository.PermissionRepository;
import com.dataflow.auth.repository.RoleRepository;
import com.dataflow.auth.repository.UserRepository;
import com.dataflow.auth.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    @Transactional
    public JwtResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found: " + request.getUsername()));

        // Update last login timestamp
        user.setLastLoginAt(java.time.LocalDateTime.now());
        userRepository.save(user);

        String token = tokenProvider.generateToken(user);
        String refreshToken = tokenProvider.generateRefreshToken(user);

        Set<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        Set<String> permissions = user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(Permission::getCode)
                .collect(Collectors.toSet());

        return JwtResponse.builder()
                .token(token)
                .accessToken(token)
                .refreshToken(refreshToken)
                .type("Bearer")
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .roles(roles)
                .permissions(permissions)
                .build();
    }

    @Transactional
    public UserProfileDto register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already taken!");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already in use!");
        }

        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> roleRepository.save(Role.builder()
                        .name("ROLE_USER")
                        .description("Standard User Role")
                        .build()));

        Set<Role> roles = new HashSet<>();
        roles.add(userRole);

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .rawPassword(request.getPassword())
                .status("ACTIVE")
                .roles(roles)
                .build();

        User savedUser = userRepository.save(user);

        return getUserProfile(savedUser);
    }

    @Transactional(readOnly = true)
    public UserProfileDto getCurrentUserProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        return getUserProfile(user);
    }

    @Transactional
    public JwtResponse refreshToken(RefreshTokenRequest request) {
        String token = request.getRefreshToken();
        if (!tokenProvider.validateToken(token)) {
            throw new IllegalArgumentException("Invalid or expired Refresh Token");
        }

        String username = tokenProvider.getUsernameFromToken(token);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        String newAccessToken = tokenProvider.generateToken(user);
        String newRefreshToken = tokenProvider.generateRefreshToken(user);

        Set<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        Set<String> permissions = user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(Permission::getCode)
                .collect(Collectors.toSet());

        return JwtResponse.builder()
                .token(newAccessToken)
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .type("Bearer")
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .roles(roles)
                .permissions(permissions)
                .build();
    }

    @Transactional(readOnly = true)
    public List<UserProfileDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::getUserProfile)
                .collect(Collectors.toList());
    }

    @Transactional
    public UserProfileDto createUser(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already taken: " + request.getUsername());
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already in use: " + request.getEmail());
        }

        Set<Role> roles = new HashSet<>();
        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            for (String roleName : request.getRoles()) {
                Role role = roleRepository.findByName(roleName)
                        .orElseGet(() -> roleRepository.save(Role.builder()
                                .name(roleName)
                                .description("Role " + roleName)
                                .build()));
                roles.add(role);
            }
        } else {
            Role defaultRole = roleRepository.findByName("ROLE_VIEWER")
                    .orElseGet(() -> roleRepository.save(Role.builder()
                            .name("ROLE_VIEWER")
                            .description("Default Viewer Role")
                            .build()));
            roles.add(defaultRole);
        }

        String rawPass = request.getPassword() != null && !request.getPassword().isBlank() ? request.getPassword() : "Dataflow@123";
        User user = User.builder()
                .username(request.getUsername())
                .fullName(request.getFullName() != null && !request.getFullName().isBlank() ? request.getFullName() : request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(rawPass))
                .rawPassword(rawPass)
                .status("ACTIVE")
                .roles(roles)
                .build();

        User savedUser = userRepository.save(user);
        return getUserProfile(savedUser);
    }

    @Transactional
    public UserProfileDto toggleUserStatus(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));

        if ("ACTIVE".equalsIgnoreCase(user.getStatus())) {
            user.setStatus("LOCKED");
        } else {
            user.setStatus("ACTIVE");
        }

        User updatedUser = userRepository.save(user);
        return getUserProfile(updatedUser);
    }

    @Transactional(readOnly = true)
    public List<RolePermissionDto> getAllRoles() {
        return roleRepository.findAll().stream()
                .map(role -> {
                    List<String> perms = role.getPermissions().stream()
                            .map(Permission::getCode)
                            .sorted()
                            .collect(Collectors.toList());
                    int count = (int) userRepository.findAll().stream()
                            .filter(u -> u.getRoles().stream().anyMatch(r -> r.getId().equals(role.getId())))
                            .count();
                    return RolePermissionDto.builder()
                            .id(role.getId())
                            .roleName(role.getName())
                            .description(role.getDescription())
                            .permissions(perms)
                            .userCount(count)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public RolePermissionDto updateRolePermissions(Long roleId, List<String> permissionCodes) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role not found with id: " + roleId));

        Set<Permission> updatedPermissions = new HashSet<>();
        if (permissionCodes != null) {
            for (String code : permissionCodes) {
                if (code == null || code.isBlank()) continue;
                String permCode = code.trim();
                Permission perm = permissionRepository.findByCode(permCode)
                        .orElseGet(() -> permissionRepository.save(Permission.builder()
                                .code(permCode)
                                .description("Permission " + permCode)
                                .build()));
                updatedPermissions.add(perm);
            }
        }

        role.setPermissions(updatedPermissions);
        Role savedRole = roleRepository.save(role);

        List<String> perms = savedRole.getPermissions().stream()
                .map(Permission::getCode)
                .sorted()
                .collect(Collectors.toList());
        int count = (int) userRepository.findAll().stream()
                .filter(u -> u.getRoles().stream().anyMatch(r -> r.getId().equals(savedRole.getId())))
                .count();

        return RolePermissionDto.builder()
                .id(savedRole.getId())
                .roleName(savedRole.getName())
                .description(savedRole.getDescription())
                .permissions(perms)
                .userCount(count)
                .build();
    }

    @Transactional
    public void changePassword(String username, ChangePasswordRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        if (request.getOldPassword() == null || !passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Mật khẩu hiện tại không chính xác!");
        }

        if (request.getNewPassword() == null || request.getNewPassword().length() < 6) {
            throw new IllegalArgumentException("Mật khẩu mới phải có ít nhất 6 ký tự!");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setRawPassword(request.getNewPassword());
        userRepository.save(user);
    }

    @Transactional
    public void adminResetPassword(Long userId, AdminResetPasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        if (request.getNewPassword() == null || request.getNewPassword().length() < 6) {
            throw new IllegalArgumentException("Mật khẩu mới phải có ít nhất 6 ký tự!");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setRawPassword(request.getNewPassword());
        userRepository.save(user);
    }

    private UserProfileDto getUserProfile(User user) {
        Set<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        Set<String> permissions = user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(Permission::getCode)
                .collect(Collectors.toSet());

        String displayPassword = user.getRawPassword() != null && !user.getRawPassword().isBlank() 
                ? user.getRawPassword() 
                : user.getPassword();

        return UserProfileDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .status(user.getStatus())
                .roles(roles)
                .permissions(permissions)
                .password(displayPassword)
                .lastLoginAt(user.getLastLoginAt() != null ? user.getLastLoginAt().toString() : null)
                .createdAt(user.getCreatedAt() != null ? user.getCreatedAt().toString() : null)
                .updatedAt(user.getUpdatedAt() != null ? user.getUpdatedAt().toString() : null)
                .build();
    }
}

