package com.vigilai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AssistantQueryRequest {
    @NotBlank
    private String question;
}