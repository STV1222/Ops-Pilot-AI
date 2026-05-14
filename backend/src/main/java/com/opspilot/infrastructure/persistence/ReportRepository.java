package com.opspilot.infrastructure.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

@Repository
public class ReportRepository {

    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper = new ObjectMapper();

    public ReportRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void save(UUID executionId, String markdown, Map<String, Object> summary) {
        jdbc.update("""
                INSERT INTO generated_reports
                  (id, workflow_execution_id, markdown_content, json_summary, generated_at)
                VALUES (?, ?, ?, ?::jsonb, ?)
                """,
                UUID.randomUUID(), executionId, markdown, toJson(summary), Timestamp.from(Instant.now()));
    }

    public Optional<Map<String, Object>> findByExecutionId(UUID executionId) {
        List<Map<String, Object>> rows = jdbc.query(
                "SELECT markdown_content, json_summary FROM generated_reports WHERE workflow_execution_id = ? LIMIT 1",
                (rs, i) -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("markdown", rs.getString("markdown_content"));
                    row.put("summary", fromJson(rs.getString("json_summary")));
                    return row;
                }, executionId);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    private String toJson(Object obj) {
        try { return mapper.writeValueAsString(obj); } catch (Exception e) { return "{}"; }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fromJson(String json) {
        try { return mapper.readValue(json, Map.class); } catch (Exception e) { return Map.of(); }
    }
}
