package com.vigilai.controller;

import com.vigilai.dto.ActivityLogResponse;
import com.vigilai.security.CurrentUser;
import com.vigilai.service.ActivityLogService;
import com.vigilai.service.WorkspaceService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Tag(name = "Activity Logs", description = "Audit trail of actions within a workspace")
public class ActivityLogController {

    private final ActivityLogService activityLogService;
    private final WorkspaceService workspaceService;
    private final CurrentUser currentUser;

    public ActivityLogController(ActivityLogService activityLogService, WorkspaceService workspaceService, CurrentUser currentUser) {
        this.activityLogService = activityLogService;
        this.workspaceService = workspaceService;
        this.currentUser = currentUser;
    }

    @GetMapping("/api/workspaces/{workspaceId}/activity")
    public List<ActivityLogResponse> list(Authentication auth, @PathVariable Long workspaceId) {
        workspaceService.assertMember(workspaceId, currentUser.idOf(auth));
        return activityLogService.getForWorkspace(workspaceId);
    }
}
