package com.dataflow.auth.config;

import com.dataflow.auth.domain.Permission;
import com.dataflow.auth.domain.Role;
import com.dataflow.auth.domain.User;
import com.dataflow.auth.repository.PermissionRepository;
import com.dataflow.auth.repository.RoleRepository;
import com.dataflow.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // 1. Create Permissions
        Permission permJobRead = createPermissionIfNotFound("JOB_READ", "Permission to read jobs");
        Permission permJobExecute = createPermissionIfNotFound("JOB_EXECUTE", "Permission to execute jobs");
        Permission permJobManage = createPermissionIfNotFound("JOB_MANAGE", "Permission to create/edit/delete jobs");
        Permission permExport = createPermissionIfNotFound("EXPORT_DATA", "Permission to export data");
        Permission permImport = createPermissionIfNotFound("IMPORT_DATA", "Permission to import data");

        // 2. Create Admin Role & Permissions
        Set<Permission> adminPermissions = new HashSet<>();
        adminPermissions.add(permJobRead);
        adminPermissions.add(permJobExecute);
        adminPermissions.add(permJobManage);
        adminPermissions.add(permExport);
        adminPermissions.add(permImport);

        Role adminRole = createRoleIfNotFound("ROLE_ADMIN", "Administrator Role", adminPermissions);

        // 3. Create Operator Role & Permissions
        Set<Permission> operatorPermissions = new HashSet<>();
        operatorPermissions.add(permJobRead);
        operatorPermissions.add(permJobExecute);

        Role operatorRole = createRoleIfNotFound("ROLE_OPERATOR", "Operator Role", operatorPermissions);

        // 4. Create or Update Initial Admin User
        Set<Role> adminRoles = new HashSet<>();
        adminRoles.add(adminRole);

        userRepository.findByUsername("admin").ifPresentOrElse(existingAdmin -> {
            existingAdmin.setPassword(passwordEncoder.encode("Admin@123"));
            if (existingAdmin.getFullName() == null) {
                existingAdmin.setFullName("Quản trị viên Hệ thống");
            }
            userRepository.save(existingAdmin);
        }, () -> {
            User adminUser = User.builder()
                    .username("admin")
                    .fullName("Quản trị viên Hệ thống")
                    .password(passwordEncoder.encode("Admin@123"))
                    .email("admin@dataflow.com")
                    .status("ACTIVE")
                    .roles(adminRoles)
                    .build();
            userRepository.save(adminUser);
        });

        // 5. Create Initial Operator User
        if (!userRepository.existsByUsername("operator")) {
            Set<Role> operRoles = new HashSet<>();
            operRoles.add(operatorRole);
            User operUser = User.builder()
                    .username("operator")
                    .fullName("Trần Văn Vận Hành")
                    .password(passwordEncoder.encode("Oper@123"))
                    .email("operator@dataflow.com")
                    .status("ACTIVE")
                    .roles(operRoles)
                    .build();
            userRepository.save(operUser);
        }
    }

    private Permission createPermissionIfNotFound(String code, String description) {
        return permissionRepository.findByCode(code)
                .orElseGet(() -> permissionRepository.save(
                        Permission.builder().code(code).description(description).build()
                ));
    }

    private Role createRoleIfNotFound(String name, String description, Set<Permission> permissions) {
        return roleRepository.findByName(name)
                .orElseGet(() -> roleRepository.save(
                        Role.builder().name(name).description(description).permissions(permissions).build()
                ));
    }
}
