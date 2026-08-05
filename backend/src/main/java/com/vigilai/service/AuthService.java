package com.vigilai.service;

import com.vigilai.dto.*;
import com.vigilai.entity.*;
import com.vigilai.exception.ApiException;
import com.vigilai.repository.PasswordResetTokenRepository;
import com.vigilai.repository.UserRepository;
import com.vigilai.repository.VerificationTokenRepository;
import com.vigilai.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final long verificationTokenExpiryMs;
    private final long resetTokenExpiryMs;

    public AuthService(
            UserRepository userRepository,
            VerificationTokenRepository verificationTokenRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            EmailService emailService,
            @Value("${app.verification-token-expiry-ms}") long verificationTokenExpiryMs,
            @Value("${app.reset-token-expiry-ms}") long resetTokenExpiryMs
    ) {
        this.userRepository = userRepository;
        this.verificationTokenRepository = verificationTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.emailService = emailService;
        this.verificationTokenExpiryMs = verificationTokenExpiryMs;
        this.resetTokenExpiryMs = resetTokenExpiryMs;
    }

    @Transactional
    public UserProfileResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw ApiException.conflict("An account with this email already exists");
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .provider(AuthProvider.LOCAL)
                .emailVerified(false)
                .build();

        user = userRepository.save(user);
        issueVerificationToken(user);
        log.info("New user registered: userId={} email={}", user.getId(), user.getEmail());

        return toProfileResponse(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (Exception ex) {
            log.warn("Failed login attempt for email={}", request.getEmail());
            throw new BadCredentialsException("Invalid email or password");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> ApiException.notFound("User not found"));

        log.info("Login succeeded: userId={} email={}", user.getId(), user.getEmail());
        return buildAuthResponse(user);
    }

    @Transactional
    public void verifyEmail(String token) {
        VerificationToken vt = verificationTokenRepository.findByToken(token)
                .orElseThrow(() -> ApiException.badRequest("Invalid or expired verification link"));

        if (vt.getExpiresAt().isBefore(Instant.now())) {
            throw ApiException.badRequest("This verification link has expired. Please request a new one.");
        }

        User user = userRepository.findById(vt.getUserId())
                .orElseThrow(() -> ApiException.notFound("User not found"));

        user.setEmailVerified(true);
        userRepository.save(user);
        verificationTokenRepository.deleteByUserId(user.getId());
    }

    @Transactional
    public void resendVerification(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> ApiException.notFound("No account found with this email"));

        if (user.isEmailVerified()) {
            throw ApiException.badRequest("This email is already verified");
        }

        verificationTokenRepository.deleteByUserId(user.getId());
        issueVerificationToken(user);
    }

    @Transactional
    public void forgotPassword(String email) {
        // Always behave the same whether or not the email exists, to avoid leaking account existence.
        userRepository.findByEmail(email).ifPresent(user -> {
            String token = UUID.randomUUID().toString();
            PasswordResetToken resetToken = PasswordResetToken.builder()
                    .token(token)
                    .userId(user.getId())
                    .expiresAt(Instant.now().plusMillis(resetTokenExpiryMs))
                    .used(false)
                    .build();
            passwordResetTokenRepository.save(resetToken);
            emailService.sendPasswordResetEmail(user.getEmail(), token);
        });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> ApiException.badRequest("Invalid or expired reset link"));

        if (resetToken.isUsed() || resetToken.getExpiresAt().isBefore(Instant.now())) {
            throw ApiException.badRequest("This reset link has expired or was already used");
        }

        User user = userRepository.findById(resetToken.getUserId())
                .orElseThrow(() -> ApiException.notFound("User not found"));

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);
    }

    // --- helpers ---

    private void issueVerificationToken(User user) {
        String token = UUID.randomUUID().toString();
        VerificationToken vt = VerificationToken.builder()
                .token(token)
                .userId(user.getId())
                .expiresAt(Instant.now().plusMillis(verificationTokenExpiryMs))
                .build();
        verificationTokenRepository.save(vt);
        emailService.sendVerificationEmail(user.getEmail(), token);
    }

    public AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtService.generateAccessToken(user.getEmail(), user.getId(), user.getRole().name());
        String refreshToken = jwtService.generateRefreshToken(user.getEmail(), user.getId());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .user(toProfileResponse(user))
                .build();
    }

    public UserProfileResponse toProfileResponse(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .emailVerified(user.isEmailVerified())
                .avatarUrl(user.getAvatarUrl())
                .bio(user.getBio())
                .timezone(user.getTimezone())
                .build();
    }
}
