package com.opspilot.application.agent.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opspilot.application.agent.AgentTool;
import com.opspilot.application.ai.AIExtractionService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class ExtractItemsAgentTool implements AgentTool {

    private final AIExtractionService aiExtractionService;
    private final ObjectMapper mapper = new ObjectMapper();

    public ExtractItemsAgentTool(AIExtractionService aiExtractionService) {
        this.aiExtractionService = aiExtractionService;
    }

    @Override
    public String name() { return "extract_items"; }

    @Override
    public String description() {
        return "Extract structured line items (supplier name, item name, quantity, unit price) from raw document text. Call this first with the full combined document text.";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "text", Map.of("type", "string", "description", "Raw text from supplier quotation documents")
                ),
                "required", List.of("text")
        );
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> args) {
        String text = (String) args.getOrDefault("text", "");
        List<Map<String, Object>> items = aiExtractionService.extractStructuredQuotationItems(text);
        try {
            return Map.of("items", items, "count", items.size(), "status", "ok");
        } catch (Exception e) {
            return Map.of("items", List.of(), "count", 0, "status", "error", "error", e.getMessage());
        }
    }
}
