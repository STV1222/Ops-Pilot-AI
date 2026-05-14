package com.opspilot.infrastructure.document;

import com.opspilot.application.document.DocumentExtractor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

@Component
public class PdfExtractor implements DocumentExtractor {
    @Override
    public boolean supports(String contentType) {
        return contentType != null && contentType.contains("pdf");
    }

    @Override
    public String extractText(byte[] content) {
        try (var doc = Loader.loadPDF(content)) {
            return new PDFTextStripper().getText(doc);
        } catch (Exception e) {
            return "";
        }
    }
}
