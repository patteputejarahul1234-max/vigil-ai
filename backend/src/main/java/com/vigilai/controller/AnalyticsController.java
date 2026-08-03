package com.vigilai.controller;

import com.vigilai.dto.AnalyticsResponse;
import com.vigilai.security.CurrentUser;
import com.vigilai.service.AnalyticsService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Analytics", description = "Workspace-level dashboard stats")
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final CurrentUser currentUser;

    public AnalyticsController(AnalyticsService analyticsService, CurrentUser currentUser) {
        this.analyticsService = analyticsService;
        this.currentUser = currentUser;
    }

    @GetMapping("/api/workspaces/{workspaceId}/analytics")
    public AnalyticsResponse get(Authentication auth, @PathVariable Long workspaceId) {
        return analyticsService.getForWorkspace(workspaceId, currentUser.idOf(auth));
    }
}
