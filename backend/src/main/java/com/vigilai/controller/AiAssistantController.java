package com.vigilai.controller;

import com.vigilai.dto.AssistantQueryRequest;
import com.vigilai.dto.AssistantQueryResponse;
import com.vigilai.security.CurrentUser;
import com.vigilai.service.AiAssistantService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "AI Assistant", description = "Ask questions about your real tasks in a workspace (Stage 4)")
public class AiAssistantController {

    private final AiAssistantService aiAssistantService;
    private final CurrentUser currentUser;

    public AiAssistantController(AiAssistantService aiAssistantService, CurrentUser currentUser) {
        this.aiAssistantService = aiAssistantService;
        this.currentUser = currentUser;
    }

    @PostMapping("/api/workspaces/{workspaceId}/assistant/ask")
    public AssistantQueryResponse ask(Authentication auth, @PathVariable Long workspaceId,
    @Valid @RequestBody AssistantQueryRequest request) {
        return aiAssistantService.ask(workspaceId, currentUser.idOf(auth), request.getQuestion());
    }
}