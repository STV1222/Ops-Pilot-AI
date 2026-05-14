package com.opspilot.application.agent.tools;

import com.opspilot.application.agent.AgentTool;
import com.opspilot.application.ai.AIReportGenerationService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class GenerateReportAgentTool implements AgentTool {

    private final AIReportGenerationService aiReportGenerationService;

    public GenerateReportAgentTool(AIReportGenerationService aiReportGenerationService) {
        this.aiReportGenerationService = aiReportGenerationService;
    }

    @Override
    public String name() { return "generate_report"; }

    @Override
    public String description() {
        return "Generate a markdown procurement recommendation report. Returns markdown, best_supplier, confidence, and anomaly_count as flat fields — pass these directly to submit_report. Call after compare_quotations and detect_anomalies.";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "comparison", Map.of("type", "object", "description", "Result from compare_quotations tool"),
                        "anomalies", Map.of("type", "array", "description", "Result from detect_anomalies tool",
                                "items", Map.of("type", "object"))
                ),
                "required", List.of("comparison", "anomalies")
        );
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> execute(Map<String, Object> args) {
        try {
            Map<String, Object> comparison = (Map<String, Object>) args.getOrDefault("comparison", Map.of());
            List<Map<String, Object>> anomalies = (List<Map<String, Object>>) args.getOrDefault("anomalies", List.of());
            Map<String, Object> report = aiReportGenerationService.generateReport(comparison, anomalies);

            // Flatten so the agent can pass these directly to submit_report without deep nesting
            String markdown = (String) report.getOrDefault("markdown", "");
            @SuppressWarnings("unchecked")
            Map<String, Object> summary = (Map<String, Object>) report.getOrDefault("summary", Map.of());
            String bestSupplier = String.valueOf(summary.getOrDefault("bestSupplier", "Unknown"));
            double confidence = toDouble(summary.getOrDefault("confidence", 0.8));
            // Normalize: LLM sometimes returns 0-100 instead of 0.0-1.0
            if (confidence > 1.0) confidence = confidence / 100.0;
            int anomalyCount = toInt(summary.getOrDefault("anomalyCount", anomalies.size()));

            return Map.of(
                    "markdown", markdown,
                    "best_supplier", bestSupplier,
                    "confidence", confidence,
                    "anomaly_count", anomalyCount,
                    "status", "ok"
            );
        } catch (Exception e) {
            return Map.of("markdown", "", "best_supplier", "Unknown",
                    "confidence", 0.0, "anomaly_count", 0, "status", "error", "error", e.getMessage());
        }
    }

    private double toDouble(Object v) {
        if (v instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(v.toString()); } catch (Exception e) { return 0.8; }
    }

    private int toInt(Object v) {
        if (v instanceof Number n) return n.intValue();
        try { return Integer.parseInt(v.toString()); } catch (Exception e) { return 0; }
    }
}
