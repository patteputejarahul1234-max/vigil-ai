package com.vigilai.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Plain text generation via Gemini — separate from GeminiVisionService
 * since this never handles images. Powers the AI Assistant feature.
 * IMPORTANT: no "generationConfig: response_mime_type" here — we learned
 * the hard way (Proof of Execution debugging) that forcing strict JSON
 * mode causes this model to hang. Plain-text prompting works reliably.
 */
@Service
public class GeminiTextService {

    private static final Logger log = LoggerFactory.getLogger(GeminiTextService.class);

    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String model;

    public GeminiTextService(
            RestTemplate restTemplate,
            @Value("${app.gemini.api-key}") String apiKey,
            @Value("${app.gemini.model}") String model
    ) {
        this.restTemplate = restTemplate;
        this.apiKey = apiKey;
        this.model = model;
    }

    public String ask(String prompt) {
        if (apiKey == null || apiKey.isBlank()) {
            return "AI Assistant is not configured on this server.";
        }

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(Map.of(
                        "parts", List.of(Map.of("text", prompt))
                ))
        );

        String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                + model + ":generateContent?key=" + apiKey;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            JsonNode response = restTemplate.postForObject(
                    url, new HttpEntity<>(requestBody, headers), JsonNode.class);

            return response
                    .path("candidates").path(0)
                    .path("content").path("parts").path(0)
                    .path("text").asText("I couldn't come up with an answer just now.");

        } catch (Exception ex) {
            log.error("Gemini text call failed: {}", ex.getMessage());
            return "I couldn't reach the AI service right now — please try again in a moment.";
        }
    }
}