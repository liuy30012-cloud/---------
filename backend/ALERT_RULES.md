# Prometheus告警规则配置

## 1. 告警规则文件 (alert_rules.yml)

创建 `alert_rules.yml` 文件：

```yaml
groups:
  - name: library_backend_alerts
    interval: 30s
    rules:
      # 1. 服务可用性告警
      - alert: ServiceDown
        expr: up{job="library-backend"} == 0
        for: 1m
        labels:
          severity: critical
          category: availability
        annotations:
          summary: "图书馆后端服务宕机"
          description: "服务 {{ $labels.instance }} 已宕机超过1分钟"

      # 2. 高错误率告警
      - alert: HighErrorRate
        expr: |
          sum(rate(library_api_calls_total{status=~"5.."}[5m])) 
          / 
          sum(rate(library_api_calls_total[5m])) > 0.05
        for: 5m
        labels:
          severity: warning
          category: error_rate
        annotations:
          summary: "API错误率过高"
          description: "5xx错误率超过5%，当前值: {{ $value | humanizePercentage }}"

      # 3. 响应时间过长告警
      - alert: HighResponseTime
        expr: |
          histogram_quantile(0.95, 
            rate(library_borrow_operation_seconds_bucket[5m])
          ) > 2
        for: 5m
        labels:
          severity: warning
          category: performance
        annotations:
          summary: "借阅操作响应时间过长"
          description: "P95响应时间超过2秒，当前值: {{ $value }}s"

      # 4. 登录失败率过高告警
      - alert: HighLoginFailureRate
        expr: |
          sum(rate(library_login_attempts_total{status="failure"}[5m])) 
          / 
          sum(rate(library_login_attempts_total[5m])) > 0.3
        for: 5m
        labels:
          severity: warning
          category: security
        annotations:
          summary: "登录失败率异常"
          description: "登录失败率超过30%，可能存在暴力破解攻击"

      # 5. JVM内存使用率告警
      - alert: HighMemoryUsage
        expr: |
          (jvm_memory_used_bytes{area="heap"} 
          / 
          jvm_memory_max_bytes{area="heap"}) > 0.85
        for: 5m
        labels:
          severity: warning
          category: resource
        annotations:
          summary: "JVM堆内存使用率过高"
          description: "堆内存使用率超过85%，当前值: {{ $value | humanizePercentage }}"

      # 6. CPU使用率告警
      - alert: HighCPUUsage
        expr: process_cpu_usage > 0.8
        for: 5m
        labels:
          severity: warning
          category: resource
        annotations:
          summary: "CPU使用率过高"
          description: "CPU使用率超过80%，当前值: {{ $value | humanizePercentage }}"

      # 7. 数据库连接池告警
      - alert: DatabaseConnectionPoolExhausted
        expr: |
          hikaricp_connections_active 
          / 
          hikaricp_connections_max > 0.9
        for: 2m
        labels:
          severity: critical
          category: database
        annotations:
          summary: "数据库连接池即将耗尽"
          description: "连接池使用率超过90%，当前值: {{ $value | humanizePercentage }}"

      # 8. 库存告警频率过高
      - alert: HighInventoryAlertRate
        expr: rate(library_inventory_alerts_total[5m]) > 10
        for: 5m
        labels:
          severity: info
          category: business
        annotations:
          summary: "库存告警频率过高"
          description: "每分钟库存告警超过10次，可能需要补充热门书籍"

      # 9. 借阅操作失败率告警
      - alert: HighBorrowFailureRate
        expr: |
          sum(rate(library_borrow_operation_seconds_count{status="failure"}[5m])) 
          / 
          sum(rate(library_borrow_operation_seconds_count[5m])) > 0.1
        for: 5m
        labels:
          severity: warning
          category: business
        annotations:
          summary: "借阅操作失败率过高"
          description: "借阅失败率超过10%，当前值: {{ $value | humanizePercentage }}"

      # 10. 线程死锁检测
      - alert: ThreadDeadlock
        expr: jvm_threads_deadlocked_threads > 0
        for: 1m
        labels:
          severity: critical
          category: jvm
        annotations:
          summary: "检测到线程死锁"
          description: "发现 {{ $value }} 个死锁线程"

      # 11. GC时间过长告警
      - alert: HighGCTime
        expr: |
          rate(jvm_gc_pause_seconds_sum[5m]) 
          / 
          rate(jvm_gc_pause_seconds_count[5m]) > 0.5
        for: 5m
        labels:
          severity: warning
          category: jvm
        annotations:
          summary: "GC平均时间过长"
          description: "GC平均耗时超过500ms，当前值: {{ $value }}s"

      # 12. 请求速率异常（可能是DDoS攻击）
      - alert: AbnormalRequestRate
        expr: |
          sum(rate(library_api_calls_total[1m])) > 1000
        for: 2m
        labels:
          severity: critical
          category: security
        annotations:
          summary: "请求速率异常"
          description: "每秒请求数超过1000，可能遭受DDoS攻击"

      # 13. 磁盘空间不足告警
      - alert: LowDiskSpace
        expr: |
          (node_filesystem_avail_bytes{mountpoint="/"} 
          / 
          node_filesystem_size_bytes{mountpoint="/"}) < 0.1
        for: 5m
        labels:
          severity: warning
          category: resource
        annotations:
          summary: "磁盘空间不足"
          description: "可用磁盘空间低于10%"

      # 14. 数据库查询慢告警
      - alert: SlowDatabaseQuery
        expr: |
          histogram_quantile(0.95, 
            rate(hikaricp_connections_usage_seconds_bucket[5m])
          ) > 1
        for: 5m
        labels:
          severity: warning
          category: database
        annotations:
          summary: "数据库查询响应慢"
          description: "P95数据库查询时间超过1秒"
```

