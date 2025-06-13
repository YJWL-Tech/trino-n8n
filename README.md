# Trino N8N Webhook插件

这是一个用于Trino数据库的N8N连接器插件，可以让你在SQL查询中直接调用N8N的webhook，并且支持动态发现N8N工作流作为表进行查询。

## 配置说明

插件通过Trino的catalog配置文件进行配置。在Trino的`etc/catalog/`目录下创建`n8n.properties`配置文件：

### 基础配置

```properties
# 必需配置 - Connector名称
connector.name=n8n

# N8N服务器基础URL（用于webhook调用）
# 默认值: http://localhost:5678
n8n.base-url=http://localhost:5678

# N8N管理API基础URL（用于获取工作流信息）
# 默认值: http://localhost:5678/api/v1
n8n.api-base-url=http://localhost:5678/api/v1

# N8N API密钥（可选，用于动态发现webhook）
# 如果不配置，将使用默认的webhook表
# 获取方式: N8N界面 -> Settings -> API Keys -> Create API Key
# n8n.api-key=your-api-key-here

# HTTP请求超时时间
# 格式: 数字+单位 (s=秒, m=分钟, h=小时)
# 范围: 1s - 10m，默认值: 30s
n8n.timeout=30s

# Webhook列表缓存时间
# 避免频繁调用N8N API，提高性能
# 格式: 数字+单位 (s=秒, m=分钟, h=小时)
# 范围: 30s - 1h，默认值: 5m
n8n.cache-duration=5m
```

### 环境配置示例

**开发环境配置：**
```properties
connector.name=n8n
n8n.base-url=http://localhost:5678
n8n.api-base-url=http://localhost:5678/api/v1
n8n.timeout=10s
n8n.cache-duration=1m
```

**生产环境配置：**
```properties
connector.name=n8n
n8n.base-url=https://n8n.company.com
n8n.api-base-url=https://n8n.company.com/api/v1
n8n.api-key=your-production-api-key
n8n.timeout=60s
n8n.cache-duration=10m
```

### 配置说明

- **connector.name**: 必须设置为 `n8n`
- **n8n.base-url**: N8N服务器的基础URL，用于webhook调用
- **n8n.api-base-url**: N8N管理API的基础URL，用于获取工作流信息
- **n8n.api-key**: N8N API密钥，配置后可以动态发现所有webhook作为表
- **n8n.timeout**: HTTP请求超时时间，支持时间单位后缀
- **n8n.cache-duration**: API响应缓存时间，减少对N8N API的频繁调用

## 功能特性

- 🚀 **简单易用**：在SQL中直接调用N8N webhook
- 📊 **动态表发现**：自动发现N8N工作流作为可查询的表
- 🔄 **多种HTTP方法**：支持GET、POST请求
- 📈 **灵活的数据传输**：支持单字段、JSON、批量数据传输
- 🔐 **安全认证**：支持Bearer Token认证
- ⚡ **高性能**：内置连接池和缓存机制
- 🛡️ **错误处理**：完善的异常处理和错误信息返回
- 🔍 **调试支持**：提供调试函数帮助排查问题

## 两种使用模式

### 1. 基础模式（无API Key）
- 提供默认的webhook表
- 可以使用UDF函数调用webhook
- 适合简单的webhook调用场景

### 2. 高级模式（有API Key）
- 自动发现所有激活的webhook作为表
- 支持SQL查询webhook信息
- 动态表结构
- 适合复杂的工作流管理场景

## 支持的UDF函数

### 1. `n8n_webhook_post(webhook_path, json_payload)`
通过POST方法调用N8N webhook

```sql
SELECT n8n_webhook_post(
    '/webhook/your-webhook-id',
    '{"user": "张三", "action": "login", "timestamp": "2024-01-01T10:00:00Z"}'
);
```

### 2. `n8n_webhook_get(webhook_path)`
通过GET方法调用N8N webhook

```sql
SELECT n8n_webhook_get('/webhook/status');
```

### 3. `n8n_webhook_send_field(webhook_path, field_name, field_value)`
发送单个字段数据到N8N webhook

```sql
SELECT n8n_webhook_send_field(
    '/webhook/user-update',
    'user_id',
    '12345'
);
```

### 4. `n8n_webhook_send_json(webhook_path, json_data)`
发送JSON格式数据到N8N webhook

