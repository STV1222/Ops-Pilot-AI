package com.opspilot.application.tool;

public interface Tool {
    String name();
    Object execute(ToolContext context);
}
