package com.vigilai.controller;

import com.vigilai.dto.*;
import com.vigilai.entity.User;
import com.vigilai.exception.ApiException;
import com.vigilai.repository.UserRepository;
import com.vigilai.security.CurrentUser;
import com.vigilai.service.WorkspaceService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/workspaces")
@Tag(name = "Workspaces", description = "Team workspace creation and membership")
public class WorkspaceController {

    private final WorkspaceService workspaceService;
    private final CurrentUser currentUser;
    private final UserRepository userRepository;

    public WorkspaceController(WorkspaceService workspaceService, CurrentUser currentUser, UserRepository userRepository) {
        this.workspaceService = workspaceService;
        this.currentUser = currentUser;
        this.userRepository = userRepository;
    }

    @PostMapping
    public WorkspaceResponse create(Authentication auth, @Valid @RequestBody WorkspaceRequest request) {
        Long userId = resolveUserId(auth);
        return workspaceService.create(userId, request);
    }

    @GetMapping
    public List<WorkspaceResponse> myWorkspaces(Authentication auth) {
        Long userId = resolveUserId(auth);
        return workspaceService.getForUser(userId);
    }

    @GetMapping("/{workspaceId}")
    public WorkspaceResponse getOne(Authentication auth, @PathVariable Long workspaceId) {
        Long userId = resolveUserId(auth);
        return workspaceService.getById(workspaceId, userId);
    }

    @PostMapping("/{workspaceId}/members")
    public WorkspaceMemberResponse invite(Authentication auth, @PathVariable Long workspaceId,
                                           @Valid @RequestBody InviteMemberRequest request) {
        Long userId = resolveUserId(auth);
        return workspaceService.invite(workspaceId, userId, request);
    }

    @GetMapping("/{workspaceId}/members")
    public List<WorkspaceMemberResponse> members(Authentication auth, @PathVariable Long workspaceId) {
        Long userId = resolveUserId(auth);
        return workspaceService.getMembers(workspaceId, userId);
    }

    private Long resolveUserId(Authentication auth) {
        Long id = currentUser.idOf(auth);
        if (id != null) {
            return id;
        }

        if (auth != null && auth.getPrincipal() instanceof OAuth2User oauth2User) {
            String email = oauth2User.getAttribute("email");
            if (email != null) {
                User user = userRepository.findByEmail(email)
                        .orElseThrow(() -> ApiException.notFound("User account not found for email: " + email));
                return user.getId();
            }
        }

        if (auth != null && auth.getName() != null) {
            User user = userRepository.findByEmail(auth.getName())
                    .orElseThrow(() -> ApiException.notFound("User account not found for principal: " + auth.getName()));
            return user.getId();
        }

        throw ApiException.unauthorized("Unable to extract user authentication context");
    }
}