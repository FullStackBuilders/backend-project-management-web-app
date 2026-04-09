package com.project.projectmanagementapplication.dto;

import com.project.projectmanagementapplication.enums.PROJECT_MEMBER_ROLE;
import lombok.Data;

@Data
public class UpdateProjectMemberRoleRequest {
    private PROJECT_MEMBER_ROLE role;
}
