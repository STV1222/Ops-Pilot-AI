package com.opspilot.application.skill;

public interface SkillExecutor {
    SkillResult execute(SkillDefinition skillDefinition, SkillContext context);
}
