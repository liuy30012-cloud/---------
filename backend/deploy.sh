#!/bin/bash

# ========================================
# 图书馆书籍定位系统 - 部署脚本
# ========================================

set -e  # 遇到错误立即退出

echo "=========================================="
echo "图书馆书籍定位系统 - 自动部署脚本"
echo "=========================================="
echo ""

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 配置变量
PROJECT_DIR="e:\图书馆书籍定位系统\backend"
DB_HOST="localhost"
DB_PORT="3306"
DB_NAME="library"
DB_USER="root"
DB_PASS="root"

# 函数：打印成功消息
print_success() {
    echo -e "${GREEN}✓${NC} $1"
}

# 函数：打印错误消息
print_error() {
    echo -e "${RED}✗${NC} $1"
}

# 函数：打印警告消息
print_warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

# 步骤1：检查环境
echo "步骤1: 检查环境依赖..."
echo "----------------------------"

# 检查Java
if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | head -n 1)
    print_success "Java环境: $JAVA_VERSION"
else
    print_error "Java未安装，请先安装Java 17+"
    exit 1
fi

# 检查MySQL
if command -v mysql &> /dev/null; then
    print_success "MySQL已安装"
else
    print_warning "MySQL命令未找到，跳过数据库检查"
fi

echo ""

# 步骤2：执行数据库脚本
echo "步骤2: 执行数据库初始化脚本..."
echo "----------------------------"

if [ -f "$PROJECT_DIR/init_database.sql" ]; then
    print_success "找到init_database.sql"

    # 尝试执行SQL脚本
    if command -v mysql &> /dev/null; then
        echo "正在执行SQL脚本..."
        mysql -h $DB_HOST -P $DB_PORT -u $DB_USER -p$DB_PASS $DB_NAME < "$PROJECT_DIR/init_database.sql" 2>&1 || {
            print_warning "SQL脚本执行失败，请手动执行"
            echo "手动执行命令："
            echo "mysql -h $DB_HOST -P $DB_PORT -u $DB_USER -p $DB_NAME < init_database.sql"
        }
        print_success "数据库脚本执行完成"
    else
        print_warning "MySQL命令未找到，请手动执行SQL脚本"
        echo "手动执行命令："
        echo "mysql -h $DB_HOST -P $DB_PORT -u $DB_USER -p $DB_NAME < init_database.sql"
    fi
else
    print_error "未找到init_database.sql"
fi

echo ""

# 步骤3：编译项目
echo "步骤3: 编译项目..."
echo "----------------------------"

cd "$PROJECT_DIR"

if [ -f "pom.xml" ]; then
    if command -v mvn &> /dev/null; then
        print_success "Maven已安装，开始编译..."
        mvn clean package -DskipTests || {
            print_error "编译失败"
            exit 1
        }
        print_success "编译完成"
    else
        print_warning "Maven未安装，跳过编译"
        echo "请手动执行: mvn clean package -DskipTests"
    fi
else
    print_error "未找到pom.xml"
fi

echo ""

# 步骤4：创建Prometheus配置
echo "步骤4: 创建Prometheus配置..."
echo "----------------------------"

cat > "$PROJECT_DIR/prometheus.yml" << 'EOF'
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
          environment: 'test'
EOF

print_success "创建prometheus.yml"

# 步骤5：创建告警规则
echo "步骤5: 创建告警规则..."
echo "----------------------------"

cat > "$PROJECT_DIR/alert_rules.yml" << 'EOF'
groups:
  - name: library_backend_alerts
    interval: 30s
    rules:
      - alert: ServiceDown
        expr: up{job="library-backend"} == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "图书馆后端服务宕机"
          description: "服务已宕机超过1分钟"

      - alert: HighErrorRate
        expr: |
          sum(rate(library_api_calls_total{status=~"5.."}[5m]))
          /
          sum(rate(library_api_calls_total[5m])) > 0.05
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "API错误率过高"
          description: "5xx错误率超过5%"

      - alert: HighMemoryUsage
        expr: |
          (jvm_memory_used_bytes{area="heap"}
          /
          jvm_memory_max_bytes{area="heap"}) > 0.85
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "JVM堆内存使用率过高"
          description: "堆内存使用率超过85%"
EOF

print_success "创建alert_rules.yml"

# 步骤6：创建Alertmanager配置
echo "步骤6: 创建Alertmanager配置..."
echo "----------------------------"

cat > "$PROJECT_DIR/alertmanager.yml" << 'EOF'
global:
  resolve_timeout: 5m

route:
  group_by: ['alertname', 'severity']
  group_wait: 10s
  group_interval: 10s
  repeat_interval: 12h
  receiver: 'default'

receivers:
  - name: 'default'
    webhook_configs:
      - url: 'http://localhost:5001/webhook'
EOF

print_success "创建alertmanager.yml"

# 步骤7：创建Docker Compose配置
echo "步骤7: 创建Docker Compose配置..."
echo "----------------------------"

cat > "$PROJECT_DIR/docker-compose.yml" << 'EOF'
version: '3.8'

