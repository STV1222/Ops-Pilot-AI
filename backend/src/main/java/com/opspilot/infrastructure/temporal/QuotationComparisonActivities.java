package com.opspilot.infrastructure.temporal;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface QuotationComparisonActivities {
    @ActivityMethod
    void runSkill(String executionId);

    @ActivityMethod
    void runAgent(String executionId);
}
