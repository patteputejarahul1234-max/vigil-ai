package com.vigilai.dto;

import com.vigilai.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {
    private Long id;
    private String fullName;
    private String email;
    private Role role;
    private boolean emailVerified;
    private String avatarUrl;
    private String bio;
    private String timezone;
}
