# Prometheus监控配置

## 1. application.yml配置

在 `application.yml` 中添加以下配置：

```yaml
# Actuator配置（监控端点）
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
      base-path: /actuator
  endpoint:
    health:
      show-details: always
    metrics:
      enabled: true
    prometheus:
      enabled: true
  metrics:
    export:
      prometheus:
        enabled: true
    tags:
      application: ${spring.application.name}
```

## 2. Prometheus服务器配置

创建 `prometheus.yml` 配置文件：

```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'library-backend'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['localhost:8080']
        labels:
          application: 'library-positioning-backend'
          environment: 'production'
```

## 3. 启动Prometheus

### Docker方式
```bash
docker run -d \
  --name prometheus \
  -p 9090:9090 \
  -v $(pwd)/prometheus.yml:/etc/prometheus/prometheus.yml \
  prom/prometheus
```

### 本地安装方式
```bash
# 下载Prometheus
wget https://github.com/prometheus/prometheus/releases/download/v2.45.0/prometheus-2.45.0.linux-amd64.tar.gz
tar xvfz prometheus-*.tar.gz
cd prometheus-*

# 复制配置文件
cp /path/to/prometheus.yml ./prometheus.yml

# 启动
./prometheus --config.file=prometheus.yml
```

访问: http://localhost:9090

## 4. 自定义业务指标

已在 `PrometheusConfig.java` 中定义了以下业务指标：

### 4.1 借阅操作指标
```java
customMetrics.recordBorrowOperation("apply", "success", 150);
```
- **指标名**: `library.borrow.operation`
- **标签**: operation (apply/return/renew), status (success/failure)
- **类型**: Timer（记录耗时）

### 4.2 登录尝试指标
```java
customMetrics.recordLoginAttempt("success");
```
- **指标名**: `library.login.attempts`
- **标签**: status (success/failure/locked)
- **类型**: Counter（计数器）

### 4.3 API调用指标
```java
customMetrics.recordApiCall("/api/books/search", "GET", 200);
```
- **指标名**: `library.api.calls`
- **标签**: endpoint, method, status
- **类型**: Counter

### 4.4 当前借阅数量
```java
customMetrics.recordCurrentBorrows(1250);
```
- **指标名**: `library.borrows.current`
- **类型**: Gauge（仪表盘）

### 4.5 库存告警
```java
customMetrics.recordInventoryAlert("123", 2);
```
- **指标名**: `library.inventory.alerts`
- **标签**: book_id, available
- **类型**: Counter

## 5. 常用查询语句（PromQL）

### 5.1 API请求速率
```promql
rate(library_api_calls_total[5m])
```

### 5.2 借阅操作成功率
```promql
sum(rate(library_borrow_operation_seconds_count{status="success"}[5m])) 
/ 
sum(rate(library_borrow_operation_seconds_count[5m]))
```

### 5.3 平均响应时间
```promql
rate(library_borrow_operation_seconds_sum[5m]) 
/ 
rate(library_borrow_operation_seconds_count[5m])
```

### 5.4 登录失败率
```promql
sum(rate(library_login_attempts_total{status="failure"}[5m])) 
/ 
sum(rate(library_login_attempts_total[5m]))
```

### 5.5 当前借阅数量
```promql
library_borrows_current
```

## 6. Grafana仪表盘配置

### 6.1 安装Grafana
```bash
docker run -d \
  --name grafana \
  -p 3000:3000 \
  grafana/grafana
```

访问: http://localhost:3000 (默认账号: admin/admin)

### 6.2 添加Prometheus数据源
1. 登录Grafana
2. Configuration → Data Sources → Add data source
3. 选择Prometheus
4. URL: http://localhost:9090
5. Save & Test

### 6.3 导入仪表盘模板

创建 `grafana-dashboard.json`（见下一个文件）

## 7. 监控指标说明

### 系统指标（自动采集）
- `jvm_memory_used_bytes` - JVM内存使用
- `jvm_threads_live_threads` - 活跃线程数
- `process_cpu_usage` - CPU使用率
- `http_server_requests_seconds` - HTTP请求耗时

### 业务指标（自定义）
- `library_borrow_operation_seconds` - 借阅操作耗时
- `library_login_attempts_total` - 登录尝试次数
- `library_api_calls_total` - API调用次数
- `library_borrows_current` - 当前借阅数量
- `library_inventory_alerts_total` - 库存告警次数

## 8. 验证监控是否正常

### 8.1 检查Actuator端点
```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8080/actuator/metrics
curl http://localhost:8080/actuator/prometheus
```

### 8.2 检查Prometheus是否采集数据
1. 访问 http://localhost:9090
2. Status → Targets
3. 确认 library-backend 状态为 UP

### 8.3 查询指标
在Prometheus界面执行查询：
```promql
library_api_calls_total
```

## 9. 下一步：配置告警规则
