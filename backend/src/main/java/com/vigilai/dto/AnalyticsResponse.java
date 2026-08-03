package com.vigilai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsResponse {
    private long totalProjects;
    private long totalTasks;
    private long completedTasks;
    private double completionRate;
    private Map<String, Long> tasksByStatus;
    private Map<String, Long> tasksByPriority;
    private long overdueTasks;
}
