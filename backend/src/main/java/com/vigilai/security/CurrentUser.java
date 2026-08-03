package com.vigilai.security;

import com.vigilai.entity.User;
import com.vigilai.exception.ApiException;
import com.vigilai.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * The JWT filter only puts the user's email on the SecurityContext
 * (see JwtAuthFilter). Every Stage 2 controller needs the numeric
 * user id instead, so this resolves email -> User once per request.
 */
@Component
public class CurrentUser {

    private final UserRepository userRepository;

    public CurrentUser(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Long idOf(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .map(User::getId)
                .orElseThrow(() -> ApiException.unauthorized("User not found for this session"));
    }
}
