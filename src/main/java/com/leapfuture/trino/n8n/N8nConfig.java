package com.leapfuture.trino.n8n;

import io.airlift.configuration.Config;
import io.airlift.configuration.ConfigDescription;
import io.airlift.configuration.ConfigSecuritySensitive;
import io.airlift.units.Duration;
import io.airlift.units.MaxDuration;
import io.airlift.units.MinDuration;

import javax.validation.constraints.NotNull;
import java.util.concurrent.TimeUnit;

/**
 * N8N Connector配置类
 * 支持webhook调用和管理API调用
 */
public class N8nConfig {
    
    private String baseUrl = "http://localhost:5678";
    private String apiBaseUrl = "http://localhost:5678/api/v1";
    private String apiKey = "";
    private Duration timeout = Duration.succinctDuration(30, TimeUnit.SECONDS);
    private Duration cacheDuration = Duration.succinctDuration(5, TimeUnit.MINUTES);
    
    /**
     * 获取N8N服务器基础URL（用于webhook调用）
     * 
     * @return 基础URL
     */
    @NotNull
    public String getBaseUrl() {
        return baseUrl;
    }
    
    @Config("n8n.base-url")
    @ConfigDescription("N8N server base URL for webhook calls")
    public N8nConfig setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
        return this;
    }
    
    /**
     * 获取N8N API基础URL（用于管理API调用）
     * 
     * @return API基础URL
     */
    @NotNull
    public String getApiBaseUrl() {
        return apiBaseUrl;
    }
    
    @Config("n8n.api-base-url")
    @ConfigDescription("N8N API base URL for management API calls")
    public N8nConfig setApiBaseUrl(String apiBaseUrl) {
        this.apiBaseUrl = apiBaseUrl;
        return this;
    }
    
    /**
     * 获取N8N API Key
     * 
     * @return API Key
     */
    public String getApiKey() {
        return apiKey;
    }
    
    @Config("n8n.api-key")
    @ConfigDescription("N8N API key for accessing management API")
    @ConfigSecuritySensitive
    public N8nConfig setApiKey(String apiKey) {
        this.apiKey = apiKey;
        return this;
    }
    
    /**
     * 获取HTTP请求超时时间
     * 
     * @return 超时时间
     */
    @NotNull
    @MaxDuration("10m")
    @MinDuration("1s")
    public Duration getTimeout() {
        return timeout;
    }
    
    @Config("n8n.timeout")
    @ConfigDescription("HTTP request timeout")
    public N8nConfig setTimeout(Duration timeout) {
        this.timeout = timeout;
        return this;
    }
    
    /**
     * 获取缓存持续时间
     * 
     * @return 缓存持续时间
     */
    @NotNull
    @MaxDuration("1h")
    @MinDuration("30s")
    public Duration getCacheDuration() {
        return cacheDuration;
    }
    
    @Config("n8n.cache-duration")
    @ConfigDescription("Cache duration for API responses")
    public N8nConfig setCacheDuration(Duration cacheDuration) {
        this.cacheDuration = cacheDuration;
        return this;
    }
    
    /**
     * 检查是否配置了API Key
     * 
     * @return 是否有API Key
     */
    public boolean hasApiKey() {
        return apiKey != null && !apiKey.trim().isEmpty();
    }
    
    /**
     * 构建完整的webhook URL
     * 
     * @param webhookPath webhook路径 (例如: "/webhook/test" 或 "webhook/test")
     * @return 完整的webhook URL
     */
    public String buildWebhookUrl(String webhookPath) {
        String cleanBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String cleanPath = webhookPath.startsWith("/") ? webhookPath : "/" + webhookPath;
        return cleanBaseUrl + cleanPath;
    }
    
    /**
     * 构建完整的API URL
     * 
     * @param apiPath API路径 (例如: "/workflows" 或 "workflows")
     * @return 完整的API URL
     */
    public String buildApiUrl(String apiPath) {
        String cleanApiBaseUrl = apiBaseUrl.endsWith("/") ? apiBaseUrl.substring(0, apiBaseUrl.length() - 1) : apiBaseUrl;
        String cleanPath = apiPath.startsWith("/") ? apiPath : "/" + apiPath;
        return cleanApiBaseUrl + cleanPath;
    }
    
    @Override
    public String toString() {
        return "N8nConfig{" +
                "baseUrl='" + baseUrl + '\'' +
                ", apiBaseUrl='" + apiBaseUrl + '\'' +
                ", hasApiKey=" + hasApiKey() +
                ", timeout=" + timeout +
                ", cacheDuration=" + cacheDuration +
                '}';
    }
} 