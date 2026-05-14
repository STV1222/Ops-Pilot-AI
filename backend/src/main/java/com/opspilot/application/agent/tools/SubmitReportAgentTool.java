package com.opspilot.application.agent.tools;

import com.opspilot.application.agent.AgentTool;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Terminal tool — when the agent calls this, the loop ends and the report is submitted.
 * The AgentLoop detects this tool call and extracts the final result.
 */
@Component
public class SubmitReportAgentTool implements AgentTool {

    public static final String NAME = "submit_report";

    @Override
    public String name() { return NAME; }

    @Override
    public String description() {
        return "Submit the final procurement recommendation to the manager approval queue. Call this once you have completed all analysis steps and generated the report. This ends the workflow.";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "markdown", Map.of("type", "string", "description", "Full markdown report from generate_report"),
                        "best_supplier", Map.of("type", "string", "description", "Name of the recommended supplier"),
                        "confidence", Map.of("type", "number", "description", "Confidence score 0.0-1.0"),
                        "anomaly_count", Map.of("type", "integer", "description", "Number of anomalies detected")
                ),
                "required", List.of("markdown", "best_supplier", "confidence")
        );
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> args) {
        // Return the args directly — AgentLoop extracts from here
        return Map.copyOf(args);
    }
}