## 2. 更新Prometheus配置

修改 `prometheus.yml`，添加告警规则：

```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

# 告警规则文件
rule_files:
  - "alert_rules.yml"

# Alertmanager配置
alerting:
  alertmanagers:
    - static_configs:
        - targets: ['localhost:9093']

scrape_configs:
  - job_name: 'library-backend'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['localhost:8080']
        labels:
          application: 'library-positioning-backend'
          environment: 'production'
```

## 3. Alertmanager配置

创建 `alertmanager.yml`：

```yaml
global:
  resolve_timeout: 5m
  smtp_smarthost: 'smtp.example.com:587'
  smtp_from: 'alertmanager@example.com'
  smtp_auth_username: 'alertmanager@example.com'
  smtp_auth_password: 'your-password'

# 告警路由
route:
  group_by: ['alertname', 'severity']
  group_wait: 10s
  group_interval: 10s
  repeat_interval: 12h
  receiver: 'default'
  
  routes:
    # 严重告警立即发送
    - match:
        severity: critical
      receiver: 'critical-alerts'
      continue: true
    
    # 警告级别告警
    - match:
        severity: warning
      receiver: 'warning-alerts'
    
    # 信息级别告警
    - match:
        severity: info
      receiver: 'info-alerts'

# 接收器配置
receivers:
  - name: 'default'
    webhook_configs:
      - url: 'http://localhost:5001/webhook'
  
  - name: 'critical-alerts'
    email_configs:
      - to: 'admin@example.com'
        headers:
          Subject: '[CRITICAL] 图书馆系统告警'
    webhook_configs:
      - url: 'http://localhost:5001/webhook/critical'
  
  - name: 'warning-alerts'
    email_configs:
      - to: 'ops@example.com'
        headers:
          Subject: '[WARNING] 图书馆系统告警'
  
  - name: 'info-alerts'
    webhook_configs:
      - url: 'http://localhost:5001/webhook/info'

# 告警抑制规则
inhibit_rules:
  # 如果服务宕机，抑制其他所有告警
  - source_match:
      alertname: 'ServiceDown'
    target_match_re:
      alertname: '.*'
    equal: ['instance']
```

## 4. 启动Alertmanager

### Docker方式
```bash
docker run -d \
  --name alertmanager \
  -p 9093:9093 \
  -v $(pwd)/alertmanager.yml:/etc/alertmanager/alertmanager.yml \
  prom/alertmanager
```

### 本地安装方式
```bash
wget https://github.com/prometheus/alertmanager/releases/download/v0.26.0/alertmanager-0.26.0.linux-amd64.tar.gz
tar xvfz alertmanager-*.tar.gz
cd alertmanager-*
./alertmanager --config.file=alertmanager.yml
```

访问: http://localhost:9093

## 5. 告警级别说明

| 级别 | 说明 | 响应时间 | 通知方式 |
|------|------|----------|----------|
| **critical** | 严重故障，需立即处理 | 立即 | 邮件 + 短信 + 电话 |
| **warning** | 警告，需要关注 | 15分钟内 | 邮件 + 企业微信 |
| **info** | 信息提示 | 1小时内 | 邮件 |

## 6. 告警测试

### 6.1 手动触发告警
```bash
# 停止服务触发ServiceDown告警
systemctl stop library-backend

# 等待1分钟后检查Alertmanager
curl http://localhost:9093/api/v2/alerts
```

### 6.2 模拟高负载
```bash
# 使用ab工具模拟高并发
ab -n 10000 -c 100 http://localhost:8080/api/books/search
```

## 7. 告警静默（Silence）

在维护期间临时关闭告警：

```bash
# 创建静默规则（静默1小时）
curl -X POST http://localhost:9093/api/v2/silences \
  -H "Content-Type: application/json" \
  -d '{
    "matchers": [
      {
        "name": "alertname",
        "value": "ServiceDown",
        "isRegex": false
      }
    ],
    "startsAt": "2026-04-05T10:00:00Z",
    "endsAt": "2026-04-05T11:00:00Z",
    "createdBy": "admin",
    "comment": "系统维护"
  }'
```

## 8. 告警通知集成

### 8.1 企业微信机器人
```yaml
receivers:
  - name: 'wechat-alerts'
    wechat_configs:
      - corp_id: 'your-corp-id'
        api_secret: 'your-api-secret'
        to_user: '@all'
        agent_id: 'your-agent-id'
```

### 8.2 钉钉机器人
```yaml
receivers:
  - name: 'dingtalk-alerts'
    webhook_configs:
      - url: 'https://oapi.dingtalk.com/robot/send?access_token=your-token'
```

### 8.3 Slack
```yaml
receivers:
  - name: 'slack-alerts'
    slack_configs:
      - api_url: 'https://hooks.slack.com/services/YOUR/WEBHOOK/URL'
        channel: '#alerts'
```

## 9. 下一步：准备应急预案
