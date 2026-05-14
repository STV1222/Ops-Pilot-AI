package com.opspilot.application.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opspilot.infrastructure.ai.OpenRouterClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class AIReportGenerationService {
    private static final Logger log = LoggerFactory.getLogger(AIReportGenerationService.class);

    private final OpenRouterClient openRouterClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AIReportGenerationService(OpenRouterClient openRouterClient) {
        this.openRouterClient = openRouterClient;
    }

    public Map<String, Object> generateReport(Map<String, Object> comparison, List<Map<String, Object>> anomalies) {
        String system = """
                You write an operational quotation comparison report for business users (not a chat).
                Respond with a single JSON object only, no markdown fences.
                Schema: {"markdown":"string (full markdown report)","summary":{"bestSupplier":"string","confidence":number,"anomalyCount":number}}.
                Include sections: Recommendation, Pricing Comparison (markdown table with ACTUAL prices from itemComparison — never use placeholders like $X1), Anomalies, Confidence.
                The itemComparison field contains real unit prices per supplier per item — use those exact numbers in the table.
                Use the provided comparison and anomaly list faithfully.""";

        String user;
        try {
            user = "comparison:\n" + objectMapper.writeValueAsString(comparison)
                    + "\n\nanomalies:\n" + objectMapper.writeValueAsString(anomalies);
        } catch (Exception e) {
            user = "comparison: " + comparison + "\nanomalies: " + anomalies;
        }

        Optional<String> json = openRouterClient.completeJsonObject(system, user);
        if (json.isPresent()) {
            try {
                JsonNode root = objectMapper.readTree(json.get());
                String md = root.path("markdown").asText("");
                JsonNode sum = root.path("summary");
                if (!md.isBlank() && sum.isObject()) {
                    Map<String, Object> summary = objectMapper.convertValue(sum, new com.fasterxml.jackson.core.type.TypeReference<>() {});
                    return Map.of("markdown", md, "summary", summary);
                }
            } catch (Exception e) {
                log.warn("Failed to parse report JSON: {}", e.toString());
            }
        }
        return stubReport(comparison, anomalies);
    }

    private Map<String, Object> stubReport(Map<String, Object> comparison, List<Map<String, Object>> anomalies) {
        String markdown = """
                # Quotation Comparison Report

                ## Recommendation
                - Best Supplier: %s
                - Confidence: %s

                ## Rationale
                %s

                ## Anomalies
                - Count: %d
                """.formatted(comparison.get("bestSupplier"), comparison.get("confidence"), comparison.get("summary"), anomalies.size());

        return Map.of(
                "markdown", markdown,
                "summary", Map.of(
                        "bestSupplier", comparison.get("bestSupplier"),
                        "confidence", comparison.get("confidence"),
                        "anomalyCount", anomalies.size()
                )
        );
    }
}
