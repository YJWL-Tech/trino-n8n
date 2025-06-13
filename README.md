# Trino N8N Webhook插件

这是一个用于Trino数据库的UDF插件，可以让你在SQL查询中直接调用N8N的webhook，实现数据处理流程的自动化集成。

## 配置说明

插件支持通过以下方式配置N8N服务器地址和其他参数：

### 1. 配置文件（推荐）

在Trino的`etc/catalog/`目录下创建`n8n.properties`配置文件：

```properties
# N8N服务器基础URL
n8n.base.url=https://your-n8n-server.com:5678

# HTTP请求超时时间（秒）
n8n.timeout.seconds=30

# HTTP连接池配置
n8n.max.connections=100
n8n.max.connections.per.route=20

# 重试配置
n8n.enable.retry=true
n8n.max.retries=3
```

### 2. 系统属性

在Trino启动时设置JVM系统属性：
```bash
-Dn8n.base.url=https://your-n8n-server.com
```

### 3. 环境变量

设置环境变量：
```bash
export N8N_BASE_URL=https://your-n8n-server.com
```

### 4. 默认值

如果未配置，将使用默认值：`http://localhost:5678`

**配置优先级**：配置文件 > 系统属性 > 环境变量 > 默认值

## 功能特性

- 🚀 **简单易用**：在SQL中直接调用N8N webhook
- 🔄 **多种HTTP方法**：支持GET、POST请求
- 📊 **灵活的数据传输**：支持单字段、JSON、批量数据传输
- 🔐 **安全认证**：支持Bearer Token认证
- ⚡ **高性能**：内置连接池和重试机制
- 🛡️ **错误处理**：完善的异常处理和错误信息返回

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

## 安装部署

### 1. 编译插件

```bash
mvn clean package
```

### 2. 配置N8N插件

选择以下任一方式配置N8N插件：

**方式1：使用配置文件（推荐）**
```bash
# 复制示例配置文件到Trino catalog目录
cp examples/n8n.properties /path/to/trino/etc/catalog/

# 编辑配置文件
vim /path/to/trino/etc/catalog/n8n.properties
```

**方式2：设置系统属性**
在Trino的`etc/jvm.config`文件中添加：
```
-Dn8n.base.url=https://your-n8n-server.com:5678
```

**方式3：设置环境变量**
```bash
export N8N_BASE_URL=https://your-n8n-server.com:5678
```

### 3. 部署到Trino

将生成的JAR文件复制到Trino的插件目录：

```bash
cp target/trino-n8n-plugin-1.0-SNAPSHOT.jar /path/to/trino/plugin/n8n-webhook/
```

### 4. 重启Trino服务

```bash
sudo systemctl restart trino
```

### 5. 验证安装

在Trino CLI中执行：

```sql
SHOW FUNCTIONS LIKE 'n8n_%';
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
- **重试机制**：自动重试失败的请求
- **超时控制**：可配置的请求超时时间
- **并发控制**：支持大并发量的webhook调用

## 安全建议

1. **使用HTTPS**：确保webhook URL使用HTTPS协议
2. **认证机制**：对敏感操作使用`n8n_webhook_auth`函数
3. **网络隔离**：将Trino和N8N部署在安全的网络环境中
4. **数据脱敏**：避免在webhook中传输敏感数据

## 故障排除

### 常见问题

**1. 函数未找到**
```
解决方案：检查插件是否正确安装，重启Trino服务
```

**2. 连接超时**
```
解决方案：检查网络连接，增加超时时间配置
```

**3. JSON格式错误**
```
解决方案：验证JSON格式的正确性，使用JSON_FORMAT函数
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