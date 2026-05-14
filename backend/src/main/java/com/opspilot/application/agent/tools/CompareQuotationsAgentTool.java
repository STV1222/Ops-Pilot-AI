package com.opspilot.application.agent.tools;

import com.opspilot.application.agent.AgentTool;
import com.opspilot.application.ai.AIComparisonService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class CompareQuotationsAgentTool implements AgentTool {

    private final AIComparisonService aiComparisonService;

    public CompareQuotationsAgentTool(AIComparisonService aiComparisonService) {
        this.aiComparisonService = aiComparisonService;
    }

    @Override
    public String name() { return "compare_quotations"; }

    @Override
    public String description() {
        return "AI-powered supplier comparison. Ranks suppliers by total cost and value, returns best supplier with confidence score and per-item price breakdown. Call after extract_items.";
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
            Map<String, Object> comparison = aiComparisonService.compare(items);
            return Map.of("comparison", comparison, "status", "ok");
        } catch (Exception e) {
            return Map.of("comparison", Map.of(), "status", "error", "error", e.getMessage());
        }
    }
}
