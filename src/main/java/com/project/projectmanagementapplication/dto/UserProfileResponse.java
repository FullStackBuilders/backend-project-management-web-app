package com.project.projectmanagementapplication.dto;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class UserProfileResponse {
    private String firstName;
    private String lastName;
    private String email;
}
