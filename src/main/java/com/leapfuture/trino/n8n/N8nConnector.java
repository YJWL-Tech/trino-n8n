package com.leapfuture.trino.n8n;

import com.google.inject.Inject;
import io.trino.spi.connector.Connector;
import io.trino.spi.connector.ConnectorMetadata;
import io.trino.spi.connector.ConnectorPageSourceProvider;
import io.trino.spi.connector.ConnectorSession;
import io.trino.spi.connector.ConnectorSplitManager;
import io.trino.spi.connector.ConnectorTransactionHandle;
import io.trino.spi.transaction.IsolationLevel;

import static java.util.Objects.requireNonNull;

/**
 * N8N Connector Implementation
 */
public class N8nConnector implements Connector {
    
    private final N8nMetadata metadata;
    private final N8nSplitManager splitManager;
    private final N8nPageSourceProvider pageSourceProvider;
    
    @Inject
    public N8nConnector(
            N8nMetadata metadata,
            N8nSplitManager splitManager,
            N8nPageSourceProvider pageSourceProvider) {
        this.metadata = requireNonNull(metadata, "metadata is null");
        this.splitManager = requireNonNull(splitManager, "splitManager is null");
        this.pageSourceProvider = requireNonNull(pageSourceProvider, "pageSourceProvider is null");
    }
    
    @Override
    public ConnectorTransactionHandle beginTransaction(IsolationLevel isolationLevel, boolean readOnly, boolean autoCommit) {
        return N8nTransactionHandle.INSTANCE;
    }
    
    @Override
    public ConnectorMetadata getMetadata(ConnectorSession session, ConnectorTransactionHandle transactionHandle) {
        return metadata;
    }
    
    @Override
    public ConnectorSplitManager getSplitManager() {
        return splitManager;
    }
    
    @Override
    public ConnectorPageSourceProvider getPageSourceProvider() {
        return pageSourceProvider;
    }
    
    @Override
    public void shutdown() {
        // 清理资源
    }
} 