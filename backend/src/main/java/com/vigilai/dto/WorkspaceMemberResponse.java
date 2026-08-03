package com.vigilai.dto;

import com.vigilai.entity.WorkspaceRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceMemberResponse {
    private Long userId;
    private String fullName;
    private String email;
    private String avatarUrl;
    private WorkspaceRole role;
}
