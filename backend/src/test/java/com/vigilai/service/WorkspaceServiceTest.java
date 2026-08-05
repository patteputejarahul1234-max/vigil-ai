package com.vigilai.service;

import com.vigilai.entity.WorkspaceMember;
import com.vigilai.entity.WorkspaceRole;
import com.vigilai.exception.ApiException;
import com.vigilai.repository.UserRepository;
import com.vigilai.repository.WorkspaceMemberRepository;
import com.vigilai.repository.WorkspaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * The access-control rules (assertMember / assertRole) are the part of
 * Stage 2 most worth pinning down with tests — every workspace-scoped
 * endpoint depends on this logic being right.
 */
@ExtendWith(MockitoExtension.class)
class WorkspaceServiceTest {

    @Mock private WorkspaceRepository workspaceRepository;
    @Mock private WorkspaceMemberRepository memberRepository;
    @Mock private UserRepository userRepository;
    @Mock private NotificationService notificationService;
    @Mock private ActivityLogService activityLogService;

    private WorkspaceService workspaceService;

    @BeforeEach
    void setUp() {
        workspaceService = new WorkspaceService(
                workspaceRepository, memberRepository, userRepository,
                notificationService, activityLogService
        );
    }

    @Test
    void assertMemberPassesForExistingMember() {
        when(memberRepository.existsByWorkspaceIdAndUserId(1L, 10L)).thenReturn(true);
        assertDoesNotThrow(() -> workspaceService.assertMember(1L, 10L));
    }

    @Test
    void assertMemberThrowsForNonMember() {
        when(memberRepository.existsByWorkspaceIdAndUserId(1L, 99L)).thenReturn(false);

        ApiException ex = assertThrows(ApiException.class, () -> workspaceService.assertMember(1L, 99L));
        assertTrue(ex.getMessage().toLowerCase().contains("not a member"));
    }

    @Test
    void assertRoleAllowsOwnerForAdminRequirement() {
        WorkspaceMember owner = WorkspaceMember.builder().workspaceId(1L).userId(5L).role(WorkspaceRole.OWNER).build();
        when(memberRepository.findByWorkspaceIdAndUserId(1L, 5L)).thenReturn(Optional.of(owner));

        assertDoesNotThrow(() -> workspaceService.assertRole(1L, 5L, WorkspaceRole.ADMIN));
    }

    @Test
    void assertRoleRejectsPlainMemberForAdminRequirement() {
        WorkspaceMember member = WorkspaceMember.builder().workspaceId(1L).userId(7L).role(WorkspaceRole.MEMBER).build();
        when(memberRepository.findByWorkspaceIdAndUserId(1L, 7L)).thenReturn(Optional.of(member));

        ApiException ex = assertThrows(ApiException.class,
                () -> workspaceService.assertRole(1L, 7L, WorkspaceRole.ADMIN));
        assertTrue(ex.getMessage().toLowerCase().contains("permission"));
    }

    @Test
    void assertRoleAllowsAdminForAdminRequirement() {
        WorkspaceMember admin = WorkspaceMember.builder().workspaceId(1L).userId(8L).role(WorkspaceRole.ADMIN).build();
        when(memberRepository.findByWorkspaceIdAndUserId(1L, 8L)).thenReturn(Optional.of(admin));

        assertDoesNotThrow(() -> workspaceService.assertRole(1L, 8L, WorkspaceRole.ADMIN));
    }
}