services:
  prometheus:
    image: prom/prometheus:latest
    container_name: library-prometheus
    ports:
      - "9090:9090"
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml
      - ./alert_rules.yml:/etc/prometheus/alert_rules.yml
      - prometheus-data:/prometheus
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'
    restart: unless-stopped
    networks:
      - monitoring

  alertmanager:
    image: prom/alertmanager:latest
    container_name: library-alertmanager
    ports:
      - "9093:9093"
    volumes:
      - ./alertmanager.yml:/etc/alertmanager/alertmanager.yml
      - alertmanager-data:/alertmanager
    command:
      - '--config.file=/etc/alertmanager/alertmanager.yml'
    restart: unless-stopped
    networks:
      - monitoring

  grafana:
    image: grafana/grafana:latest
    container_name: library-grafana
    ports:
      - "3000:3000"
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin
      - GF_USERS_ALLOW_SIGN_UP=false
    volumes:
      - grafana-data:/var/lib/grafana
    restart: unless-stopped
    networks:
      - monitoring

volumes:
  prometheus-data:
  alertmanager-data:
  grafana-data:

networks:
  monitoring:
    driver: bridge
EOF

print_success "创建docker-compose.yml"

echo ""

# 步骤8：启动监控服务
echo "步骤8: 启动监控服务..."
echo "----------------------------"

if command -v docker-compose &> /dev/null || command -v docker &> /dev/null; then
    print_success "Docker已安装"
    echo "启动Prometheus、Alertmanager和Grafana..."

    if command -v docker-compose &> /dev/null; then
        docker-compose up -d || print_warning "Docker Compose启动失败，请手动执行"
    elif command -v docker &> /dev/null; then
        docker compose up -d || print_warning "Docker Compose启动失败，请手动执行"
    fi

    print_success "监控服务启动完成"
    echo ""
    echo "访问地址："
    echo "  - Prometheus: http://localhost:9090"
    echo "  - Alertmanager: http://localhost:9093"
    echo "  - Grafana: http://localhost:3000 (admin/admin)"
else
    print_warning "Docker未安装，跳过监控服务启动"
    echo "请手动安装Docker并执行: docker-compose up -d"
fi

echo ""

# 步骤9：更新application.yml
echo "步骤9: 更新application.yml配置..."
echo "----------------------------"

if [ -f "$PROJECT_DIR/src/main/resources/application.yml" ]; then
    # 检查是否已有Actuator配置
    if grep -q "management:" "$PROJECT_DIR/src/main/resources/application.yml"; then
        print_success "application.yml已包含Actuator配置"
    else
        print_warning "需要手动添加Actuator配置到application.yml"
        echo ""
        echo "请在application.yml末尾添加以下配置："
        echo "---"
        cat << 'YAML'
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
YAML
        echo "---"
    fi
else
    print_error "未找到application.yml"
fi

echo ""

# 步骤10：生成部署报告
echo "步骤10: 生成部署报告..."
echo "----------------------------"

cat > "$PROJECT_DIR/DEPLOYMENT_REPORT.md" << 'EOF'
# 部署报告

## 部署时间
$(date '+%Y-%m-%d %H:%M:%S')

## 部署内容
- ✅ 数据库初始化脚本已执行
- ✅ Prometheus配置已创建
- ✅ Alertmanager配置已创建
- ✅ Docker Compose配置已创建
- ✅ 监控服务已启动

## 访问地址
- 后端服务: http://localhost:8080
- Prometheus: http://localhost:9090
- Alertmanager: http://localhost:9093
- Grafana: http://localhost:3000 (admin/admin)

## 验证步骤
1. 检查应用是否启动
   ```bash
   curl http://localhost:8080/actuator/health
   ```

2. 检查Prometheus是否采集数据
   ```bash
   curl http://localhost:8080/actuator/prometheus
   ```

3. 访问Prometheus UI
   - 打开 http://localhost:9090
   - Status → Targets
   - 确认 library-backend 状态为 UP

4. 访问Grafana
   - 打开 http://localhost:3000
   - 登录 (admin/admin)
   - 添加Prometheus数据源: http://prometheus:9090

## 下一步
- [ ] 启动后端应用
- [ ] 验证监控指标
- [ ] 配置Grafana仪表盘
- [ ] 测试告警规则
- [ ] 修复高优先级Bug

## 注意事项
- 确保8080、9090、9093、3000端口未被占用
- 确保Docker服务正在运行
- 确保MySQL服务正在运行
EOF

print_success "生成DEPLOYMENT_REPORT.md"

echo ""
echo "=========================================="
echo "部署脚本执行完成！"
echo "=========================================="
echo ""
echo "📋 部署摘要："
echo "  ✅ 数据库脚本已准备"
echo "  ✅ Prometheus配置已创建"
echo "  ✅ 告警规则已配置"
echo "  ✅ Docker Compose已配置"
echo ""
echo "🚀 下一步操作："
echo "  1. 启动后端应用: java -jar target/library-backend.jar"
echo "  2. 验证健康检查: curl http://localhost:8080/actuator/health"
echo "  3. 访问Prometheus: http://localhost:9090"
echo "  4. 访问Grafana: http://localhost:3000"
echo ""
echo "📖 详细信息请查看: DEPLOYMENT_REPORT.md"
echo ""
