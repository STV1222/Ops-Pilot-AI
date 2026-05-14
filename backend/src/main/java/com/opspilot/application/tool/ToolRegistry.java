package com.opspilot.application.tool;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ToolRegistry {
    private final Map<String, Tool> tools;

    public ToolRegistry(List<Tool> tools) {
        this.tools = tools.stream().collect(Collectors.toMap(Tool::name, Function.identity()));
    }

    public Tool get(String name) {
        return tools.get(name);
    }
}