```sql
SELECT n8n_webhook_send_json(
    '/webhook/process-data',
    '{"name": "李四", "department": "技术部", "salary": 15000}'
);
```

### 5. `n8n_webhook_batch(webhook_path, json_array)`
批量发送数组数据到N8N webhook

```sql
SELECT n8n_webhook_batch(
    '/webhook/batch-process',
    '[{"id": 1, "name": "项目A"}, {"id": 2, "name": "项目B"}]'
);
```

### 6. `n8n_webhook_auth(webhook_path, json_payload, auth_token)`
使用认证令牌调用N8N webhook

```sql
SELECT n8n_webhook_auth(
    '/webhook/secure-endpoint',
    '{"sensitive_data": "重要信息"}',
    'your-bearer-token'
);
```

### 7. `n8n_webhook_extract_field(webhook_path, json_payload, response_field)`
调用webhook并提取响应中的特定字段

```sql
SELECT n8n_webhook_extract_field(
    '/webhook/get-status',
    '{"query": "user_status"}',
    'status'
);
```

### 8. `n8n_webhook_debug(webhook_path)` 🆕
调试N8N webhook连接和配置

```sql
SELECT n8n_webhook_debug('/webhook/deploysysmodel');
```

## 表查询功能

### 发现所有webhook表
```sql
SHOW TABLES FROM n8n.default;
```

### 查询特定webhook数据
```sql
SELECT * FROM n8n.default.user_registration_webhook;
```

### 查看webhook元信息
```sql
SELECT webhook_path, method, workflow_name, is_active 
FROM n8n.default.order_processing_webhook;
```

### 表结构说明
每个webhook表包含以下列：
- `webhook_path`: webhook路径
- `method`: HTTP方法 (GET/POST)
- `workflow_name`: 工作流名称
- `workflow_id`: 工作流ID
- `is_active`: 是否激活
- `response_data`: webhook响应数据 (JSON格式)
- `status_code`: HTTP状态码
- `timestamp`: 调用时间戳

## 安装部署

### 1. 编译插件

```bash
mvn clean package
```

### 2. 部署到Trino

将生成的JAR文件复制到Trino的插件目录：

```bash
# 创建插件目录
mkdir -p /path/to/trino/plugin/n8n

# 复制插件JAR文件
cp target/trino-n8n-plugin-1.0-SNAPSHOT.jar /path/to/trino/plugin/n8n/
```

### 3. 配置N8N连接器

在Trino的`etc/catalog/`目录下创建`n8n.properties`配置文件：

```bash
# 复制示例配置文件
cp examples/n8n-simple.properties /path/to/trino/etc/catalog/n8n.properties

# 编辑配置文件
vim /path/to/trino/etc/catalog/n8n.properties
```

**最小配置示例：**
```properties
connector.name=n8n
n8n.base-url=http://localhost:5678
```

**完整配置示例：**
```properties
connector.name=n8n
n8n.base-url=https://n8n.company.com
n8n.api-base-url=https://n8n.company.com/api/v1
n8n.api-key=your-api-key-here
n8n.timeout=30s
n8n.cache-duration=5m
```

### 4. 重启Trino服务

```bash
sudo systemctl restart trino
```

### 5. 验证安装

在Trino CLI中执行：

```sql
-- 查看可用的UDF函数
SHOW FUNCTIONS LIKE 'n8n_%';

-- 查看N8N catalog
SHOW SCHEMAS FROM n8n;

-- 测试调试函数
SELECT n8n_webhook_debug('/webhook/test');
```

## 实际使用示例

### 场景1：用户行为数据推送到N8N

```sql
-- 将用户登录数据推送到N8N进行分析处理
SELECT 
    user_id,
    n8n_webhook_send_json(
        '/webhook/user-analytics',
        JSON_FORMAT(JSON_OBJECT(
            'user_id', user_id,
            'login_time', login_time,
            'ip_address', ip_address,
            'device_type', device_type
        ))
    ) as webhook_response
FROM user_login_logs
WHERE login_time >= CURRENT_DATE;
```

### 场景2：数据质量检查触发通知

```sql
-- 检查数据质量，异常时触发N8N通知流程
SELECT 
    table_name,
    error_count,
    CASE 
        WHEN error_count > 100 THEN 
            n8n_webhook_post(
                '/webhook/data-quality-alert',
                JSON_FORMAT(JSON_OBJECT(
                    'table', table_name,
                    'error_count', error_count,
                    'severity', 'HIGH',
                    'timestamp', CURRENT_TIMESTAMP
                ))
            )
        ELSE 'OK'
    END as notification_status
FROM data_quality_report
WHERE check_date = CURRENT_DATE;
```

