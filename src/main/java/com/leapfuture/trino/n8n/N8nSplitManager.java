package com.leapfuture.trino.n8n;

import com.google.inject.Inject;
import io.trino.spi.connector.ConnectorSession;
import io.trino.spi.connector.ConnectorSplitManager;
import io.trino.spi.connector.ConnectorSplitSource;
import io.trino.spi.connector.ConnectorTableHandle;
import io.trino.spi.connector.ConnectorTransactionHandle;
import io.trino.spi.connector.Constraint;
import io.trino.spi.connector.DynamicFilter;

/**
 * N8N Split Manager
 */
public class N8nSplitManager implements ConnectorSplitManager {
    
    private final N8nConfig config;
    
    @Inject
    public N8nSplitManager(N8nConfig config) {
        this.config = config;
    }
    
    @Override
    public ConnectorSplitSource getSplits(
            ConnectorTransactionHandle transaction,
            ConnectorSession session,
            ConnectorTableHandle table,
            DynamicFilter dynamicFilter,
            Constraint constraint) {
        
        return new N8nSplitSource(config, (N8nTableHandle) table);
    }
} 