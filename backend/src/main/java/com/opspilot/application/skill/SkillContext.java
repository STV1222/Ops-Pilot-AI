package com.opspilot.application.skill;

import java.util.Map;
import java.util.UUID;

public record SkillContext(
        UUID workflowExecutionId,
        UUID organizationId,
        Map<String, Object> input,
        Map<String, String> prompts
) {}
