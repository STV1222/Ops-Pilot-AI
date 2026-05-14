package com.opspilot.application.skill;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class SkillRegistry {
    private final Map<String, SkillDefinition> byName = new HashMap<>();

    public SkillRegistry(List<SkillLoader> loaders) {
        loaders.stream().flatMap(loader -> loader.load().stream()).forEach(def -> byName.put(def.skillName(), def));
    }

    public Optional<SkillDefinition> get(String skillName) {
        return Optional.ofNullable(byName.get(skillName));
    }
}
