package com.leapfuture.trino.n8n;

import com.google.common.collect.ImmutableList;
import io.trino.spi.HostAddress;
import io.trino.spi.connector.ConnectorSplit;
import io.trino.spi.connector.ConnectorSplitSource;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * N8N Split Source - 生成N8N数据分片
 */
public class N8nSplitSource implements ConnectorSplitSource {
    
    private final N8nConfig config;
    private final N8nTableHandle tableHandle;
    private boolean finished = false;
    
    public N8nSplitSource(N8nConfig config, N8nTableHandle tableHandle) {
        this.config = config;
        this.tableHandle = tableHandle;
    }
    
    @Override
    public CompletableFuture<ConnectorSplitBatch> getNextBatch(int maxSize) {
        if (finished) {
            return CompletableFuture.completedFuture(new ConnectorSplitBatch(ImmutableList.of(), true));
        }
        
        finished = true;
        
        // 从表句柄创建split
        List<ConnectorSplit> splits = ImmutableList.of(
            N8nSplit.fromTableHandle(
                tableHandle,
                ImmutableList.of(HostAddress.fromString("localhost:5678"))
            )
        );
        
        return CompletableFuture.completedFuture(new ConnectorSplitBatch(splits, true));
    }
    
    @Override
    public void close() {
        // 清理资源
    }
    
    @Override
    public boolean isFinished() {
        return finished;
    }
} 