package com.leapfuture.trino.n8n;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.airlift.slice.Slice;
import io.airlift.slice.Slices;
import io.trino.spi.function.Description;
import io.trino.spi.function.ScalarFunction;
import io.trino.spi.function.SqlNullable;
import io.trino.spi.function.SqlType;
import io.trino.spi.type.StandardTypes;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;

/**
 * N8N Webhook UDF函数实现类
 * 包含调用N8N webhook的各种函数
 */
public class N8nWebhookFunctions {
    
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    
    /**
     * 调用N8N webhook (POST方法)
     * 
     * @param webhookPath N8N webhook的路径
     * @param jsonPayload 要发送的JSON数据
     * @return webhook的响应结果
     */
    @ScalarFunction("n8n_webhook_post")
    @Description("调用N8N webhook (POST方法)")
    @SqlType(StandardTypes.VARCHAR)
    public static Slice callN8nWebhookPost(
            @SqlNullable @SqlType(StandardTypes.VARCHAR) Slice webhookPath,
            @SqlNullable @SqlType(StandardTypes.VARCHAR) Slice jsonPayload) {
        
        if (webhookPath == null || jsonPayload == null) {
            return Slices.utf8Slice("{\"error\": \"Parameters cannot be null\"}");
        }
        
        N8nConfig config = N8nConfigHolder.getInstance();
        CloseableHttpClient httpClient = N8nHttpClient.getClient(config);
        String fullUrl = config.buildWebhookUrl(webhookPath.toStringUtf8());
        
        try {
            HttpPost httpPost = new HttpPost(fullUrl);
            
            // 设置请求头
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("User-Agent", "Trino-N8N-Connector/1.0");
            
            // 设置请求体
            StringEntity entity = new StringEntity(jsonPayload.toStringUtf8(), ContentType.APPLICATION_JSON);
            httpPost.setEntity(entity);
            
            // 执行请求并获取响应
            String result = httpClient.execute(httpPost, response -> {
                int statusCode = response.getCode();
                String responseBody = EntityUtils.toString(response.getEntity());
                
                // 构建详细的响应信息
                try {
                    // 尝试解析响应为JSON
                    JsonNode responseJson = OBJECT_MAPPER.readTree(responseBody);
                    return String.format("{\"status\": %d, \"url\": \"%s\", \"method\": \"POST\", \"payload\": %s, \"response\": %s}", 
                        statusCode, fullUrl, jsonPayload.toStringUtf8(), responseJson.toString());
                } catch (Exception e) {
                    // 如果响应不是JSON，直接返回原始响应
                    return String.format("{\"status\": %d, \"url\": \"%s\", \"method\": \"POST\", \"payload\": %s, \"response\": \"%s\"}", 
                        statusCode, fullUrl, jsonPayload.toStringUtf8(), responseBody.replace("\"", "\\\""));
                }
            });
            
            return Slices.utf8Slice(result);
            
        } catch (Exception e) {
            return Slices.utf8Slice(String.format("{\"error\": \"%s\", \"url\": \"%s\", \"payload\": %s}", 
                e.getMessage().replace("\"", "\\\""), fullUrl, jsonPayload.toStringUtf8()));
        }
    }
    
    /**
     * 调用N8N webhook (GET方法)
     * 
     * @param webhookPath N8N webhook的路径
     * @return webhook的响应结果
     */
    @ScalarFunction("n8n_webhook_get")
    @Description("调用N8N webhook (GET方法)")
    @SqlType(StandardTypes.VARCHAR)
    public static Slice callN8nWebhookGet(
            @SqlNullable @SqlType(StandardTypes.VARCHAR) Slice webhookPath) {
        
        if (webhookPath == null) {
            return Slices.utf8Slice("{\"error\": \"Webhook path cannot be null\"}");
        }
        
        N8nConfig config = N8nConfigHolder.getInstance();
        CloseableHttpClient httpClient = N8nHttpClient.getClient(config);
        String fullUrl = config.buildWebhookUrl(webhookPath.toStringUtf8());
        
        try {
            HttpGet httpGet = new HttpGet(fullUrl);
            
            // 设置请求头
            httpGet.setHeader("Accept", "application/json");
            
            // 执行请求并获取响应
            String result = httpClient.execute(httpGet, response -> {
                int statusCode = response.getCode();
                String responseBody = EntityUtils.toString(response.getEntity());
                
                // 返回格式化的响应
                return String.format("{\"status\": %d, \"response\": %s}", 
                    statusCode, responseBody);
            });
            
            return Slices.utf8Slice(result);
            
        } catch (Exception e) {
            return Slices.utf8Slice(String.format("{\"error\": \"%s\"}", e.getMessage()));
        }
    }
    
