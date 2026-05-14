package com.opspilot.infrastructure.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opspilot.domain.workflow.WorkflowExecution;
import com.opspilot.domain.workflow.WorkflowStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

@Repository
public class WorkflowExecutionRepository {

    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper = new ObjectMapper();

    public WorkflowExecutionRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void save(WorkflowExecution exec) {
        jdbc.update("""
                INSERT INTO workflow_executions
                  (id, organization_id, workflow_type, status, started_at, completed_at, triggered_by, logs, execution_metadata)
                VALUES (?, ?, ?, ?, ?, ?, ?, '[]'::jsonb, ?::jsonb)
                ON CONFLICT (id) DO NOTHING
                """,
                exec.id(), exec.organizationId(), exec.workflowType(), exec.status().name(),
                Timestamp.from(exec.startedAt()),
                exec.completedAt() != null ? Timestamp.from(exec.completedAt()) : null,
                exec.triggeredBy(),
                toJson(exec.executionMetadata()));
    }

    public void updateStatus(UUID id, WorkflowStatus status, Instant completedAt) {
        jdbc.update("UPDATE workflow_executions SET status = ?, completed_at = ? WHERE id = ?",
                status.name(),
                completedAt != null ? Timestamp.from(completedAt) : null,
                id);
    }

    public Optional<WorkflowExecution> findById(UUID id) {
        List<WorkflowExecution> rows = jdbc.query(
                "SELECT * FROM workflow_executions WHERE id = ?",
                (rs, i) -> map(rs), id);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    public List<WorkflowExecution> findAll() {
        return jdbc.query(
                "SELECT * FROM workflow_executions ORDER BY started_at DESC",
                (rs, i) -> map(rs));
    }

    private WorkflowExecution map(java.sql.ResultSet rs) throws java.sql.SQLException {
        Timestamp completedAt = rs.getTimestamp("completed_at");
        return new WorkflowExecution(
                UUID.fromString(rs.getString("id")),
                UUID.fromString(rs.getString("organization_id")),
                rs.getString("workflow_type"),
                WorkflowStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("started_at").toInstant(),
                completedAt != null ? completedAt.toInstant() : null,
                UUID.fromString(rs.getString("triggered_by")),
                fromJson(rs.getString("execution_metadata")));
    }

    private String toJson(Object obj) {
        try { return mapper.writeValueAsString(obj); } catch (Exception e) { return "{}"; }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fromJson(String json) {
        try { return mapper.readValue(json, Map.class); } catch (Exception e) { return Map.of(); }
    }
}
