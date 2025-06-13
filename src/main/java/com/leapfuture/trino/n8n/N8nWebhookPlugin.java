package com.leapfuture.trino.n8n;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.trino.spi.Plugin;
import io.trino.spi.connector.ConnectorFactory;

import java.util.Set;

/**
 * N8N Plugin for Trino
 * 
 * This plugin provides both UDF functions and a connector to integrate with N8N webhooks.
 */
public class N8nWebhookPlugin implements Plugin {
    
    @Override
    public Set<Class<?>> getFunctions() {
        return ImmutableSet.<Class<?>>builder()
                .add(N8nWebhookFunctions.class)
                .build();
    }
    
    @Override
    public Iterable<ConnectorFactory> getConnectorFactories() {
        return ImmutableList.of(new N8nConnectorFactory());
    }
} 