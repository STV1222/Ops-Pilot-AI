package com.opspilot.application.agent;

import java.util.Map;

public interface AgentTool {
    String name();
    String description();
    Map<String, Object> parametersSchema();
    Map<String, Object> execute(Map<String, Object> args);
}
