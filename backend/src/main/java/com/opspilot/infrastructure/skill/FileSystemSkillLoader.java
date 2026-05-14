package com.opspilot.infrastructure.skill;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opspilot.application.skill.SkillDefinition;
import com.opspilot.application.skill.SkillLoader;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class FileSystemSkillLoader implements SkillLoader {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<SkillDefinition> load() {
        List<SkillDefinition> defs = new ArrayList<>();
        // Check relative path (local dev) then absolute Docker mount path
        Path skillsRoot = Files.exists(Path.of("..", "skills"))
                ? Path.of("..", "skills")
                : Path.of("/skills");
        if (!Files.exists(skillsRoot)) {
            return defs;
        }
        try (var stream = Files.list(skillsRoot)) {
            stream.filter(Files::isDirectory).forEach(dir -> {
                try {
                    File config = dir.resolve("config.json").toFile();
                    if (!config.exists()) {
                        return;
                    }
                    Map<String, Object> map = objectMapper.readValue(config, new TypeReference<>() {});
                    defs.add(new SkillDefinition(
                            UUID.randomUUID().toString(),
                            (String) map.getOrDefault("skillName", dir.getFileName().toString()),
                            (String) map.getOrDefault("version", "0.1.0"),
                            (String) map.getOrDefault("description", ""),
                            dir.toAbsolutePath().toString(),
                            map
                    ));
                } catch (Exception ignored) {
                }
            });
        } catch (Exception ignored) {
        }
        return defs;
    }
}
