package com.opspilot.infrastructure.temporal;

import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.UUID;

public class QuotationComparisonWorkflowImpl implements QuotationComparisonWorkflow {

    private static final Logger log = Workflow.getLogger(QuotationComparisonWorkflowImpl.class);

    private final QuotationComparisonActivities activities = Workflow.newActivityStub(
            QuotationComparisonActivities.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofMinutes(5))
                    .build());

    @Override
    public void run(UUID executionId) {
        log.info("Workflow started for execution {}", executionId);
        activities.runSkill(executionId.toString());
        log.info("Workflow completed for execution {}", executionId);
    }
}
