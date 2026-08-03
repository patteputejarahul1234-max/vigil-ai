package com.vigilai.dto;

import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String fullName;
    private String bio;
    private String timezone;
    private String avatarUrl;
}
