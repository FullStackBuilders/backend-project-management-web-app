package com.project.projectmanagementapplication.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThroughputByDayPoint {
    private LocalDate date;
    private int completedTasks;
}
