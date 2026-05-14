package com.opspilot.infrastructure.document;

import com.opspilot.application.document.DocumentExtractor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Handles CSV, TSV, plain text, and octet-stream files that are text-based.
 * Registered at lowest priority so PDF and Excel extractors match first.
 */
@Component
@Order(100)
public class PlainTextExtractor implements DocumentExtractor {

    @Override
    public boolean supports(String contentType) {
        if (contentType == null) return true;
        return contentType.startsWith("text/")
                || contentType.contains("csv")
                || contentType.contains("octet-stream");
    }

    @Override
    public String extractText(byte[] content) {
        if (content == null || content.length == 0) return "";
        // Try UTF-8 first, fall back to ISO-8859-1
        try {
            String text = new String(content, StandardCharsets.UTF_8);
            // If it looks like binary (many replacement chars), fall back
            long badChars = text.chars().filter(c -> c == '�').count();
            if (badChars > text.length() * 0.05) {
                return new String(content, StandardCharsets.ISO_8859_1);
            }
            return text;
        } catch (Exception e) {
            return new String(content, StandardCharsets.ISO_8859_1);
        }
    }
}
