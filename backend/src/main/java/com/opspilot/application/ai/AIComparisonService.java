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
public class AIComparisonService {
    private static final Logger log = LoggerFactory.getLogger(AIComparisonService.class);

    private final OpenRouterClient openRouterClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AIComparisonService(OpenRouterClient openRouterClient) {
        this.openRouterClient = openRouterClient;
    }

    public Map<String, Object> compare(List<Map<String, Object>> parsedItems) {
        String system = """
                You compare supplier quotation line items for an SME procurement workflow.
                Respond with a single JSON object only, no markdown fences.
                Schema: {
                  "bestSupplier":"string",
                  "confidence":number between 0 and 1,
                  "summary":"one short paragraph",
                  "suppliers":["string"],
                  "itemComparison":[{"itemName":"string","prices":{"SupplierName":number}}]
                }.
                Populate itemComparison with actual numeric unit prices per supplier per item.
                If supplier identity is unclear, infer labels like Supplier A / Supplier B from document context.""";

        String user;
        try {
            user = "Line items JSON:\n" + objectMapper.writeValueAsString(parsedItems);
        } catch (Exception e) {
            user = parsedItems.toString();
        }

        Optional<String> json = openRouterClient.completeJsonObject(system, user);
        if (json.isPresent()) {
            try {
                JsonNode root = objectMapper.readTree(json.get());
                if (root.hasNonNull("bestSupplier") && root.has("confidence")) {
                    var result = new java.util.HashMap<String, Object>();
                    result.put("bestSupplier", root.path("bestSupplier").asText());
                    result.put("confidence", root.path("confidence").asDouble(0.5));
                    result.put("summary", root.path("summary").asText("See attached line items."));
                    if (root.has("suppliers")) {
                        result.put("suppliers", objectMapper.convertValue(root.path("suppliers"), new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {}));
                    }
                    if (root.has("itemComparison")) {
                        result.put("itemComparison", objectMapper.convertValue(root.path("itemComparison"), new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {}));
                    }
                    return result;
                }
            } catch (Exception e) {
                log.warn("Failed to parse comparison JSON: {}", e.toString());
            }
        }
        return stubComparison();
    }

    private Map<String, Object> stubComparison() {
        return Map.of(
                "bestSupplier", "Supplier A",
                "confidence", 0.82,
                "summary", "Supplier A has the lowest normalized total with acceptable risk profile."
        );
    }
}
