package com.opspilot.infrastructure.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public class DocumentRepository {

    private final JdbcTemplate jdbc;

    public DocumentRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void save(UUID orgId, UUID executionId, String filename, String fileType, String extractedText) {
        jdbc.update("""
                INSERT INTO uploaded_documents
                  (id, organization_id, workflow_execution_id, filename, file_type, storage_path, extracted_text, uploaded_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                UUID.randomUUID(), orgId, executionId,
                filename, fileType, "memory://" + filename,
                extractedText, Timestamp.from(Instant.now()));
    }

    public List<String> findExtractedTextByExecutionId(UUID executionId) {
        return jdbc.queryForList(
                "SELECT extracted_text FROM uploaded_documents WHERE workflow_execution_id = ? AND extracted_text IS NOT NULL",
                String.class, executionId);
    }
}
