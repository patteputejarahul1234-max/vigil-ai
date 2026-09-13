package com.vigilai.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

import com.vigilai.entity.User;
import com.vigilai.exception.ApiException;
import com.vigilai.repository.UserRepository;

/**
 * Resolves the numeric user ID from the Spring Security Authentication context,
 * supporting both direct JWT email logins and OAuth2 authentication.
 */
@Component
public class CurrentUser {

    private final UserRepository userRepository;

    public CurrentUser(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Long idOf(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw ApiException.unauthorized("User is not authenticated");
        }

        // 1. Check if OAuth2User principal contains email attribute
        if (authentication.getPrincipal() instanceof OAuth2User oauth2User) {
            String email = oauth2User.getAttribute("email");
            if (email != null && !email.isBlank()) {
                return userRepository.findByEmail(email)
                        .map(User::getId)
                        .orElseThrow(() -> ApiException.unauthorized("User not found for OAuth email: " + email));
            }
        }

        // 2. Fallback to authentication.getName() (Standard JWT Email)
        String principalName = authentication.getName();
        if (principalName != null && !principalName.isBlank()) {
            return userRepository.findByEmail(principalName)
                    .map(User::getId)
                    .orElseThrow(() -> ApiException.unauthorized("User not found for principal: " + principalName));
        }

        throw ApiException.unauthorized("User not found for this session");
    }
}