### 场景3：批量数据同步

```sql
-- 批量同步产品数据到N8N进行处理
SELECT n8n_webhook_batch(
    '/webhook/product-sync',
    JSON_FORMAT(
        JSON_ARRAYAGG(
            JSON_OBJECT(
                'product_id', product_id,
                'name', product_name,
                'price', price,
                'category', category,
                'updated_at', updated_at
            )
        )
    )
) as sync_result
FROM products
WHERE updated_at >= CURRENT_DATE - INTERVAL '1' DAY;
```

## 错误处理

所有函数都会返回JSON格式的响应，包含状态码和结果数据：

**成功响应示例：**
```json
{
  "status": 200,
  "response": {
    "message": "Data processed successfully",
    "id": "workflow-123"
  }
}
```

**错误响应示例：**
```json
{
  "error": "Connection timeout"
}
```

## 性能优化

- **连接池**：内置HTTP连接池，支持连接复用
- **缓存机制**：API响应缓存，减少重复请求
- **超时控制**：可配置的请求超时时间
- **并发控制**：支持大并发量的webhook调用

## 安全建议

1. **使用HTTPS**：确保webhook URL使用HTTPS协议
2. **认证机制**：对敏感操作使用`n8n_webhook_auth`函数
3. **网络隔离**：将Trino和N8N部署在安全的网络环境中
4. **数据脱敏**：避免在webhook中传输敏感数据

## 故障排除

### 调试步骤

**1. 检查配置**
```sql
-- 使用调试函数检查配置
SELECT n8n_webhook_debug('/webhook/test');
```

**2. 测试连接**
```sql
-- 测试简单的webhook调用
SELECT n8n_webhook_get('/webhook/test');
```

**3. 查看详细响应**
```sql
-- 新版本的POST函数会返回详细的调试信息
SELECT n8n_webhook_post('/webhook/test', '{"test": "data"}');
```

### 常见问题

**1. 函数未找到**
```
错误: Function n8n_webhook_post not registered
解决方案: 
- 检查插件是否正确安装到 /path/to/trino/plugin/n8n/ 目录
- 检查配置文件 n8n.properties 是否存在
- 重启Trino服务
```

**2. 连接超时**
```
错误: {"error": "Connection timeout"}
解决方案:
- 检查N8N服务器是否运行正常
- 检查网络连接和防火墙设置
- 增加超时时间: n8n.timeout=60s
- 验证URL格式是否正确
```

**3. Workflow could not be started**
```
错误: {"status": 500, "response": {"message": "Workflow could not be started!"}}
解决方案:
- 检查N8N工作流是否已激活
- 验证webhook路径是否正确
- 检查工作流中的webhook节点配置
- 确认webhook节点的HTTP方法设置
```

**4. JSON格式错误**
```
错误: {"error": "Invalid JSON format"}
解决方案:
- 验证JSON格式的正确性
- 使用JSON_FORMAT函数格式化JSON
- 检查特殊字符是否正确转义
```

**5. Duration配置错误**
```
错误: Invalid value '300' for type Duration
解决方案:
- 使用正确的时间格式: 30s, 5m, 1h
- 检查配置文件中的时间单位
```

### 响应格式说明

**成功响应示例：**
```json
{
  "status": 200,
  "url": "http://localhost:5678/webhook/test",
  "method": "POST",
  "payload": {"test": "data"},
  "response": {
    "message": "Data processed successfully",
    "id": "workflow-123"
  }
}
```

**错误响应示例：**
```json
{
  "error": "Connection timeout",
  "url": "http://localhost:5678/webhook/test",
  "payload": {"test": "data"}
}
```

### 调试配置示例

**开发环境调试配置：**
```properties
connector.name=n8n
n8n.base-url=http://localhost:5678
n8n.timeout=10s
n8n.cache-duration=30s
```

## 开发和测试

### 运行测试

```bash
mvn test
```

### 调试模式

在开发环境中，可以启用详细的日志输出来调试webhook调用。

## 许可证

本项目使用MIT许可证。

## 贡献

欢迎提交Issue和Pull Request来改进这个插件。

## 联系方式

如有问题或建议，请通过GitHub Issues联系我们。 