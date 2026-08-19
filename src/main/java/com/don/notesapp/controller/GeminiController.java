package com.don.notesapp.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class GeminiController {

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    public GeminiController(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newHttpClient();
    }

    @GetMapping("/greeting")
    public String greeting() {
        String fallback = "Welcome!\nCapture your thoughts, organize your mind.";

        try {
            int hour = LocalTime.now().getHour();
            String timeOfDay;

            if (hour < 5 || hour >= 22) {
                timeOfDay = "night";
            } else if (hour < 12) {
                timeOfDay = "morning";
            } else if (hour < 18) {
                timeOfDay = "afternoon";
            } else {
                timeOfDay = "evening";
            }

            String prompt = "Generate a short, warm, inspiring 2-line greeting for a notes app dashboard.\n"
                    + "It is currently " + timeOfDay + ".\n"
                    + "Line 1: A warm greeting (max 4 words, can be creative, not just 'Good morning').\n"
                    + "Line 2: A short motivational subtitle (max 8 words, about capturing thoughts or creativity).\n"
                    + "Return ONLY the two lines separated by a newline, no quotes, no explanation.";

            String requestBody = objectMapper.writeValueAsString(Map.of(
                    "contents", new Object[]{Map.of(
                            "parts", new Object[]{Map.of("text", prompt)}
                    )}
            ));

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl + (apiUrl.contains("?") ? "&" : "?")
                            + "key=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8)))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    httpRequest,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                System.out.println("=== Gemini greeting error: " + response.statusCode() + " " + response.body());
                return fallback;
            }

            JsonNode text = objectMapper.readTree(response.body())
                    .path("candidates")
                    .path(0)
                    .path("content")
                    .path("parts")
                    .path(0)
                    .path("text");

            return text.isTextual() && !text.asText().isBlank()
                    ? text.asText().trim()
                    : fallback;
        } catch (Exception exception) {
            System.out.println("=== Gemini greeting exception: " + exception.getMessage());
            return fallback;
        }
        }

    @PostMapping("/generate-title")
    public String generateTitle(@RequestBody GenerateTitleRequest request) {
        try {
            String prompt = "Generate a short, clear, catchy title (max 4 words) for a note. Treat the note as personalized entity and make the title like you would name a note for yourself. "
                    + "with the following content. Return ONLY the title, no quotes, no explanation:\n\n"
                    + request.content();

            String requestBody = objectMapper.writeValueAsString(Map.of(
                    "contents", new Object[]{Map.of(
                            "parts", new Object[]{Map.of("text", prompt)}
                    )}
            ));

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl + (apiUrl.contains("?") ? "&" : "?")
                            + "key=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8)))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    httpRequest,
                    HttpResponse.BodyHandlers.ofString()
            );

                        if (response.statusCode() < 200 || response.statusCode() >= 300) {
                                System.out.println("=== Gemini error: " + response.statusCode() + " " + response.body());
                                return "Untitled Note";
                        }

            JsonNode title = objectMapper.readTree(response.body())
                    .path("candidates")
                    .path(0)
                    .path("content")
                    .path("parts")
                    .path(0)
                    .path("text");

            return title.isTextual() && !title.asText().isBlank()
                    ? title.asText()
                    : "Untitled Note";
                } catch (Exception exception) {
                        System.out.println("=== Gemini exception: " + exception.getMessage());
                        return "Untitled Note";
                }
    }

    @PostMapping("/generate-tags")
    public String generateTags(@RequestBody GenerateTagsRequest request) {
        try {
            String prompt = "Generate 3 to 5 short, relevant tags for a note with the following content. "
                    + "Return ONLY the tags as a comma-separated list, lowercase, no hashtags, no explanation:\n\n"
                    + request.content();

            String requestBody = objectMapper.writeValueAsString(Map.of(
                    "contents", new Object[]{Map.of(
                            "parts", new Object[]{Map.of("text", prompt)}
                    )}
            ));

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl + (apiUrl.contains("?") ? "&" : "?")
                            + "key=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8)))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    httpRequest,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                System.out.println("=== Gemini tags error: " + response.statusCode() + " " + response.body());
                return "";
            }

            JsonNode text = objectMapper.readTree(response.body())
                    .path("candidates")
                    .path(0)
                    .path("content")
                    .path("parts")
                    .path(0)
                    .path("text");

            return text.isTextual() && !text.asText().isBlank()
                    ? text.asText().trim()
                    : "";

        } catch (Exception exception) {
            System.out.println("=== Gemini tags exception: " + exception.getMessage());
            return "";
        }
    }

    @PostMapping("/summarize")
    public String summarize(@RequestBody SummarizeRequest request) {
        try {
            String prompt = "Summarize the following note in clear, concise and brief bullet points(Number of bullet point are estimated according to the size of the note and enough to cover the summary)."
                    +"Make it personalized and easy to understand for not only the owner but also for collaborators (editors and viewers)."
                    + "Return ONLY the summary, no preamble, no explanation:\n\n"
                    + request.content();

            String requestBody = objectMapper.writeValueAsString(Map.of(
                    "contents", new Object[]{Map.of(
                            "parts", new Object[]{Map.of("text", prompt)}
                    )}
            ));

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl + (apiUrl.contains("?") ? "&" : "?")
                            + "key=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8)))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    httpRequest,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                System.out.println("=== Gemini summary error: " + response.statusCode() + " " + response.body());
                return "";
            }

            JsonNode text = objectMapper.readTree(response.body())
                    .path("candidates")
                    .path(0)
                    .path("content")
                    .path("parts")
                    .path(0)
                    .path("text");

            return text.isTextual() && !text.asText().isBlank()
                    ? text.asText().trim()
                    : "";

        } catch (Exception exception) {
            System.out.println("=== Gemini summary exception: " + exception.getMessage());
            return "";
        }
    }

    public record GenerateTitleRequest(String content) {
    }

    public record GenerateTagsRequest(String content) {
    }

    public record SummarizeRequest(String content) {
    }
}