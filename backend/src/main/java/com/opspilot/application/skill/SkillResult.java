package com.opspilot.application.skill;

import java.util.Map;

public record SkillResult(
        boolean success,
        Map<String, Object> output,
        String message
) {}
