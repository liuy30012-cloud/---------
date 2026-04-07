# 生产环境部署清单

## ⚠️ 部署前必须完成的安全配置

### 1. 修改JWT密钥
**位置**: `application-prod.yml`
```yaml
jwt:
  secret: ${JWT_SECRET:YOUR_VERY_LONG_RANDOM_SECRET_KEY_HERE}
```

**生成强密钥**:
```bash
# 方法1: 使用OpenSSL
openssl rand -base64 64

# 方法2: 使用Python
python3 -c "import secrets; print(secrets.token_urlsafe(64))"

# 方法3: 使用Node.js
node -e "console.log(require('crypto').randomBytes(64).toString('base64'))"
```

**设置环境变量**:
```bash
export JWT_SECRET="your_generated_secret_here"
```

---

### 2. 修改数据库密码
**位置**: `application-prod.yml`
```yaml
spring:
  datasource:
    username: ${DB_USERNAME:library_prod_user}
    password: ${DB_PASSWORD:CHANGE_ME}
```

**设置环境变量**:
```bash
export DB_USERNAME="library_prod_user"
export DB_PASSWORD="your_strong_database_password"
```

**创建数据库用户**:
```sql
-- 连接到MySQL
mysql -u root -p

-- 创建生产数据库
CREATE DATABASE library_prod CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 创建专用用户（限制权限）
CREATE USER 'library_prod_user'@'%' IDENTIFIED BY 'your_strong_password';

-- 授予必要权限（不包括DROP、ALTER等危险权限）
GRANT SELECT, INSERT, UPDATE, DELETE ON library_prod.* TO 'library_prod_user'@'%';

-- 刷新权限
FLUSH PRIVILEGES;
```

---

### 3. 配置Redis密码
**位置**: `application-prod.yml`
```yaml
spring:
  redis:
    password: ${REDIS_PASSWORD:CHANGE_ME}
```

**设置Redis密码**:
```bash
# 编辑redis.conf
requirepass your_strong_redis_password

# 重启Redis
systemctl restart redis
```

**设置环境变量**:
```bash
export REDIS_PASSWORD="your_strong_redis_password"
```

---

### 4. 配置CORS白名单
**位置**: `application-prod.yml`
```yaml
cors:
  allowed-origins: ${CORS_ORIGINS:https://library.yourdomain.com}
```

**设置环境变量**:
```bash
export CORS_ORIGINS="https://library.yourdomain.com,https://www.yourdomain.com"
```

---

### 5. 配置SSL/TLS证书
**使用Let's Encrypt**:
```bash
# 安装certbot
sudo apt-get install certbot

# 获取证书
sudo certbot certonly --standalone -d library.yourdomain.com

# 证书位置
# /etc/letsencrypt/live/library.yourdomain.com/fullchain.pem
# /etc/letsencrypt/live/library.yourdomain.com/privkey.pem
```

**配置Nginx反向代理**:
```nginx
server {
    listen 443 ssl http2;
    server_name library.yourdomain.com;

    ssl_certificate /etc/letsencrypt/live/library.yourdomain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/library.yourdomain.com/privkey.pem;

    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;

    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

---

## 📋 部署步骤

### 步骤1: 准备服务器环境
```bash
# 更新系统
sudo apt-get update && sudo apt-get upgrade -y

# 安装Java 17+
sudo apt-get install openjdk-17-jdk -y

# 安装MySQL
sudo apt-get install mysql-server -y

# 安装Redis
sudo apt-get install redis-server -y

# 安装Nginx
sudo apt-get install nginx -y

# 安装Docker（用于监控）
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
```

---

### 步骤2: 初始化数据库
```bash
# 执行数据库脚本
mysql -u root -p library_prod < init_database.sql

# 验证表创建
mysql -u root -p -e "USE library_prod; SHOW TABLES;"
```

---

### 步骤3: 配置环境变量
```bash
# 创建环境变量文件
sudo nano /etc/systemd/system/library-backend.service.d/override.conf

