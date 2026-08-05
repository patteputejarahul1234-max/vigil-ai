package com.vigilai.service;

import com.vigilai.dto.RegisterRequest;
import com.vigilai.dto.UserProfileResponse;
import com.vigilai.entity.Role;
import com.vigilai.entity.User;
import com.vigilai.exception.ApiException;
import com.vigilai.repository.PasswordResetTokenRepository;
import com.vigilai.repository.UserRepository;
import com.vigilai.repository.VerificationTokenRepository;
import com.vigilai.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * AuthService holds the registration/login business rules — this is
 * pure unit testing with Mockito, no Spring context, no database.
 * Fast, and pins down the rules that actually matter (duplicate email
 * rejection, password hashing, verification token issuance).
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private VerificationTokenRepository verificationTokenRepository;
    @Mock private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtService jwtService;
    @Mock private EmailService emailService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository, verificationTokenRepository, passwordResetTokenRepository,
                passwordEncoder, authenticationManager, jwtService, emailService,
                86_400_000L, 1_800_000L
        );
    }

    @Test
    void registerRejectsDuplicateEmail() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Test User");
        request.setEmail("existing@example.com");
        request.setPassword("password123");

        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        ApiException ex = assertThrows(ApiException.class, () -> authService.register(request));
        assertTrue(ex.getMessage().toLowerCase().contains("already exists"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerHashesPasswordAndIssuesVerificationToken() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Test User");
        request.setEmail("new@example.com");
        request.setPassword("plaintext-password");

        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("plaintext-password")).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });

        UserProfileResponse response = authService.register(request);

        assertEquals("new@example.com", response.getEmail());
        assertFalse(response.isEmailVerified());
        assertEquals(Role.USER, response.getRole());

        ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(savedUser.capture());
        assertEquals("hashed-password", savedUser.getValue().getPasswordHash());

        // Never store or return the plaintext password
        assertNotEquals("plaintext-password", savedUser.getValue().getPasswordHash());

        verify(emailService).sendVerificationEmail(eq("new@example.com"), any());
    }

    @Test
    void resendVerificationRejectsAlreadyVerifiedUser() {
        User verifiedUser = User.builder().id(1L).email("verified@example.com").emailVerified(true).build();
        when(userRepository.findByEmail("verified@example.com")).thenReturn(Optional.of(verifiedUser));

        ApiException ex = assertThrows(ApiException.class,
                () -> authService.resendVerification("verified@example.com"));
        assertTrue(ex.getMessage().toLowerCase().contains("already verified"));
    }
}
