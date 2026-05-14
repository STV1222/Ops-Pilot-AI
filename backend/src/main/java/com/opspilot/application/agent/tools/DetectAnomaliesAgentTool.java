package com.opspilot.application.agent.tools;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opspilot.application.agent.AgentTool;
import com.opspilot.application.anomaly.AnomalyDetectorService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class DetectAnomaliesAgentTool implements AgentTool {

    private final AnomalyDetectorService anomalyDetectorService;
    private final ObjectMapper mapper = new ObjectMapper();

    public DetectAnomaliesAgentTool(AnomalyDetectorService anomalyDetectorService) {
        this.anomalyDetectorService = anomalyDetectorService;
    }

    @Override
    public String name() { return "detect_anomalies"; }

    @Override
    public String description() {
        return "Run rule-based anomaly detection on extracted line items. Flags price deviations >15% and missing quantities. Call this after extract_items.";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "items", Map.of("type", "array", "description", "List of extracted line items from extract_items tool",
                                "items", Map.of("type", "object"))
                ),
                "required", List.of("items")
        );
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> execute(Map<String, Object> args) {
        try {
            List<Map<String, Object>> items = (List<Map<String, Object>>) args.get("items");
            if (items == null) items = List.of();
            List<Map<String, Object>> anomalies = anomalyDetectorService.detect(items);
            return Map.of("anomalies", anomalies, "count", anomalies.size(), "status", "ok");
        } catch (Exception e) {
            return Map.of("anomalies", List.of(), "count", 0, "status", "error", "error", e.getMessage());
        }
    }
}
