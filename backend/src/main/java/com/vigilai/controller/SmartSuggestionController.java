package com.vigilai.controller;

import com.vigilai.dto.SmartSuggestionResponse;
import com.vigilai.security.CurrentUser;
import com.vigilai.service.SmartSuggestionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Smart Suggestions", description = "Data-driven reminder timing based on your own completion history (Stage 4)")
public class SmartSuggestionController {

    private final SmartSuggestionService smartSuggestionService;
    private final CurrentUser currentUser;

    public SmartSuggestionController(SmartSuggestionService smartSuggestionService, CurrentUser currentUser) {
        this.smartSuggestionService = smartSuggestionService;
        this.currentUser = currentUser;
    }

    @GetMapping("/api/tasks/{taskId}/suggestions")
    public SmartSuggestionResponse getSuggestion(Authentication auth, @PathVariable Long taskId) {
        return smartSuggestionService.getForTask(taskId, currentUser.idOf(auth));
    }
}