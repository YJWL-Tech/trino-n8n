package com.leapfuture.trino.n8n;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.trino.spi.connector.ConnectorTableHandle;
import io.trino.spi.connector.SchemaTableName;

import static java.util.Objects.requireNonNull;

/**
 * N8N Table Handle - 表示N8N中的一个webhook表
 */
public class N8nTableHandle implements ConnectorTableHandle {
    
    private final SchemaTableName schemaTableName;
    private final String webhookPath;
    private final String method;
    private final String workflowId;
    private final String workflowName;
    private final boolean isActive;
    
    @JsonCreator
    public N8nTableHandle(
            @JsonProperty("schemaTableName") SchemaTableName schemaTableName,
            @JsonProperty("webhookPath") String webhookPath,
            @JsonProperty("method") String method,
            @JsonProperty("workflowId") String workflowId,
            @JsonProperty("workflowName") String workflowName,
            @JsonProperty("isActive") boolean isActive) {
        this.schemaTableName = requireNonNull(schemaTableName, "schemaTableName is null");
        this.webhookPath = requireNonNull(webhookPath, "webhookPath is null");
        this.method = requireNonNull(method, "method is null");
        this.workflowId = workflowId;
        this.workflowName = workflowName;
        this.isActive = isActive;
    }
    
    /**
     * 从SchemaTableName创建（用于向后兼容）
     */
    public N8nTableHandle(SchemaTableName schemaTableName) {
        this(schemaTableName, "/webhook/default", "GET", "default", "Default Webhook", true);
    }
    
    /**
     * 从WebhookInfo创建
     */
    public static N8nTableHandle fromWebhookInfo(N8nApiClient.WebhookInfo webhookInfo, String schemaName) {
        SchemaTableName schemaTableName = new SchemaTableName(schemaName, webhookInfo.getTableName());
        return new N8nTableHandle(
            schemaTableName,
            webhookInfo.getWebhookPath(),
            webhookInfo.getMethod(),
            webhookInfo.getWorkflowId(),
            webhookInfo.getWorkflowName(),
            webhookInfo.isActive()
        );
    }
    
    @JsonProperty
    public SchemaTableName getSchemaTableName() {
        return schemaTableName;
    }
    
    @JsonProperty
    public String getWebhookPath() {
        return webhookPath;
    }
    
    @JsonProperty
    public String getMethod() {
        return method;
    }
    
    @JsonProperty
    public String getWorkflowId() {
        return workflowId;
    }
    
    @JsonProperty
    public String getWorkflowName() {
        return workflowName;
    }
    
    @JsonProperty
    public boolean isActive() {
        return isActive;
    }
    
    public String getSchemaName() {
        return schemaTableName.getSchemaName();
    }
    
    public String getTableName() {
        return schemaTableName.getTableName();
    }
    
    @Override
    public String toString() {
        return "N8nTableHandle{" +
                "schemaTableName=" + schemaTableName +
                ", webhookPath='" + webhookPath + '\'' +
                ", method='" + method + '\'' +
                ", workflowName='" + workflowName + '\'' +
                ", isActive=" + isActive +
                '}';
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        N8nTableHandle other = (N8nTableHandle) obj;
        return schemaTableName.equals(other.schemaTableName);
    }
    
    @Override
    public int hashCode() {
        return schemaTableName.hashCode();
    }
} 