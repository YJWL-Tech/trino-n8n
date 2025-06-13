package com.leapfuture.trino.n8n;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Scopes;

import static io.airlift.configuration.ConfigBinder.configBinder;

/**
 * N8N Connector Guice模块
 * 负责绑定配置和依赖注入
 */
public class N8nModule implements Module {
    
    @Override
    public void configure(Binder binder) {
        // 绑定配置类
        configBinder(binder).bindConfig(N8nConfig.class);
        
        // 绑定其他服务类为单例
        binder.bind(N8nConnector.class).in(Scopes.SINGLETON);
        binder.bind(N8nMetadata.class).in(Scopes.SINGLETON);
        binder.bind(N8nSplitManager.class).in(Scopes.SINGLETON);
        binder.bind(N8nPageSourceProvider.class).in(Scopes.SINGLETON);
        binder.bind(N8nApiClient.class).in(Scopes.SINGLETON);
        binder.bind(N8nHttpClient.class).in(Scopes.SINGLETON);
        binder.bind(N8nConfigHolder.class).in(Scopes.SINGLETON);
    }
}
