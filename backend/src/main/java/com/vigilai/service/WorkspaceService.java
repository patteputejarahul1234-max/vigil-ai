package com.vigilai.service;

import com.vigilai.dto.*;
import com.vigilai.entity.*;
import com.vigilai.exception.ApiException;
import com.vigilai.repository.UserRepository;
import com.vigilai.repository.WorkspaceMemberRepository;
import com.vigilai.repository.WorkspaceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final ActivityLogService activityLogService;

    public WorkspaceService(
            WorkspaceRepository workspaceRepository,
            WorkspaceMemberRepository memberRepository,
            UserRepository userRepository,
            NotificationService notificationService,
            ActivityLogService activityLogService
    ) {
        this.workspaceRepository = workspaceRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.activityLogService = activityLogService;
    }

    @Transactional
    public WorkspaceResponse create(Long ownerId, WorkspaceRequest request) {
        Workspace workspace = Workspace.builder()
                .name(request.getName())
                .description(request.getDescription())
                .ownerId(ownerId)
                .build();
        workspace = workspaceRepository.save(workspace);

        memberRepository.save(WorkspaceMember.builder()
                .workspaceId(workspace.getId())
                .userId(ownerId)
                .role(WorkspaceRole.OWNER)
                .build());

        activityLogService.log(workspace.getId(), ownerId, "WORKSPACE_CREATED", "WORKSPACE", workspace.getId(),
                "Created workspace \"" + workspace.getName() + "\"");

        return toResponse(workspace);
    }

    public List<WorkspaceResponse> getForUser(Long userId) {
        return memberRepository.findByUserId(userId).stream()
                .map(m -> workspaceRepository.findById(m.getWorkspaceId()).orElse(null))
                .filter(w -> w != null)
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public WorkspaceResponse getById(Long workspaceId, Long requesterId) {
        assertMember(workspaceId, requesterId);
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> ApiException.notFound("Workspace not found"));
        return toResponse(workspace);
    }

    @Transactional
    public WorkspaceMemberResponse invite(Long workspaceId, Long inviterId, InviteMemberRequest request) {
        assertRole(workspaceId, inviterId, WorkspaceRole.ADMIN);

        User invitee = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> ApiException.notFound("No Vigil AI account found for this email"));

        if (memberRepository.existsByWorkspaceIdAndUserId(workspaceId, invitee.getId())) {
            throw ApiException.conflict("This user is already a member of the workspace");
        }

        WorkspaceRole role = "ADMIN".equalsIgnoreCase(request.getRole()) ? WorkspaceRole.ADMIN : WorkspaceRole.MEMBER;

        WorkspaceMember member = memberRepository.save(WorkspaceMember.builder()
                .workspaceId(workspaceId)
                .userId(invitee.getId())
                .role(role)
                .build());

        Workspace workspace = workspaceRepository.findById(workspaceId).orElseThrow();
        notificationService.notify(invitee.getId(), NotificationType.WORKSPACE_INVITE,
                "You were added to workspace \"" + workspace.getName() + "\"", "WORKSPACE", workspaceId);

        activityLogService.log(workspaceId, inviterId, "MEMBER_ADDED", "WORKSPACE", workspaceId,
                invitee.getEmail() + " joined as " + role);

        return WorkspaceMemberResponse.builder()
                .userId(invitee.getId())
                .fullName(invitee.getFullName())
                .email(invitee.getEmail())
                .avatarUrl(invitee.getAvatarUrl())
                .role(member.getRole())
                .build();
    }

    public List<WorkspaceMemberResponse> getMembers(Long workspaceId, Long requesterId) {
        assertMember(workspaceId, requesterId);

        return memberRepository.findByWorkspaceId(workspaceId).stream()
                .map(m -> {
                    User u = userRepository.findById(m.getUserId()).orElse(null);
                    return WorkspaceMemberResponse.builder()
                            .userId(m.getUserId())
                            .fullName(u != null ? u.getFullName() : "Unknown")
                            .email(u != null ? u.getEmail() : "")
                            .avatarUrl(u != null ? u.getAvatarUrl() : null)
                            .role(m.getRole())
                            .build();
                })
                .collect(Collectors.toList());
    }

    // --- access control helpers, reused by Project/Task services ---

    public void assertMember(Long workspaceId, Long userId) {
        if (!memberRepository.existsByWorkspaceIdAndUserId(workspaceId, userId)) {
            throw ApiException.badRequest("You are not a member of this workspace");
        }
    }

    public void assertRole(Long workspaceId, Long userId, WorkspaceRole minimumRole) {
        WorkspaceMember member = memberRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseThrow(() -> ApiException.badRequest("You are not a member of this workspace"));

        boolean sufficient = switch (minimumRole) {
            case MEMBER -> true; // any member qualifies
            case ADMIN -> member.getRole() == WorkspaceRole.ADMIN || member.getRole() == WorkspaceRole.OWNER;
            case OWNER -> member.getRole() == WorkspaceRole.OWNER;
        };

        if (!sufficient) {
            throw ApiException.badRequest("You don't have permission to do this in this workspace");
        }
    }

    private WorkspaceResponse toResponse(Workspace workspace) {
        int memberCount = memberRepository.findByWorkspaceId(workspace.getId()).size();
        return WorkspaceResponse.builder()
                .id(workspace.getId())
                .name(workspace.getName())
                .description(workspace.getDescription())
                .ownerId(workspace.getOwnerId())
                .memberCount(memberCount)
                .createdAt(workspace.getCreatedAt())
                .build();
    }
}
