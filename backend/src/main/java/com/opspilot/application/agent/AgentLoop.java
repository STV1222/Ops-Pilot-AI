package com.opspilot.application.agent;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opspilot.application.agent.tools.SubmitReportAgentTool;
import com.opspilot.infrastructure.ai.OpenRouterClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * ReAct-style agent loop.
 *
 * Each iteration:
 *   1. Send conversation history + tool definitions to the LLM
 *   2. LLM responds with either a tool_calls request or a final message
 *   3. If tool_calls: execute each tool, append results to history, repeat
 *   4. If submit_report tool called: extract result and terminate
 *   5. If finish_reason == "stop": LLM decided it's done
 *
 * The LLM autonomously decides which tools to call and in what order.
 */
@Component
public class AgentLoop {
    private static final Logger log = LoggerFactory.getLogger(AgentLoop.class);
    private static final int MAX_ITERATIONS = 12;

    private static final String SYSTEM_PROMPT = """
            You are an AI procurement analyst for an SME operations platform.
            Your job is to analyze supplier quotations and produce a structured recommendation report.

            You have access to the following tools. Use them in this exact order:
            1. extract_items — extract structured line items from the raw document text
            2. detect_anomalies — identify price deviations and missing data
            3. compare_quotations — AI-powered supplier ranking by total cost
            4. generate_report — produce the final markdown report; returns markdown, best_supplier, confidence, anomaly_count as flat fields
            5. submit_report — pass the markdown, best_supplier, confidence, anomaly_count from generate_report directly into this tool

            Rules:
            - Always call extract_items first with the full document text
            - When calling submit_report, use the exact field values returned by generate_report: markdown → markdown, best_supplier → best_supplier, confidence → confidence, anomaly_count → anomaly_count
            - Always call submit_report when analysis is complete — this is required to finish the workflow
            - Be thorough but efficient — do not repeat tool calls unnecessarily
            - If a tool returns an error, note it and continue with available data
            """;

    private final OpenRouterClient openRouterClient;
    private final Map<String, AgentTool> toolsByName;
    private final ObjectMapper mapper = new ObjectMapper();

    public AgentLoop(OpenRouterClient openRouterClient, List<AgentTool> tools) {
        this.openRouterClient = openRouterClient;
        this.toolsByName = new LinkedHashMap<>();
        tools.forEach(t -> toolsByName.put(t.name(), t));
        log.info("AgentLoop initialized with {} tools: {}", tools.size(), toolsByName.keySet());
    }

