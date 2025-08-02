package com.project.projectmanagementapplication.dto;

import lombok.Data;

@Data
public class EmailInviteRequest {

    private Long projectId;
    private String email;

}
