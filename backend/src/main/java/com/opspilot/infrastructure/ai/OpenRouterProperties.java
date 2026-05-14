package com.opspilot.infrastructure.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai.openrouter")
public class OpenRouterProperties {
    /**
     * OpenRouter OpenAI-compatible base URL (no trailing slash).
     */
    private String baseUrl = "https://openrouter.ai/api/v1";
    private String apiKey = "";
    /** OpenRouter model id, e.g. openai/gpt-4o-mini, google/gemini-2.0-flash-001 */
    private String model = "openai/gpt-4o-mini";
    /** Optional; used by OpenRouter for attribution */
    private String httpReferer = "http://localhost:8080";
    private String appName = "OpsPilot AI";

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getHttpReferer() {
        return httpReferer;
    }

    public void setHttpReferer(String httpReferer) {
        this.httpReferer = httpReferer;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank() && !"replace-me".equalsIgnoreCase(apiKey.trim());
    }
}
