package com.leapfuture.trino.n8n;

import com.google.inject.Inject;

/**
 * N8N配置持有者
 * 为UDF函数提供配置访问
 */
public class N8nConfigHolder {
    
    private static volatile N8nConfig instance;
    
    @Inject
    public N8nConfigHolder(N8nConfig config) {
        N8nConfigHolder.instance = config;
    }
    
    /**
     * 获取配置实例
     * 
     * @return N8N配置
     */
    public static N8nConfig getInstance() {
        N8nConfig config = instance;
        if (config == null) {
            // 如果没有注入的配置，返回默认配置
            config = new N8nConfig();
        }
        return config;
    }
} 