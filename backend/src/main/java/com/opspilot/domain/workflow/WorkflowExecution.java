package com.opspilot.domain.workflow;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record WorkflowExecution(
        UUID id,
        UUID organizationId,
        String workflowType,
        WorkflowStatus status,
        Instant startedAt,
        Instant completedAt,
        UUID triggeredBy,
        Map<String, Object> executionMetadata
) {}
