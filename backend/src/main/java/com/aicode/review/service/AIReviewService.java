package com.aicode.review.service;

import com.aicode.review.service.analysis.AnalysisFinding;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Stage 3: AI-Powered Review.
 * Sends the submitted code (plus a summary of static-analysis findings) to an
 * OpenAI-compatible chat completions endpoint and asks for a structured JSON
 * review: bugs, code smells, performance, security, refactors, naming,
 * a quality score, and a summary - exactly per the prompt template in the
 * project spec.
 */
@Slf4j
@Service
public class AIReviewService {

    @Value("${app.ai.provider.base-url}")
    private String baseUrl;

    @Value("${app.ai.provider.api-key}")
    private String apiKey;

    @Value("${app.ai.provider.model}")
    private String model;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String SYSTEM_PROMPT = """
            You are a Senior Java Software Engineer performing an automated code review.
            Review the provided source code and return ONLY a valid JSON object (no markdown
            fences, no commentary) with this exact shape:

            {
              "summary": "2-4 sentence overall summary of code quality",
              "qualityScore": 0-100 integer,
              "findings": [
                {
                  "severity": "CRITICAL|HIGH|MEDIUM|LOW|INFO",
                  "category": "BUG|SECURITY|CODE_SMELL|PERFORMANCE|STYLE|MAINTAINABILITY",
                  "fileName": "string",
                  "lineNumber": integer or null,
                  "issue": "short title",
                  "explanation": "why this matters",
                  "suggestion": "concrete fix or refactor, including better names where relevant"
                }
              ]
            }

            Cover: bugs, security vulnerabilities, code smells, performance improvements,
            best practices, refactoring suggestions, and better variable/method names.
            """;

