package com.vigilai.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class InviteMemberRequest {
    @NotBlank
    @Email
    private String email;

    private String role; // "ADMIN" or "MEMBER", defaults to MEMBER
}
