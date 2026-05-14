package com.opspilot.infrastructure.document;

import com.opspilot.application.document.DocumentExtractor;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;

@Component
public class ExcelExtractor implements DocumentExtractor {
    @Override
    public boolean supports(String contentType) {
        return contentType != null && (contentType.contains("spreadsheet") || contentType.contains("excel") || contentType.contains("sheet"));
    }

    @Override
    public String extractText(byte[] content) {
        StringBuilder sb = new StringBuilder();
        try (var workbook = WorkbookFactory.create(new java.io.ByteArrayInputStream(content))) {
            workbook.forEach(sheet -> sheet.forEach(row -> row.forEach(cell -> sb.append(cell.toString()).append(" | "))));
        } catch (Exception ignored) {
        }
        return sb.toString();
    }
}