# 添加以下内容
[Service]
Environment="JWT_SECRET=your_generated_secret"
Environment="DB_USERNAME=library_prod_user"
Environment="DB_PASSWORD=your_db_password"
Environment="REDIS_PASSWORD=your_redis_password"
Environment="CORS_ORIGINS=https://library.yourdomain.com"
Environment="SPRING_PROFILES_ACTIVE=prod"
```

---

### 步骤4: 部署应用
```bash
# 编译项目
mvn clean package -DskipTests -Pprod

# 创建部署目录
sudo mkdir -p /opt/library-backend
sudo cp target/library-backend.jar /opt/library-backend/

# 创建systemd服务
sudo nano /etc/systemd/system/library-backend.service
```

**服务配置**:
```ini
[Unit]
Description=Library Backend Service
After=network.target mysql.service redis.service

[Service]
Type=simple
User=library
WorkingDirectory=/opt/library-backend
ExecStart=/usr/bin/java -jar -Xms512m -Xmx2g -Dspring.profiles.active=prod /opt/library-backend/library-backend.jar
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

**启动服务**:
```bash
# 重载systemd
sudo systemctl daemon-reload

# 启动服务
sudo systemctl start library-backend

# 设置开机自启
sudo systemctl enable library-backend

# 查看状态
sudo systemctl status library-backend
```

---

### 步骤5: 部署监控服务
```bash
# 启动Prometheus、Alertmanager、Grafana
cd /opt/library-backend
docker-compose up -d

# 验证服务
docker-compose ps
```

---

### 步骤6: 配置防火墙
```bash
# 允许HTTP/HTTPS
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp

# 允许SSH
sudo ufw allow 22/tcp

# 禁止直接访问应用端口（通过Nginx代理）
sudo ufw deny 8080/tcp

# 启用防火墙
sudo ufw enable
```

---

## 🔍 验证部署

### 1. 健康检查
```bash
curl https://library.yourdomain.com/actuator/health
```

**期望输出**:
```json
{"status":"UP"}
```

---

### 2. 测试登录
```bash
curl -X POST https://library.yourdomain.com/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"studentId":"2021001","password":"Test123456"}'
```

---

### 3. 检查监控
- Prometheus: http://your-server:9090
- Grafana: http://your-server:3000
- Alertmanager: http://your-server:9093

---

## 🔄 回滚方案

### 方案1: 快速回滚到上一版本
```bash
# 停止当前服务
sudo systemctl stop library-backend

# 恢复旧版本
sudo cp /opt/library-backend/library-backend.jar.backup /opt/library-backend/library-backend.jar

# 启动服务
sudo systemctl start library-backend
```

---

### 方案2: 数据库回滚
```bash
# 恢复数据库备份
mysql -u root -p library_prod < backup_$(date +%Y%m%d).sql
```

---

### 方案3: 完整回滚
```bash
# 执行回滚脚本
bash rollback.sh
```

---

## 📊 监控告警

### 关键指标
- CPU使用率 > 80%
- 内存使用率 > 85%
- API错误率 > 5%
- 响应时间 > 2秒
- 数据库连接池耗尽

### 告警通知
- 邮件: ops@yourdomain.com
- 短信: 运维人员手机
- Slack: #ops-alerts

---

## 🚨 应急联系人

| 角色 | 姓名 | 电话 | 邮箱 |
|------|------|------|------|
| 技术负责人 | XXX | 138xxxx | xxx@domain.com |
| 运维工程师 | XXX | 139xxxx | xxx@domain.com |
| DBA | XXX | 137xxxx | xxx@domain.com |

---

## 📝 部署检查清单

- [ ] JWT密钥已修改
- [ ] 数据库密码已修改
- [ ] Redis密码已配置
- [ ] CORS白名单已配置
- [ ] SSL证书已安装
- [ ] 数据库已初始化
- [ ] 环境变量已设置
- [ ] 应用已启动
- [ ] 监控服务已部署
- [ ] 防火墙已配置
- [ ] 健康检查通过
- [ ] 备份策略已配置
- [ ] 回滚方案已准备
- [ ] 应急联系人已确认
- [ ] 文档已更新

---

**部署日期**: ___________  
**部署人员**: ___________  
**审核人员**: ___________
