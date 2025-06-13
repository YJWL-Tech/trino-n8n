package com.leapfuture.trino.n8n;

import com.google.inject.Injector;
import io.airlift.bootstrap.Bootstrap;
import io.trino.spi.connector.Connector;
import io.trino.spi.connector.ConnectorContext;
import io.trino.spi.connector.ConnectorFactory;

import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * N8N Connector Factory
 */
public class N8nConnectorFactory implements ConnectorFactory {
    
    @Override
    public String getName() {
        return "n8n";
    }
    
    @Override
    public Connector create(String catalogName, Map<String, String> config, ConnectorContext context) {
        requireNonNull(catalogName, "catalogName is null");
        requireNonNull(config, "config is null");
        
        // 使用Bootstrap和Injector来初始化配置和依赖注入
        Bootstrap app = new Bootstrap(new N8nModule());
        Injector injector = app
                .doNotInitializeLogging()
                .setRequiredConfigurationProperties(config)
                .initialize();
        
        return injector.getInstance(N8nConnector.class);
    }
} 