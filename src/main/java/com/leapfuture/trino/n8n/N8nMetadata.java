package com.leapfuture.trino.n8n;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import io.trino.spi.connector.ColumnHandle;
import io.trino.spi.connector.ColumnMetadata;
import io.trino.spi.connector.ConnectorMetadata;
import io.trino.spi.connector.ConnectorSession;
import io.trino.spi.connector.ConnectorTableHandle;
import io.trino.spi.connector.ConnectorTableMetadata;
import io.trino.spi.connector.SchemaTableName;
import io.trino.spi.connector.SchemaTablePrefix;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.trino.spi.type.VarcharType.VARCHAR;

/**
 * N8N Metadata - 动态发现N8N webhook作为表
 */
public class N8nMetadata implements ConnectorMetadata {
    
    public static final String SCHEMA_NAME = "default";
    
    private final N8nApiClient apiClient;
    
    @Inject
    public N8nMetadata(N8nApiClient apiClient) {
        this.apiClient = apiClient;
    }
    
    @Override
    public List<String> listSchemaNames(ConnectorSession session) {
        return ImmutableList.of(SCHEMA_NAME);
    }
    
    public List<SchemaTableName> listTables(ConnectorSession session, SchemaTablePrefix prefix) {
        if (prefix.getSchema().isPresent() && !prefix.getSchema().get().equals(SCHEMA_NAME)) {
            return ImmutableList.of();
        }
        
        try {
            // 通过API获取所有webhook信息
            List<N8nApiClient.WebhookInfo> webhooks = apiClient.getWebhookWorkflows();
            
            ImmutableList.Builder<SchemaTableName> tables = ImmutableList.builder();
            for (N8nApiClient.WebhookInfo webhook : webhooks) {
                // 只返回激活的webhook
                if (webhook.isActive()) {
                    tables.add(new SchemaTableName(SCHEMA_NAME, webhook.getTableName()));
                }
            }
            
            return tables.build();
            
        } catch (Exception e) {
            System.err.println("获取N8N表列表时出错: " + e.getMessage());
            // 出错时返回默认表
            return ImmutableList.of(new SchemaTableName(SCHEMA_NAME, "webhooks"));
        }
    }
    
    public ConnectorTableHandle getTableHandle(ConnectorSession session, SchemaTableName tableName) {
        if (!tableName.getSchemaName().equals(SCHEMA_NAME)) {
            return null;
        }
        
        try {
            // 查找对应的webhook信息
            List<N8nApiClient.WebhookInfo> webhooks = apiClient.getWebhookWorkflows();
            
            for (N8nApiClient.WebhookInfo webhook : webhooks) {
                if (webhook.getTableName().equals(tableName.getTableName()) && webhook.isActive()) {
                    return N8nTableHandle.fromWebhookInfo(webhook, SCHEMA_NAME);
                }
            }
            
            // 如果没找到，可能是默认表
            if ("webhooks".equals(tableName.getTableName())) {
                return new N8nTableHandle(tableName);
            }
            
        } catch (Exception e) {
            System.err.println("获取表句柄时出错: " + e.getMessage());
            // 出错时返回默认表句柄
            if ("webhooks".equals(tableName.getTableName())) {
                return new N8nTableHandle(tableName);
            }
        }
        
        return null;
    }
    
    @Override
    public ConnectorTableMetadata getTableMetadata(ConnectorSession session, ConnectorTableHandle table) {
        N8nTableHandle n8nTable = (N8nTableHandle) table;
        
        // 为所有webhook表返回统一的列结构
        // 实际数据结构会在运行时动态确定
        return new ConnectorTableMetadata(
            n8nTable.getSchemaTableName(),
            ImmutableList.of(
                new ColumnMetadata("webhook_path", VARCHAR),
                new ColumnMetadata("method", VARCHAR),
                new ColumnMetadata("workflow_name", VARCHAR),
                new ColumnMetadata("workflow_id", VARCHAR),
                new ColumnMetadata("is_active", VARCHAR),
                new ColumnMetadata("response_data", VARCHAR),
                new ColumnMetadata("status_code", VARCHAR),
                new ColumnMetadata("timestamp", VARCHAR)
            )
        );
    }
    
    @Override
    public Map<String, ColumnHandle> getColumnHandles(ConnectorSession session, ConnectorTableHandle tableHandle) {
        N8nTableHandle n8nTable = (N8nTableHandle) tableHandle;
        
        // 返回统一的列句柄
        return ImmutableMap.<String, ColumnHandle>builder()
            .put("webhook_path", new N8nColumnHandle("webhook_path", VARCHAR, 0))
            .put("method", new N8nColumnHandle("method", VARCHAR, 1))
            .put("workflow_name", new N8nColumnHandle("workflow_name", VARCHAR, 2))
            .put("workflow_id", new N8nColumnHandle("workflow_id", VARCHAR, 3))
            .put("is_active", new N8nColumnHandle("is_active", VARCHAR, 4))
            .put("response_data", new N8nColumnHandle("response_data", VARCHAR, 5))
            .put("status_code", new N8nColumnHandle("status_code", VARCHAR, 6))
            .put("timestamp", new N8nColumnHandle("timestamp", VARCHAR, 7))
            .build();
    }
    
    @Override
    public ColumnMetadata getColumnMetadata(ConnectorSession session, ConnectorTableHandle tableHandle, ColumnHandle columnHandle) {
        N8nColumnHandle n8nColumn = (N8nColumnHandle) columnHandle;
        return new ColumnMetadata(n8nColumn.getName(), n8nColumn.getType());
    }
} 