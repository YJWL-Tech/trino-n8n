package com.leapfuture.trino.n8n;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.io.entity.EntityUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * N8N API客户端
 * 用于调用N8N管理API获取工作流和webhook信息
 */
public class N8nApiClient {
    
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    
    // 缓存工作流信息，避免频繁API调用
    private static final Map<String, CachedWorkflows> WORKFLOW_CACHE = new ConcurrentHashMap<>();
    
    private final N8nConfig config;
    private final CloseableHttpClient httpClient;
    
    @Inject
    public N8nApiClient(N8nConfig config) {
        this.config = config;
        this.httpClient = N8nHttpClient.getClient(config);
    }
    
    /**
     * 获取所有包含webhook的工作流信息
     * 
     * @return webhook信息列表
     */
    public List<WebhookInfo> getWebhookWorkflows() {
        if (!config.hasApiKey()) {
            // 如果没有API Key，返回默认的webhook表
            return getDefaultWebhooks();
        }
        
        try {
            // 检查缓存
            String cacheKey = config.getApiBaseUrl() + ":" + config.getApiKey();
            CachedWorkflows cached = WORKFLOW_CACHE.get(cacheKey);
            
            if (cached != null && !cached.isExpired(java.time.Duration.ofMillis(config.getCacheDuration().toMillis()))) {
                return cached.getWebhooks();
            }
            
            // 调用API获取工作流
            List<WebhookInfo> webhooks = fetchWorkflowsFromApi();
            
            // 更新缓存
            WORKFLOW_CACHE.put(cacheKey, new CachedWorkflows(webhooks, Instant.now()));
            
            return webhooks;
            
        } catch (Exception e) {
            System.err.println("警告: 无法获取N8N工作流信息: " + e.getMessage());
            // 出错时返回默认webhook
            return getDefaultWebhooks();
        }
    }
    
