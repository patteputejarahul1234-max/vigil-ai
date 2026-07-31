package com.vigilai.config;

import com.vigilai.entity.AuthProvider;
import com.vigilai.entity.Role;
import com.vigilai.entity.User;
import com.vigilai.repository.UserRepository;
import com.vigilai.security.JwtService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * After Google/GitHub authenticate the user, this creates or links a local
 * User record, mints our own JWT (so the rest of the app only ever deals
 * with one auth format), and redirects back to the frontend with it.
 */
@Component
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final String frontendUrl;

    public OAuth2LoginSuccessHandler(
            UserRepository userRepository,
            JwtService jwtService,
            @Value("${app.frontend-url}") String frontendUrl
    ) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.frontendUrl = frontendUrl;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                         Authentication authentication) throws IOException, ServletException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String registrationId = extractRegistrationId(request);

        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String avatar = oAuth2User.getAttribute("picture") != null
                ? oAuth2User.getAttribute("picture")
                : oAuth2User.getAttribute("avatar_url");

        AuthProvider provider = "github".equalsIgnoreCase(registrationId) ? AuthProvider.GITHUB : AuthProvider.GOOGLE;

        User user = userRepository.findByEmail(email).orElseGet(() -> User.builder()
                .fullName(name != null ? name : email)
                .email(email)
                .provider(provider)
                .role(Role.USER)
                .emailVerified(true) // OAuth providers already verify email ownership
                .avatarUrl(avatar)
                .build());

        user.setAvatarUrl(avatar);
        user = userRepository.save(user);

        String accessToken = jwtService.generateAccessToken(user.getEmail(), user.getId(), user.getRole().name());
        String refreshToken = jwtService.generateRefreshToken(user.getEmail(), user.getId());

        String redirectUrl = frontendUrl + "/oauth/callback"
                + "?accessToken=" + accessToken
                + "&refreshToken=" + refreshToken;

        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }

    private String extractRegistrationId(HttpServletRequest request) {
        // Path looks like /login/oauth2/code/google or /login/oauth2/code/github
        String uri = request.getRequestURI();
        String[] parts = uri.split("/");
        return parts[parts.length - 1];
    }
}
