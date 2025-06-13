package com.leapfuture.trino.n8n;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import io.airlift.slice.Slices;
import io.trino.spi.Page;
import io.trino.spi.PageBuilder;
import io.trino.spi.block.BlockBuilder;
import io.trino.spi.connector.ColumnHandle;
import io.trino.spi.connector.ConnectorPageSource;
import io.trino.spi.type.Type;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static io.trino.spi.type.VarcharType.VARCHAR;
import static java.util.concurrent.CompletableFuture.completedFuture;

/**
 * N8N Page Source - 从N8N webhook读取数据
 */
public class N8nPageSource implements ConnectorPageSource {
    
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    
    private final N8nConfig config;
    private final N8nSplit split;
    private final N8nTableHandle tableHandle;
    private final List<N8nColumnHandle> columnHandles;
    private final PageBuilder pageBuilder;
    
    private boolean finished = false;
    
    public N8nPageSource(
            N8nConfig config,
            N8nSplit split,
            N8nTableHandle tableHandle,
            List<ColumnHandle> columnHandles) {
        this.config = config;
        this.split = split;
        this.tableHandle = tableHandle;
        this.columnHandles = columnHandles.stream()
                .map(N8nColumnHandle.class::cast)
                .collect(ImmutableList.toImmutableList());
        
        // 创建PageBuilder
        List<Type> types = this.columnHandles.stream()
                .map(N8nColumnHandle::getType)
                .collect(ImmutableList.toImmutableList());
        this.pageBuilder = new PageBuilder(types);
    }
    
    @Override
    public long getCompletedBytes() {
        return 0;
    }
    
    @Override
    public long getReadTimeNanos() {
        return 0;
    }
    
    @Override
    public long getMemoryUsage() {
        return 0;
    }
    
    @Override
    public boolean isFinished() {
        return finished;
    }
    
    @Override
    @SuppressWarnings("deprecation")
    public Page getNextPage() {
        if (finished) {
            return null;
        }
        
        finished = true;
        
        try {
            // 调用实际的webhook获取数据
            String responseData = callWebhook();
            
            // 构建数据行
            buildRowFromWebhookResponse(responseData, "200");
            
            Page page = pageBuilder.build();
            pageBuilder.reset();
            
            return page;
            
        } catch (Exception e) {
            // 构建错误行
            buildRowFromWebhookResponse("{\"error\": \"" + e.getMessage() + "\"}", "500");
            Page page = pageBuilder.build();
            pageBuilder.reset();
            return page;
        }
    }
    
    @Override
    public CompletableFuture<?> isBlocked() {
        return completedFuture(null);
    }
    
    @Override
    public void close() {
        // 清理资源
    }
    
    /**
     * 调用webhook获取数据
     */
    private String callWebhook() throws Exception {
        CloseableHttpClient httpClient = N8nHttpClient.getClient(config);
        String fullUrl = config.buildWebhookUrl(split.getWebhookPath());
        
        if ("POST".equalsIgnoreCase(split.getMethod())) {
            HttpPost httpPost = new HttpPost(fullUrl);
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setHeader("Accept", "application/json");
            
            // POST请求可以发送空的JSON body来触发webhook
            StringEntity entity = new StringEntity("{}", ContentType.APPLICATION_JSON);
            httpPost.setEntity(entity);
            
            return httpClient.execute(httpPost, response -> {
                int statusCode = response.getCode();
                String responseBody = EntityUtils.toString(response.getEntity());
                
                if (statusCode >= 200 && statusCode < 300) {
                    return responseBody;
                } else {
                    throw new RuntimeException("HTTP " + statusCode + ": " + responseBody);
                }
            });
        } else {
            HttpGet httpGet = new HttpGet(fullUrl);
            httpGet.setHeader("Accept", "application/json");
            
            return httpClient.execute(httpGet, response -> {
                int statusCode = response.getCode();
                String responseBody = EntityUtils.toString(response.getEntity());
                
                if (statusCode >= 200 && statusCode < 300) {
                    return responseBody;
                } else {
                    throw new RuntimeException("HTTP " + statusCode + ": " + responseBody);
                }
            });
        }
    }
    
    /**
     * 从webhook响应构建数据行
     */
    private void buildRowFromWebhookResponse(String responseData, String statusCode) {
        pageBuilder.declarePosition();
        
        for (int i = 0; i < columnHandles.size(); i++) {
            N8nColumnHandle columnHandle = columnHandles.get(i);
            BlockBuilder blockBuilder = pageBuilder.getBlockBuilder(i);
            
            String columnName = columnHandle.getName();
            String value = getColumnValue(columnName, responseData, statusCode);
            
            if (value == null) {
                blockBuilder.appendNull();
            } else {
                VARCHAR.writeSlice(blockBuilder, Slices.utf8Slice(value));
            }
        }
    }
    
    /**
     * 获取列值
     */
    private String getColumnValue(String columnName, String responseData, String statusCode) {
        switch (columnName) {
            case "webhook_path":
                return split.getWebhookPath();
            case "method":
                return split.getMethod();
            case "workflow_name":
                return split.getWorkflowName();
            case "workflow_id":
                return split.getWorkflowId();
            case "is_active":
                return String.valueOf(split.isActive());
            case "response_data":
                return responseData;
            case "status_code":
                return statusCode;
            case "timestamp":
                return Instant.now().toString();
            default:
                return null;
        }
    }
} 