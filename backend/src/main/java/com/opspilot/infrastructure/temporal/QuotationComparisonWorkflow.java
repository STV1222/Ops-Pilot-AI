package com.opspilot.infrastructure.temporal;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

import java.util.UUID;

@WorkflowInterface
public interface QuotationComparisonWorkflow {
    @WorkflowMethod
    void run(UUID workflowExecutionId);
}
