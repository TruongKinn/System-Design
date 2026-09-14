package com.dataflow.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JwtResponse {
    private String token;
    private String accessToken;
    private String refreshToken;
    @Builder.Default
    private String type = "Bearer";

    public String getAccessToken() {
        return accessToken != null ? accessToken : token;
    }
    private Long id;
    private String username;
    private String email;
    private Set<String> roles;
    private Set<String> permissions;
}
