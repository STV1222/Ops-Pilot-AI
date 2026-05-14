package com.opspilot.infrastructure.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class OpenRouterClient {
    private static final Logger log = LoggerFactory.getLogger(OpenRouterClient.class);

    private final OpenRouterProperties props;
    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OpenRouterClient(OpenRouterProperties props) {
        this.props = props;
        this.webClient = WebClient.builder()
                .baseUrl(props.getBaseUrl().replaceAll("/$", ""))
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * Chat completion with JSON object response. Returns empty if not configured or on failure.
     */
    public Optional<String> completeJsonObject(String systemPrompt, String userPrompt) {
        if (!props.isConfigured()) {
            return Optional.empty();
        }
        Map<String, Object> body = Map.of(
                "model", props.getModel(),
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                ),
                "response_format", Map.of("type", "json_object"),
                "temperature", 0.2
        );
        try {
            String raw = webClient.post()
                    .uri("/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + props.getApiKey().trim())
                    .header("HTTP-Referer", props.getHttpReferer())
                    .header("X-Title", props.getAppName())
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .timeout(Duration.ofSeconds(120))
                    .map(node -> {
                        JsonNode choices = node.path("choices");
                        if (!choices.isArray() || choices.isEmpty()) {
                            return "";
                        }
                        return choices.get(0).path("message").path("content").asText("");
                    })
                    .onErrorResume(e -> {
                        log.warn("OpenRouter request failed: {}", e.toString());
                        return Mono.just("");
                    })
                    .subscribeOn(Schedulers.boundedElastic())
                    .block(Duration.ofSeconds(125));
            if (raw == null || raw.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(stripJsonFence(raw));
        } catch (Exception e) {
            log.warn("OpenRouter completion error", e);
            return Optional.empty();
        }
    }

    static String stripJsonFence(String text) {
        String t = text.strip();
        if (t.startsWith("```")) {
            int first = t.indexOf('\n');
            int last = t.lastIndexOf("```");
            if (first > 0 && last > first) {
                t = t.substring(first + 1, last).strip();
            }
        }
        return t;
    }
}
