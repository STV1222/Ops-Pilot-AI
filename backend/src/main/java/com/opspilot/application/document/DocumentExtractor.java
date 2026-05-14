package com.opspilot.application.document;

public interface DocumentExtractor {
    boolean supports(String contentType);
    String extractText(byte[] content);
}
