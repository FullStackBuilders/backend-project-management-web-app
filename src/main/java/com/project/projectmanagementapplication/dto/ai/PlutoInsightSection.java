package com.project.projectmanagementapplication.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlutoInsightSection {
    private String key;
    private String title;
    private String content;
}
