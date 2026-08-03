package com.vigilai.controller;

import com.vigilai.dto.UpdateProfileRequest;
import com.vigilai.dto.UserProfileResponse;
import com.vigilai.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "Profile management for the authenticated user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public UserProfileResponse getCurrentUser(Authentication authentication) {
        return userService.getProfile(authentication.getName());
    }

    @PutMapping("/me")
    public UserProfileResponse updateCurrentUser(Authentication authentication,
                                                  @RequestBody UpdateProfileRequest request) {
        return userService.updateProfile(authentication.getName(), request);
    }
}
