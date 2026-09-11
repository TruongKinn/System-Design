package com.dataflow.auth;

import com.dataflow.auth.dto.JwtResponse;
import com.dataflow.auth.dto.LoginRequest;
import com.dataflow.auth.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AuthServiceApplicationTests {

    @Autowired
    private AuthService authService;

    @Test
    void contextLoads() {
        assertNotNull(authService);
    }

    @Test
    void testAdminLoginAndJwtGeneration() {
        LoginRequest loginRequest = new LoginRequest("admin", "admin123");
        JwtResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertNotNull(response.getToken());
        assertNotNull(response.getRefreshToken());
        assertEquals("admin", response.getUsername());
        assertTrue(response.getRoles().contains("ROLE_ADMIN"));
        assertTrue(response.getPermissions().contains("JOB_READ"));
        assertTrue(response.getPermissions().contains("EXPORT_DATA"));
    }
}
