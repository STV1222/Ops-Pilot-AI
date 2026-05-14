package com.opspilot.infrastructure.tool;

import com.opspilot.application.tool.Tool;
import com.opspilot.application.tool.ToolContext;
import com.opspilot.infrastructure.document.PdfExtractor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class PdfTool implements Tool {

    private final PdfExtractor extractor;

    public PdfTool(PdfExtractor extractor) {
        this.extractor = extractor;
    }

    @Override
    public String name() {
        return "pdf-tool";
    }

    @Override
    public Object execute(ToolContext context) {
        byte[] content = (byte[]) context.input().get("content");
        if (content == null) return Map.of("text", "");
        String text = extractor.extractText(content);
        return Map.of("text", text);
    }
}
