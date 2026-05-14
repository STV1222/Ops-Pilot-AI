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
        return "Generate a markdown procurement recommendation report from the comparison results and anomalies. Returns full markdown and a JSON summary. Call after compare_quotations and detect_anomalies.";
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
            return Map.of("report", report, "status", "ok");
        } catch (Exception e) {
            return Map.of("report", Map.of(), "status", "error", "error", e.getMessage());
        }
    }
}
