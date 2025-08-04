package com.project.projectmanagementapplication.dto;


import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CommentResponse {
    private Long id;
    private String content;
    private LocalDateTime createdDateTime;
    private Long userId;
    private String userName;
    private Long issueId;
}
