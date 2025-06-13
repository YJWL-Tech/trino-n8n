package com.leapfuture.trino.n8n;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.core5.util.Timeout;

import com.google.inject.Inject;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * N8N HTTP客户端管理器
 * 简化版本，提供基本的HTTP客户端功能
 */
public class N8nHttpClient {
    
    private static final ConcurrentMap<String, CloseableHttpClient> CLIENT_CACHE = new ConcurrentHashMap<>();
    
    private final N8nConfig config;
    
    @Inject
    public N8nHttpClient(N8nConfig config) {
        this.config = config;
    }
    
    /**
     * 获取HTTP客户端实例
     * 
     * @return HTTP客户端
     */
    public CloseableHttpClient getClient() {
        return getClient(this.config);
    }
    
    /**
     * 获取HTTP客户端实例
     * 
     * @param config N8N配置
     * @return HTTP客户端
     */
    public static CloseableHttpClient getClient(N8nConfig config) {
        String configKey = generateConfigKey(config);
        
        return CLIENT_CACHE.computeIfAbsent(configKey, key -> createHttpClient(config));
    }
    
    /**
     * 创建HTTP客户端
     * 
     * @param config N8N配置
     * @return HTTP客户端
     */
    private static CloseableHttpClient createHttpClient(N8nConfig config) {
        // 请求配置
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.ofMilliseconds(config.getTimeout().toMillis()))
                .setResponseTimeout(Timeout.ofMilliseconds(config.getTimeout().toMillis()))
                .build();
        
        // 构建HTTP客户端
        return HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .build();
    }
    
    /**
     * 生成配置key
     * 
     * @param config N8N配置
     * @return 配置key
     */
    private static String generateConfigKey(N8nConfig config) {
        return String.format("baseUrl:%s-timeout:%d",
                config.getBaseUrl(),
                config.getTimeout().toMillis() / 1000);
    }
    
    /**
     * 关闭所有HTTP客户端
     */
    public static void closeAllClients() {
        CLIENT_CACHE.values().forEach(client -> {
            try {
                client.close();
            } catch (Exception e) {
                // 忽略关闭异常
            }
        });
        CLIENT_CACHE.clear();
    }
} 