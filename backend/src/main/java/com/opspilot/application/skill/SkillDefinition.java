package com.opspilot.application.skill;

import java.util.Map;

public record SkillDefinition(
        String id,
        String skillName,
        String version,
        String description,
        String skillPath,
        Map<String, Object> metadata
) {}
