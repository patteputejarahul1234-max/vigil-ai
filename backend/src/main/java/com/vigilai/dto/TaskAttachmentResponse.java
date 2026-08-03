package com.vigilai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskAttachmentResponse {
    private Long id;
    private Long taskId;
    private String fileName;
    private String downloadUrl;
    private String contentType;
    private long sizeBytes;
    private Long uploadedBy;
    private Instant uploadedAt;
}
