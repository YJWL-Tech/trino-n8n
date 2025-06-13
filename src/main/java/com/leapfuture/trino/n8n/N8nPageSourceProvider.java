package com.leapfuture.trino.n8n;

import com.google.inject.Inject;
import io.trino.spi.connector.ColumnHandle;
import io.trino.spi.connector.ConnectorPageSource;
import io.trino.spi.connector.ConnectorPageSourceProvider;
import io.trino.spi.connector.ConnectorSession;
import io.trino.spi.connector.ConnectorSplit;
import io.trino.spi.connector.ConnectorTableHandle;
import io.trino.spi.connector.ConnectorTransactionHandle;
import io.trino.spi.connector.DynamicFilter;

import java.util.List;

/**
 * N8N Page Source Provider
 */
public class N8nPageSourceProvider implements ConnectorPageSourceProvider {
    
    private final N8nConfig config;
    
    @Inject
    public N8nPageSourceProvider(N8nConfig config) {
        this.config = config;
    }
    
    @Override
    public ConnectorPageSource createPageSource(
            ConnectorTransactionHandle transaction,
            ConnectorSession session,
            ConnectorSplit split,
            ConnectorTableHandle table,
            List<ColumnHandle> columns,
            DynamicFilter dynamicFilter) {
        
        return new N8nPageSource(config, (N8nSplit) split, (N8nTableHandle) table, columns);
    }
} 