package com.vigilai.controller;

import com.vigilai.dto.*;
import com.vigilai.security.CurrentUser;
import com.vigilai.service.WorkspaceService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/workspaces")
@Tag(name = "Workspaces", description = "Team workspace creation and membership")
public class WorkspaceController {

    private final WorkspaceService workspaceService;
    private final CurrentUser currentUser;

    public WorkspaceController(WorkspaceService workspaceService, CurrentUser currentUser) {
        this.workspaceService = workspaceService;
        this.currentUser = currentUser;
    }

    @PostMapping
    public WorkspaceResponse create(Authentication auth, @Valid @RequestBody WorkspaceRequest request) {
        return workspaceService.create(currentUser.idOf(auth), request);
    }

    @GetMapping
    public List<WorkspaceResponse> myWorkspaces(Authentication auth) {
        return workspaceService.getForUser(currentUser.idOf(auth));
    }

    @GetMapping("/{workspaceId}")
    public WorkspaceResponse getOne(Authentication auth, @PathVariable Long workspaceId) {
        return workspaceService.getById(workspaceId, currentUser.idOf(auth));
    }

    @PostMapping("/{workspaceId}/members")
    public WorkspaceMemberResponse invite(Authentication auth, @PathVariable Long workspaceId,
                                           @Valid @RequestBody InviteMemberRequest request) {
        return workspaceService.invite(workspaceId, currentUser.idOf(auth), request);
    }

    @GetMapping("/{workspaceId}/members")
    public List<WorkspaceMemberResponse> members(Authentication auth, @PathVariable Long workspaceId) {
        return workspaceService.getMembers(workspaceId, currentUser.idOf(auth));
    }
}