    public AgentResult run(String extractedText) {
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", SYSTEM_PROMPT));
        messages.add(Map.of("role", "user", "content",
                "Analyze the following supplier quotation documents and produce a procurement recommendation.\n\n" +
                "Document text:\n" + extractedText));

        List<Map<String, Object>> toolDefs = buildToolDefinitions();

        for (int iteration = 0; iteration < MAX_ITERATIONS; iteration++) {
            log.info("Agent iteration {}/{}", iteration + 1, MAX_ITERATIONS);

            JsonNode response = openRouterClient.completeWithTools(messages, toolDefs);
            if (response == null) {
                log.warn("Agent received null response from LLM on iteration {}", iteration + 1);
                return failure("LLM returned no response", iteration);
            }

            String finishReason = response.path("finish_reason").asText("");
            JsonNode message = response.path("message");

            // Append assistant message to history
            messages.add(buildAssistantMessage(message));

            if ("tool_calls".equals(finishReason) && message.has("tool_calls")) {
                JsonNode toolCalls = message.path("tool_calls");

                for (JsonNode toolCall : toolCalls) {
                    String toolCallId = toolCall.path("id").asText();
                    String toolName = toolCall.path("function").path("name").asText();
                    String argsJson = toolCall.path("function").path("arguments").asText("{}");

                    log.info("Agent calling tool '{}' (id={})", toolName, toolCallId);

                    Map<String, Object> args = parseArgs(argsJson);
                    AgentTool tool = toolsByName.get(toolName);

                    if (tool == null) {
                        log.warn("Agent called unknown tool '{}'", toolName);
                        messages.add(buildToolResult(toolCallId, Map.of("error", "Unknown tool: " + toolName)));
                        continue;
                    }

                    Map<String, Object> result = tool.execute(args);
                    log.info("Tool '{}' completed, result keys: {}", toolName, result.keySet());

                    // Terminal condition: agent submitted the report
                    if (SubmitReportAgentTool.NAME.equals(toolName)) {
                        log.info("Agent submitted report after {} iterations", iteration + 1);
                        return extractFinalResult(result, iteration + 1);
                    }

                    messages.add(buildToolResult(toolCallId, result));
                }

            } else if ("stop".equals(finishReason)) {
                // LLM finished without calling submit_report — extract from content if possible
                log.info("Agent stopped naturally after {} iterations", iteration + 1);
                String content = message.path("content").asText("");
                return AgentResult(true, content, Map.of(), List.of(), iteration + 1, null);
            } else {
                log.warn("Unexpected finish_reason '{}' on iteration {}", finishReason, iteration + 1);
                break;
            }
        }

        return failure("Agent exceeded maximum iterations (" + MAX_ITERATIONS + ")", MAX_ITERATIONS);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private List<Map<String, Object>> buildToolDefinitions() {
        return toolsByName.values().stream()
                .map(tool -> Map.<String, Object>of(
                        "type", "function",
                        "function", Map.of(
                                "name", tool.name(),
                                "description", tool.description(),
                                "parameters", tool.parametersSchema()
                        )
                ))
                .toList();
    }

    private Map<String, Object> buildAssistantMessage(JsonNode message) {
        Map<String, Object> msg = new HashMap<>();
        msg.put("role", "assistant");
        String content = message.path("content").asText(null);
        msg.put("content", content);
        if (message.has("tool_calls")) {
            try {
                msg.put("tool_calls", mapper.convertValue(message.path("tool_calls"),
                        new TypeReference<List<Object>>() {}));
            } catch (Exception ignored) {}
        }
        return msg;
    }

    private Map<String, Object> buildToolResult(String toolCallId, Map<String, Object> result) {
        try {
            return Map.of(
                    "role", "tool",
                    "tool_call_id", toolCallId,
                    "content", mapper.writeValueAsString(result)
            );
        } catch (Exception e) {
            return Map.of("role", "tool", "tool_call_id", toolCallId, "content", result.toString());
        }
    }

    private Map<String, Object> parseArgs(String json) {
        try {
            return mapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }

    @SuppressWarnings("unchecked")
    private AgentResult extractFinalResult(Map<String, Object> submitArgs, int iterations) {
        String markdown = (String) submitArgs.getOrDefault("markdown", "");
        String bestSupplier = (String) submitArgs.getOrDefault("best_supplier", "Unknown");
        double confidence = toDouble(submitArgs.getOrDefault("confidence", 0.8));
        if (confidence > 1.0) confidence = confidence / 100.0;
        int anomalyCount = toInt(submitArgs.getOrDefault("anomaly_count", 0));

        Map<String, Object> summary = Map.of(
                "bestSupplier", bestSupplier,
                "confidence", confidence,
                "anomalyCount", anomalyCount
        );
        return new AgentResult(true, markdown, summary, List.of(), iterations, null);
    }

    private AgentResult AgentResult(boolean success, String markdown, Map<String, Object> summary,
                                     List<Map<String, Object>> anomalies, int iterations, String failureReason) {
        return new AgentResult(success, markdown, summary, anomalies, iterations, failureReason);
    }

    private AgentResult failure(String reason, int iterations) {
        log.error("Agent failed: {}", reason);
        return new AgentResult(false, "", Map.of(), List.of(), iterations, reason);
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
