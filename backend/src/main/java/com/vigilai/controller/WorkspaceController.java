package com.vigilai.controller;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vigilai.dto.InviteMemberRequest;
import com.vigilai.dto.WorkspaceMemberResponse;
import com.vigilai.dto.WorkspaceRequest;
import com.vigilai.dto.WorkspaceResponse;
import com.vigilai.entity.User;
import com.vigilai.exception.ApiException;
import com.vigilai.repository.UserRepository;
import com.vigilai.security.CurrentUser;
import com.vigilai.service.WorkspaceService;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

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
        if (auth == null || !auth.isAuthenticated()) {
            throw ApiException.unauthorized("User is not authenticated");
        }

        // Try CurrentUser helper
        try {
            Long id = currentUser.idOf(auth);
            if (id != null) return id;
        } catch (Exception ignored) {}

        // Try OAuth2 Principal
        if (auth.getPrincipal() instanceof OAuth2User oauth2User) {
            String email = oauth2User.getAttribute("email");
            if (email != null) {
                return userRepository.findByEmail(email)
                        .map(User::getId)
                        .orElseThrow(() -> ApiException.unauthorized("User account not found for email: " + email));
            }
        }

        // Try JWT Principal
        if (auth.getPrincipal() instanceof Jwt jwt) {
            String email = jwt.getClaimAsString("email");
            if (email != null) {
                return userRepository.findByEmail(email)
                        .map(User::getId)
                        .orElseThrow(() -> ApiException.unauthorized("User account not found for email: " + email));
            }
        }

        // Try Principal Name (Email fallback)
        if (auth.getName() != null && !auth.getName().isBlank()) {
            return userRepository.findByEmail(auth.getName())
                    .map(User::getId)
                    .orElseThrow(() -> ApiException.unauthorized("User account not found for principal: " + auth.getName()));
        }

        throw ApiException.unauthorized("Unable to determine authenticated user ID");
    }
}