package com.vigilai.controller;

import com.vigilai.dto.ProjectRequest;
import com.vigilai.dto.ProjectResponse;
import com.vigilai.security.CurrentUser;
import com.vigilai.service.ProjectService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Tag(name = "Projects", description = "Projects belong to a workspace")
public class ProjectController {

    private final ProjectService projectService;
    private final CurrentUser currentUser;

    public ProjectController(ProjectService projectService, CurrentUser currentUser) {
        this.projectService = projectService;
        this.currentUser = currentUser;
    }

    @PostMapping("/api/workspaces/{workspaceId}/projects")
    public ProjectResponse create(Authentication auth, @PathVariable Long workspaceId,
                                   @Valid @RequestBody ProjectRequest request) {
        return projectService.create(workspaceId, currentUser.idOf(auth), request);
    }

    @GetMapping("/api/workspaces/{workspaceId}/projects")
    public List<ProjectResponse> list(Authentication auth, @PathVariable Long workspaceId) {
        return projectService.getForWorkspace(workspaceId, currentUser.idOf(auth));
    }

    @GetMapping("/api/projects/{projectId}")
    public ProjectResponse getOne(Authentication auth, @PathVariable Long projectId) {
        return projectService.getById(projectId, currentUser.idOf(auth));
    }

    @PutMapping("/api/projects/{projectId}")
    public ProjectResponse update(Authentication auth, @PathVariable Long projectId,
                                   @RequestBody ProjectRequest request) {
        return projectService.update(projectId, currentUser.idOf(auth), request);
    }

    @DeleteMapping("/api/projects/{projectId}")
    public void delete(Authentication auth, @PathVariable Long projectId) {
        projectService.delete(projectId, currentUser.idOf(auth));
    }
}
