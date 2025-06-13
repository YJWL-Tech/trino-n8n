package com.leapfuture.trino.n8n;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.trino.spi.HostAddress;
import io.trino.spi.connector.ConnectorSplit;

import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * N8N Split - 表示N8N数据的一个分片
 */
public class N8nSplit implements ConnectorSplit {
    
    private final String webhookPath;
    private final String method;
    private final String workflowId;
    private final String workflowName;
    private final boolean isActive;
    private final List<HostAddress> addresses;
    
    @JsonCreator
    public N8nSplit(
            @JsonProperty("webhookPath") String webhookPath,
            @JsonProperty("method") String method,
            @JsonProperty("workflowId") String workflowId,
            @JsonProperty("workflowName") String workflowName,
            @JsonProperty("isActive") boolean isActive,
            @JsonProperty("addresses") List<HostAddress> addresses) {
        this.webhookPath = requireNonNull(webhookPath, "webhookPath is null");
        this.method = requireNonNull(method, "method is null");
        this.workflowId = workflowId;
        this.workflowName = workflowName;
        this.isActive = isActive;
        this.addresses = requireNonNull(addresses, "addresses is null");
    }
    
    /**
     * 从N8nTableHandle创建Split
     */
    public static N8nSplit fromTableHandle(N8nTableHandle tableHandle, List<HostAddress> addresses) {
        return new N8nSplit(
            tableHandle.getWebhookPath(),
            tableHandle.getMethod(),
            tableHandle.getWorkflowId(),
            tableHandle.getWorkflowName(),
            tableHandle.isActive(),
            addresses
        );
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
    
    @JsonProperty
    public List<HostAddress> getAddresses() {
        return addresses;
    }
    
    @Override
    public boolean isRemotelyAccessible() {
        return true;
    }
    
    public Object getInfo() {
        return this;
    }
    
    @Override
    public String toString() {
        return "N8nSplit{" +
                "webhookPath='" + webhookPath + '\'' +
                ", method='" + method + '\'' +
                ", workflowName='" + workflowName + '\'' +
                ", isActive=" + isActive +
                '}';
    }
} 