    /**
     * 调用N8N webhook并传递数据表字段
     * 
     * @param webhookPath N8N webhook的路径
     * @param fieldName 字段名称
     * @param fieldValue 字段值
     * @return webhook的响应结果
     */
    @ScalarFunction("n8n_webhook_send_field")
    @Description("调用N8N webhook并传递数据表字段")
    @SqlType(StandardTypes.VARCHAR)
    public static Slice callN8nWebhookSendField(
            @SqlNullable @SqlType(StandardTypes.VARCHAR) Slice webhookPath,
            @SqlNullable @SqlType(StandardTypes.VARCHAR) Slice fieldName,
            @SqlNullable @SqlType(StandardTypes.VARCHAR) Slice fieldValue) {
        
        if (webhookPath == null || fieldName == null || fieldValue == null) {
            return Slices.utf8Slice("{\"error\": \"Parameters cannot be null\"}");
        }
        
        try {
            // 构造JSON负载
            String jsonPayload = String.format("{\"%s\": \"%s\"}", 
                fieldName.toStringUtf8(), fieldValue.toStringUtf8());
            
            // 调用POST方法
            return callN8nWebhookPost(webhookPath, Slices.utf8Slice(jsonPayload));
            
        } catch (Exception e) {
            return Slices.utf8Slice(String.format("{\"error\": \"%s\"}", e.getMessage()));
        }
    }
    
    /**
     * 调用N8N webhook并传递多个字段
     * 
     * @param webhookPath N8N webhook的路径
     * @param jsonFields JSON格式的字段数据
     * @return webhook的响应结果
     */
    @ScalarFunction("n8n_webhook_send_json")
    @Description("调用N8N webhook并传递JSON格式的数据")
    @SqlType(StandardTypes.VARCHAR)
    public static Slice callN8nWebhookSendJson(
            @SqlNullable @SqlType(StandardTypes.VARCHAR) Slice webhookPath,
            @SqlNullable @SqlType(StandardTypes.VARCHAR) Slice jsonFields) {
        
        if (webhookPath == null || jsonFields == null) {
            return Slices.utf8Slice("{\"error\": \"Parameters cannot be null\"}");
        }
        
        try {
            // 验证JSON格式
            OBJECT_MAPPER.readTree(jsonFields.toStringUtf8());
            
            // 调用POST方法
            return callN8nWebhookPost(webhookPath, jsonFields);
            
        } catch (Exception e) {
            return Slices.utf8Slice(String.format("{\"error\": \"Invalid JSON format: %s\"}", e.getMessage()));
        }
    }
    
    /**
     * 调用N8N webhook并解析响应中的特定字段
     * 
     * @param webhookPath N8N webhook的路径
     * @param jsonPayload 要发送的JSON数据
     * @param responseField 要提取的响应字段
     * @return 提取的字段值
     */
    @ScalarFunction("n8n_webhook_extract_field")
    @Description("调用N8N webhook并提取响应中的特定字段")
    @SqlType(StandardTypes.VARCHAR)
    @SqlNullable
    public static Slice callN8nWebhookExtractField(
            @SqlNullable @SqlType(StandardTypes.VARCHAR) Slice webhookPath,
            @SqlNullable @SqlType(StandardTypes.VARCHAR) Slice jsonPayload,
            @SqlNullable @SqlType(StandardTypes.VARCHAR) Slice responseField) {
        
        if (webhookPath == null || jsonPayload == null || responseField == null) {
            return Slices.utf8Slice("Error: Parameters cannot be null");
        }
        
        try {
            // 调用webhook
            Slice response = callN8nWebhookPost(webhookPath, jsonPayload);
            
            // 解析响应
            JsonNode responseJson = OBJECT_MAPPER.readTree(response.toStringUtf8());
            JsonNode responseData = responseJson.get("response");
            
            if (responseData != null && responseData.has(responseField.toStringUtf8())) {
                return Slices.utf8Slice(responseData.get(responseField.toStringUtf8()).asText());
            } else {
                return null;
            }
            
        } catch (Exception e) {
            return Slices.utf8Slice(String.format("Error: %s", e.getMessage()));
        }
    }
    
