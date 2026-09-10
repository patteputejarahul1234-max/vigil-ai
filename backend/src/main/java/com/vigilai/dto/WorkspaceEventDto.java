package com.vigilai.dto;

import java.time.Instant;

public record WorkspaceEventDto(
        String eventType,
        Long workspaceId,
        Object payload,
        Instant timestamp
) {
    public static WorkspaceEventDto of(String eventType, Long workspaceId, Object payload) {
        return new WorkspaceEventDto(eventType, workspaceId, payload, Instant.now());
    }
}
