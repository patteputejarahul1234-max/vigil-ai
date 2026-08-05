package com.vigilai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vigilai.entity.VerificationToken;
import com.vigilai.repository.UserRepository;
import com.vigilai.repository.VerificationTokenRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end test of the exact flow you verified manually by hand with
 * curl in Stage 1: register -> read the verification token from the DB
 * -> verify -> login -> call a JWT-protected route. Runs against the
 * real Spring context and an in-memory H2 database, no mocking.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthFlowIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private VerificationTokenRepository verificationTokenRepository;
    @Autowired private UserRepository userRepository;

    @Test
    void registerVerifyLoginAndAccessProtectedRouteSucceeds() throws Exception {
        String email = "integration-test@example.com";

        // 1. Register
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "fullName", "Integration Test",
                                "email", email,
                                "password", "password123"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailVerified").value(false));

        // 2. Grab the verification token for THIS user specifically — the test DB
        // is shared across test methods in this class, so we can't assume "first token".
        Long userId = userRepository.findByEmail(email).orElseThrow().getId();
        VerificationToken token = verificationTokenRepository.findAll().stream()
                .filter(t -> t.getUserId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No verification token was issued for " + email));

        // 3. Verify
        mockMvc.perform(get("/api/auth/verify-email").param("token", token.getToken()))
                .andExpect(status().isOk());

        // 4. Login
        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", "password123"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.emailVerified").value(true))
                .andReturn().getResponse().getContentAsString();

        String accessToken = objectMapper.readTree(loginResponse).get("accessToken").asText();
        assertTrue(accessToken.length() > 20, "Expected a real JWT, got: " + accessToken);

        // 5. Confirm the protected route accepts the token
        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    void loginWithWrongPasswordReturns401() throws Exception {
        String email = "wrong-password-test@example.com";

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "fullName", "Test", "email", email, "password", "correct-password"
                ))));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email, "password", "wrong-password"
                        ))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedRouteRejectsRequestWithNoToken() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }
}