    public AiReviewResult review(List<Path> sourceFiles, List<AnalysisFinding> staticFindings) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("No AI provider API key configured (set OPENAI_API_KEY). Skipping AI review stage.");
            return AiReviewResult.unavailable();
        }

        try {
            String codeBundle = buildCodeBundle(sourceFiles);
            String staticSummary = summarizeStaticFindings(staticFindings);

            String userPrompt = "Static analysis already found these issues (do not repeat them verbatim, "
                    + "add NEW insight instead):\n" + staticSummary
                    + "\n\n--- SOURCE CODE ---\n" + codeBundle;

            WebClient client = WebClient.builder()
                    .baseUrl(baseUrl)
                    .defaultHeader("Authorization", "Bearer " + apiKey)
                    .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .build();

            Map<String, Object> body = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of("role", "system", "content", SYSTEM_PROMPT),
                            Map.of("role", "user", "content", userPrompt)
                    ),
                    "temperature", 0.2,
                    "response_format", Map.of("type", "json_object")
            );

            String response = client.post()
                    .uri("/chat/completions")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(60));

            return parseResponse(response);

        } catch (Exception e) {
            log.error("AI review call failed: {}", e.getMessage(), e);
            return AiReviewResult.error(e.getMessage());
        }
    }

    private static final java.util.Set<String> SUPPORTED_EXT = java.util.Set.of(
            ".java", ".py", ".js", ".jsx", ".ts", ".tsx");

    private String buildCodeBundle(List<Path> files) throws Exception {
        StringBuilder sb = new StringBuilder();
        int totalChars = 0;
        // Cap total code sent to the model to keep requests reasonable
        final int MAX_CHARS = 60_000;

        for (Path file : files) {
            String name = file.getFileName().toString().toLowerCase();
            if (SUPPORTED_EXT.stream().noneMatch(name::endsWith)) continue;
            String content = Files.readString(file);
            String block = "\n\n// FILE: " + file.getFileName() + "\n" + content;
            if (totalChars + block.length() > MAX_CHARS) {
                sb.append("\n\n// ... remaining files truncated to fit context window ...");
                break;
            }
            sb.append(block);
            totalChars += block.length();
        }
        return sb.toString();
    }

    private String summarizeStaticFindings(List<AnalysisFinding> findings) {
        if (findings.isEmpty()) return "No static analysis findings.";
        StringBuilder sb = new StringBuilder();
        findings.stream().limit(30).forEach(f ->
                sb.append("- [").append(f.getSource()).append("/").append(f.getSeverity()).append("] ")
                  .append(f.getIssue()).append(" (").append(f.getFileName()).append(")\n"));
        return sb.toString();
    }

    private AiReviewResult parseResponse(String rawResponse) throws Exception {
        JsonNode root = objectMapper.readTree(rawResponse);
        String content = root.path("choices").get(0).path("message").path("content").asText();

        // Strip accidental markdown fences just in case
        content = content.trim();
        if (content.startsWith("```")) {
            content = content.replaceAll("^```(json)?", "").replaceAll("```$", "").trim();
        }

        JsonNode reviewJson = objectMapper.readTree(content);

        List<AnalysisFinding> findings = new ArrayList<>();
        if (reviewJson.has("findings")) {
            for (JsonNode f : reviewJson.get("findings")) {
                findings.add(AnalysisFinding.builder()
                        .source("AI")
                        .severity(f.path("severity").asText("INFO"))
                        .category(f.path("category").asText("CODE_SMELL"))
                        .fileName(f.path("fileName").asText(null))
                        .lineNumber(f.path("lineNumber").isNumber() ? f.get("lineNumber").asInt() : null)
                        .issue(f.path("issue").asText())
                        .explanation(f.path("explanation").asText())
                        .suggestion(f.path("suggestion").asText())
                        .build());
            }
        }

        String summary = reviewJson.path("summary").asText("AI review completed.");
        Integer score = reviewJson.has("qualityScore") ? reviewJson.get("qualityScore").asInt() : null;

        return new AiReviewResult(true, summary, score, findings, null);
    }

    private static final String REFACTOR_SYSTEM_PROMPT = """
            You are a Senior Java Software Engineer performing an AI-powered refactor.
            Rewrite the given file applying best practices: better names, reduced
            complexity, removed code smells, and fixed obvious bugs, while preserving
            behavior. Respond with ONLY the complete refactored source code for the
            file - no markdown fences, no commentary, no explanation before or after.
            """;

    /** AI-powered code refactoring (bonus feature): returns a fully rewritten file. */
    public RefactorResult refactorFile(Path file) {
        if (apiKey == null || apiKey.isBlank()) {
            return new RefactorResult(false, null, "AI refactor unavailable: no API key configured (set OPENAI_API_KEY).");
        }

        try {
            String original = Files.readString(file);
            WebClient client = WebClient.builder()
                    .baseUrl(baseUrl)
                    .defaultHeader("Authorization", "Bearer " + apiKey)
                    .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .build();

            Map<String, Object> body = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of("role", "system", "content", REFACTOR_SYSTEM_PROMPT),
                            Map.of("role", "user", "content", "FILE: " + file.getFileName() + "\n\n" + original)
                    ),
                    "temperature", 0.2
            );

            String response = client.post()
                    .uri("/chat/completions")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(60));

            JsonNode root = objectMapper.readTree(response);
            String content = root.path("choices").get(0).path("message").path("content").asText().trim();
            if (content.startsWith("```")) {
                content = content.replaceAll("^```(java|python|javascript)?", "").replaceAll("```$", "").trim();
            }

            return new RefactorResult(true, content, null);
        } catch (Exception e) {
            log.error("AI refactor failed: {}", e.getMessage(), e);
            return new RefactorResult(false, null, e.getMessage());
        }
    }

    public record RefactorResult(boolean available, String refactoredCode, String error) {}

    public record AiReviewResult(
            boolean available,
            String summary,
            Integer qualityScore,
            List<AnalysisFinding> findings,
            String error
    ) {
        public static AiReviewResult unavailable() {
            return new AiReviewResult(false,
                    "AI review skipped: no API key configured (set the OPENAI_API_KEY environment variable to enable it).",
                    null, List.of(), "NO_API_KEY");
        }

        public static AiReviewResult error(String message) {
            return new AiReviewResult(false,
                    "AI review failed and was skipped. Static analysis results are still shown below.",
                    null, List.of(), message);
        }
    }
}
