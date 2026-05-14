package com.opspilot.application.agent;

import java.util.List;
import java.util.Map;

public record AgentResult(
        boolean success,
        String markdown,
        Map<String, Object> summary,
        List<Map<String, Object>> anomalies,
        int iterations,
        String failureReason
) {}
