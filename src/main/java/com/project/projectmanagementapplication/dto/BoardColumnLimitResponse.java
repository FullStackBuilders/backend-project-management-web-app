package com.project.projectmanagementapplication.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BoardColumnLimitResponse {
    private String status;
    private Integer wipLimit;
    private long currentCount;
    private boolean exceeded;
}
