package com.project.projectmanagementapplication.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectMemberWithRoleResponse {
    private Long userId;
    private String firstName;
    private String lastName;
    private String email;
    private String role;
}