    /**
     * 从N8N API获取工作流信息
     */
    private List<WebhookInfo> fetchWorkflowsFromApi() throws Exception {
        String apiUrl = config.buildApiUrl("/workflows");
        
        HttpGet httpGet = new HttpGet(apiUrl);
        httpGet.setHeader("X-N8N-API-KEY", config.getApiKey());
        httpGet.setHeader("Accept", "application/json");
        
        try {
            return httpClient.execute(httpGet, response -> {
                int statusCode = response.getCode();
                String responseBody = EntityUtils.toString(response.getEntity());
                
                if (statusCode >= 200 && statusCode < 300) {
                    try {
                        return parseWorkflowsResponse(responseBody);
                    } catch (Exception e) {
                        throw new RuntimeException("解析响应失败: " + e.getMessage(), e);
                    }
                } else {
                    throw new RuntimeException("N8N API调用失败: HTTP " + statusCode + " - " + responseBody);
                }
            });
        } catch (Exception e) {
            throw new Exception("调用N8N API失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 解析工作流API响应，提取webhook信息
     */
    private List<WebhookInfo> parseWorkflowsResponse(String responseBody) throws Exception {
        List<WebhookInfo> webhooks = new ArrayList<>();
        
        JsonNode response = OBJECT_MAPPER.readTree(responseBody);
        JsonNode workflowsNode = response.get("data");
        
        if (workflowsNode != null && workflowsNode.isArray()) {
            for (JsonNode workflow : workflowsNode) {
                List<WebhookInfo> workflowWebhooks = extractWebhooksFromWorkflow(workflow);
                webhooks.addAll(workflowWebhooks);
            }
        }
        
        return webhooks;
    }
    
    /**
     * 从单个工作流中提取webhook信息
     */
    private List<WebhookInfo> extractWebhooksFromWorkflow(JsonNode workflow) {
        List<WebhookInfo> webhooks = new ArrayList<>();
        
        try {
            String workflowId = workflow.get("id").asText();
            String workflowName = workflow.get("name").asText();
            boolean isActive = workflow.get("active").asBoolean();
            
            // 解析工作流节点
            JsonNode nodesNode = workflow.get("nodes");
            if (nodesNode != null && nodesNode.isArray()) {
                for (JsonNode node : nodesNode) {
                    JsonNode typeNode = node.get("type");
                    if (typeNode != null && "n8n-nodes-base.webhook".equals(typeNode.asText())) {
                        // 找到webhook节点
                        WebhookInfo webhookInfo = extractWebhookInfo(node, workflowId, workflowName, isActive);
                        if (webhookInfo != null) {
                            webhooks.add(webhookInfo);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("解析工作流webhook信息时出错: " + e.getMessage());
        }
        
        return webhooks;
    }
    
    /**
     * 从webhook节点提取详细信息
     */
    private WebhookInfo extractWebhookInfo(JsonNode webhookNode, String workflowId, String workflowName, boolean isActive) {
        try {
            JsonNode parametersNode = webhookNode.get("parameters");
            if (parametersNode == null) {
                return null;
            }
            
            // 获取webhook路径
            JsonNode pathNode = parametersNode.get("path");
            String webhookPath = pathNode != null ? pathNode.asText() : "";
            
            // 获取HTTP方法
            JsonNode methodNode = parametersNode.get("httpMethod");
            String method = methodNode != null ? methodNode.asText() : "GET";
            
            // 生成表名（清理特殊字符）
            String tableName = generateTableName(workflowName, webhookPath);
            
            return new WebhookInfo(
                tableName,
                webhookPath,
                method,
                workflowId,
                workflowName,
                isActive
            );
            
        } catch (Exception e) {
            System.err.println("提取webhook信息时出错: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 生成表名
     */
    private String generateTableName(String workflowName, String webhookPath) {
        // 清理工作流名称，生成合法的表名
        String tableName = workflowName.toLowerCase()
                .replaceAll("[^a-z0-9_]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
        
        // 如果webhook路径有意义，也加入表名
        if (webhookPath != null && !webhookPath.trim().isEmpty() && !"/".equals(webhookPath.trim())) {
            String pathPart = webhookPath.toLowerCase()
                    .replaceAll("[^a-z0-9_]", "_")
                    .replaceAll("_+", "_")
                    .replaceAll("^_|_$", "");
            
            if (!pathPart.isEmpty() && !tableName.contains(pathPart)) {
                tableName = tableName + "_" + pathPart;
            }
        }
        
        // 确保表名不为空
        if (tableName.isEmpty()) {
            tableName = "webhook_table";
        }
        
        return tableName;
    }
    
    /**
     * 获取默认webhook列表（当没有API Key时）
     */
    private List<WebhookInfo> getDefaultWebhooks() {
        List<WebhookInfo> defaultWebhooks = new ArrayList<>();
        
        // 添加一些默认的webhook表
        defaultWebhooks.add(new WebhookInfo(
            "webhooks", 
            "/webhook/default", 
            "GET", 
            "default", 
            "Default Webhook", 
            true
        ));
        
        return defaultWebhooks;
    }
    
    /**
     * 清除缓存
     */
    public static void clearCache() {
        WORKFLOW_CACHE.clear();
    }
    
    /**
     * Webhook信息类
     */
    public static class WebhookInfo {
        private final String tableName;
        private final String webhookPath;
        private final String method;
        private final String workflowId;
        private final String workflowName;
        private final boolean isActive;
        
        public WebhookInfo(String tableName, String webhookPath, String method, 
                          String workflowId, String workflowName, boolean isActive) {
            this.tableName = tableName;
            this.webhookPath = webhookPath;
            this.method = method;
            this.workflowId = workflowId;
            this.workflowName = workflowName;
            this.isActive = isActive;
        }
        
        public String getTableName() { return tableName; }
        public String getWebhookPath() { return webhookPath; }
        public String getMethod() { return method; }
        public String getWorkflowId() { return workflowId; }
        public String getWorkflowName() { return workflowName; }
        public boolean isActive() { return isActive; }
        
        @Override
        public String toString() {
            return "WebhookInfo{" +
                    "tableName='" + tableName + '\'' +
                    ", webhookPath='" + webhookPath + '\'' +
                    ", method='" + method + '\'' +
                    ", workflowName='" + workflowName + '\'' +
                    ", isActive=" + isActive +
                    '}';
        }
    }
    
    /**
     * 缓存的工作流信息
     */
    private static class CachedWorkflows {
        private final List<WebhookInfo> webhooks;
        private final Instant cacheTime;
        
        public CachedWorkflows(List<WebhookInfo> webhooks, Instant cacheTime) {
            this.webhooks = webhooks;
            this.cacheTime = cacheTime;
        }
        
        public List<WebhookInfo> getWebhooks() {
            return webhooks;
        }
        
        public boolean isExpired(java.time.Duration cacheDuration) {
            return Instant.now().isAfter(cacheTime.plus(cacheDuration));
        }
    }
} 