package com.opspilot.application.tool;

import java.util.Map;
import java.util.UUID;

public record ToolContext(UUID organizationId, Map<String, Object> input) {}
