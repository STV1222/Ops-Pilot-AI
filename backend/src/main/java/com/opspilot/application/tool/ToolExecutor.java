package com.opspilot.application.tool;

import org.springframework.stereotype.Service;

@Service
public class ToolExecutor {
    private final ToolRegistry toolRegistry;

    public ToolExecutor(ToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
    }

    public Object execute(String toolName, ToolContext context) {
        Tool tool = toolRegistry.get(toolName);
        if (tool == null) {
            throw new IllegalArgumentException("Unknown tool: " + toolName);
        }
        return tool.execute(context);
    }
}
