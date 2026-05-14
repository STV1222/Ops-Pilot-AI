package com.opspilot.infrastructure.ai;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(OpenRouterProperties.class)
public class AiInfrastructureConfig {
}
