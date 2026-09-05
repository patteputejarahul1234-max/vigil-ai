package com.vigilai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmartSuggestionResponse {
    private boolean hasEnoughData;
    private String message;
    private Integer suggestedHourOfDay; // 0-23, null if hasEnoughData is false
    private int completedCount;
}