    /**
     * 批量调用N8N webhook
     * 
     * @param webhookPath N8N webhook的路径
     * @param jsonArrayPayload JSON数组格式的数据
     * @return webhook的响应结果
     */
    @ScalarFunction("n8n_webhook_batch")
    @Description("批量调用N8N webhook")
    @SqlType(StandardTypes.VARCHAR)
    public static Slice callN8nWebhookBatch(
            @SqlNullable @SqlType(StandardTypes.VARCHAR) Slice webhookPath,
            @SqlNullable @SqlType(StandardTypes.VARCHAR) Slice jsonArrayPayload) {
        
        if (webhookPath == null || jsonArrayPayload == null) {
            return Slices.utf8Slice("{\"error\": \"Parameters cannot be null\"}");
        }
        
        try {
            // 验证JSON数组格式
            JsonNode jsonArray = OBJECT_MAPPER.readTree(jsonArrayPayload.toStringUtf8());
            if (!jsonArray.isArray()) {
                return Slices.utf8Slice("{\"error\": \"Payload must be a JSON array\"}");
            }
            
            // 调用POST方法
            return callN8nWebhookPost(webhookPath, jsonArrayPayload);
            
        } catch (Exception e) {
            return Slices.utf8Slice(String.format("{\"error\": \"Invalid JSON array format: %s\"}", e.getMessage()));
        }
    }
    
    /**
     * 调用N8N webhook (带认证)
     * 
     * @param webhookPath N8N webhook的路径
     * @param jsonPayload 要发送的JSON数据
     * @param authToken 认证令牌
     * @return webhook的响应结果
     */
    @ScalarFunction("n8n_webhook_auth")
    @Description("调用N8N webhook (带认证)")
    @SqlType(StandardTypes.VARCHAR)
    public static Slice callN8nWebhookAuth(
            @SqlNullable @SqlType(StandardTypes.VARCHAR) Slice webhookPath,
            @SqlNullable @SqlType(StandardTypes.VARCHAR) Slice jsonPayload,
            @SqlNullable @SqlType(StandardTypes.VARCHAR) Slice authToken) {
        
        if (webhookPath == null || jsonPayload == null || authToken == null) {
            return Slices.utf8Slice("{\"error\": \"Parameters cannot be null\"}");
        }
        
        N8nConfig config = N8nConfigHolder.getInstance();
        CloseableHttpClient httpClient = N8nHttpClient.getClient(config);
        String fullUrl = config.buildWebhookUrl(webhookPath.toStringUtf8());
        
        try {
            HttpPost httpPost = new HttpPost(fullUrl);
            
            // 设置请求头
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("Authorization", "Bearer " + authToken.toStringUtf8());
            
            // 设置请求体
            StringEntity entity = new StringEntity(jsonPayload.toStringUtf8(), ContentType.APPLICATION_JSON);
            httpPost.setEntity(entity);
            
            // 执行请求并获取响应
            String result = httpClient.execute(httpPost, response -> {
                int statusCode = response.getCode();
                String responseBody = EntityUtils.toString(response.getEntity());
                
                // 返回格式化的响应
                return String.format("{\"status\": %d, \"response\": %s}", 
                    statusCode, responseBody);
            });
            
            return Slices.utf8Slice(result);
            
        } catch (Exception e) {
            return Slices.utf8Slice(String.format("{\"error\": \"%s\"}", e.getMessage()));
        }
    }
    
    /**
     * 调试N8N webhook连接
     * 
     * @param webhookPath N8N webhook的路径
     * @return 详细的连接信息和配置
     */
    @ScalarFunction("n8n_webhook_debug")
    @Description("调试N8N webhook连接和配置")
    @SqlType(StandardTypes.VARCHAR)
    public static Slice debugN8nWebhook(
            @SqlNullable @SqlType(StandardTypes.VARCHAR) Slice webhookPath) {
        
        if (webhookPath == null) {
            return Slices.utf8Slice("{\"error\": \"Webhook path cannot be null\"}");
        }
        
        try {
            N8nConfig config = N8nConfigHolder.getInstance();
            String fullUrl = config.buildWebhookUrl(webhookPath.toStringUtf8());
            
            // 构建调试信息
            String debugInfo = String.format(
                "{" +
                "\"webhook_path\": \"%s\", " +
                "\"base_url\": \"%s\", " +
                "\"full_url\": \"%s\", " +
                "\"timeout\": \"%s\", " +
                "\"has_api_key\": %s, " +
                "\"config_info\": \"%s\"" +
                "}",
                webhookPath.toStringUtf8(),
                config.getBaseUrl(),
                fullUrl,
                config.getTimeout().toString(),
                config.hasApiKey(),
                config.toString().replace("\"", "\\\"")
            );
            
            return Slices.utf8Slice(debugInfo);
            
        } catch (Exception e) {
            return Slices.utf8Slice(String.format("{\"error\": \"Debug failed: %s\"}", e.getMessage()));
        }
    }
} 