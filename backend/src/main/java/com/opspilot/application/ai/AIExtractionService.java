package com.opspilot.application.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opspilot.infrastructure.ai.OpenRouterClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class AIExtractionService {
    private static final Logger log = LoggerFactory.getLogger(AIExtractionService.class);

    private final OpenRouterClient openRouterClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AIExtractionService(OpenRouterClient openRouterClient) {
        this.openRouterClient = openRouterClient;
    }

    public List<Map<String, Object>> extractStructuredQuotationItems(String extractedText) {
        String truncated = extractedText == null ? "" : extractedText.substring(0, Math.min(extractedText.length(), 14_000));
        String system = """
                You extract structured quotation line items from operational documents (PDF text, Excel dumps, mixed tables).
                The input may contain multiple supplier quotations concatenated together.
                Respond with a single JSON object only, no markdown fences, no commentary.
                Schema: {"items":[{"supplierName":"string (company name or Supplier A/B if unclear)","itemName":"string","quantity":number or null,"unit":"string or null","unitPrice":"decimal as string","totalPrice":"decimal as string or null"}]}.
                Each row must have a supplierName. If two suppliers quote the same item, emit two separate rows with different supplierName values.
                Infer reasonable rows even if formatting is poor; omit empty rows.""";

        Optional<String> json = openRouterClient.completeJsonObject(system, "Source:\n\n" + truncated);
        if (json.isPresent()) {
            try {
                JsonNode root = objectMapper.readTree(json.get());
                JsonNode items = root.path("items");
                if (items.isArray() && !items.isEmpty()) {
                    List<Map<String, Object>> out = new ArrayList<>();
                    for (JsonNode n : items) {
                        out.add(objectMapper.convertValue(n, new TypeReference<>() {}));
                    }
                    return out;
                }
            } catch (Exception e) {
                log.warn("Failed to parse extraction JSON from model: {}", e.toString());
            }
        }
        return stubItems();
    }

    private List<Map<String, Object>> stubItems() {
        return List.of(Map.of(
                "itemName", "Sample Item",
                "quantity", 10,
                "unit", "pcs",
                "unitPrice", "95.00",
                "baselinePrice", "100.00"
        ));
    }
}
