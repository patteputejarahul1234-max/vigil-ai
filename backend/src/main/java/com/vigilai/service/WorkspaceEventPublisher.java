package com.vigilai.service;

import com.vigilai.dto.WorkspaceEventDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class WorkspaceEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(WorkspaceEventPublisher.class);
    private final SimpMessagingTemplate messagingTemplate;

    public WorkspaceEventPublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void publish(Long workspaceId, String eventType, Object payload) {
        if (workspaceId == null) return;
        try {
            WorkspaceEventDto event = WorkspaceEventDto.of(eventType, workspaceId, payload);
            messagingTemplate.convertAndSend("/topic/workspace/" + workspaceId, event);
            log.debug("Published WebSocket event '{}' to /topic/workspace/{}", eventType, workspaceId);
        } catch (Exception ex) {
            log.warn("Failed to publish WebSocket event '{}': {}", eventType, ex.getMessage());
        }
    }
}
