package com.vigilai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Calls Gemini's vision model to check whether a submitted photo
 * plausibly shows a task being completed. This is the core AI piece
 * of "Proof of Execution" — the differentiator from every other
 * habit-tracker that just takes a tap on "Done" at face value.
 */
@Service
public class GeminiVisionService {

    private static final Logger log = LoggerFactory.getLogger(GeminiVisionService.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;

    public GeminiVisionService(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            @Value("${app.gemini.api-key}") String apiKey,
            @Value("${app.gemini.model}") String model
    ) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
    }

    public record ValidationResult(boolean verified, String reason) {}

    /**
     * @param imageBytes raw photo bytes — never persisted, only forwarded to Gemini and discarded
     * @param taskTitle  what the user was supposed to do (e.g. "Drink water")
     * @param taskDescription optional extra context, may be null/blank
     */
    public ValidationResult validate(byte[] imageBytes, String mimeType, String taskTitle, String taskDescription) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("GEMINI_API_KEY not configured — auto-rejecting proof submission for task '{}'", taskTitle);
            return new ValidationResult(false, "AI verification is not configured on this server.");
        }

        String prompt = buildPrompt(taskTitle, taskDescription);
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(Map.of(
                        "parts", List.of(
                                Map.of("text", prompt),
                                Map.of("inline_data", Map.of(
                                        "mime_type", mimeType != null ? mimeType : "image/jpeg",
                                        "data", base64Image
                                ))
                        )
                )),
                "generationConfig", Map.of("response_mime_type", "application/json")
        );

        String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                + model + ":generateContent?key=" + apiKey;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            JsonNode response = restTemplate.postForObject(
                    url, new HttpEntity<>(requestBody, headers), JsonNode.class);

            String rawText = response
                    .path("candidates").path(0)
                    .path("content").path("parts").path(0)
                    .path("text").asText();

            JsonNode parsed = objectMapper.readTree(rawText);
            boolean verified = parsed.path("verified").asBoolean(false);
            String reason = parsed.path("reason").asText("No reason given.");

            return new ValidationResult(verified, reason);

        } catch (Exception ex) {
            log.error("Gemini vision call failed for task '{}': {}", taskTitle, ex.getMessage());
            return new ValidationResult(false, "Could not verify the photo right now — please try again.");
        }
    }

    private String buildPrompt(String taskTitle, String taskDescription) {
        return "You are verifying whether a photo shows completion of a specific task.\n"
                + "Task: \"" + taskTitle + "\""
                + (taskDescription != null && !taskDescription.isBlank() ? " (" + taskDescription + ")" : "")
                + "\n\nLook at the attached image and decide if it plausibly shows this task being done or completed. "
                + "Be reasonably lenient — approve clear, good-faith attempts even if imperfect. "
                + "Reject only if the image is clearly unrelated, blank, or a screenshot/reused stock photo.\n\n"
                + "Respond ONLY with JSON in this exact shape, no other text:\n"
                + "{\"verified\": true or false, \"reason\": \"one short sentence explaining your decision\"}";
    }
}