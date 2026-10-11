package com.dataflow.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RolePermissionDto {
    private Long id;
    private String roleName;
    private String description;
    private List<String> permissions;
    private int userCount;
}
