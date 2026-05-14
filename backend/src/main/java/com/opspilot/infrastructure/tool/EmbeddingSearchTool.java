package com.opspilot.infrastructure.tool;

import com.opspilot.application.tool.Tool;
import com.opspilot.application.tool.ToolContext;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class EmbeddingSearchTool implements Tool {
    @Override
    public String name() {
        return "embedding-search-tool";
    }

    @Override
    public Object execute(ToolContext context) {
        return Map.of("status", "ok", "message", "Embedding search tool placeholder");
    }
}
