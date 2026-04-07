# DDoS 防御系统部署文档

## 项目概述

这是一套完整的 DDoS 防御解决方案，针对 MHDDoS 等攻击工具的 57 种攻击方法提供多层防护。

## 目录结构

```
ddos-defense/
├── scripts/                    # 可执行脚本
│   ├── firewall-setup.sh      # 防火墙配置脚本
│   └── system-optimize.sh     # 系统参数优化脚本
├── configs/                    # 配置文件
│   ├── nginx/                 # Nginx 配置
│   │   └── ddos-protection.conf
│   ├── fail2ban/              # Fail2ban 配置
│   │   ├── jail.local
│   │   ├── http-get-dos.conf
│   │   ├── http-post-dos.conf
│   │   └── nginx-limit-req.conf
│   └── firewall/              # 防火墙配置
├── monitoring/                 # 监控工具
│   ├── ddos-monitor.py        # 实时监控脚本
│   └── monitor-config.json    # 监控配置
└── docs/                       # 文档
    └── README.md              # 本文档
```

## 快速开始

### 前置要求

- Linux 系统（Ubuntu 20.04+ / CentOS 7+ / Debian 10+）
- Root 权限
- 已安装的软件：
  - iptables
  - nginx（如果使用 Web 服务）
  - fail2ban（可选）
  - python3（用于监控脚本）

### 安装步骤

#### 1. 系统参数优化

首先优化系统内核参数以提高抗 DDoS 能力：

```bash
cd ddos-defense/scripts
chmod +x system-optimize.sh
sudo ./system-optimize.sh
```

**注意：** 建议在执行后重启系统以确保所有参数生效。

#### 2. 配置防火墙

运行防火墙配置脚本：

```bash
chmod +x firewall-setup.sh
sudo ./firewall-setup.sh
```

**重要提示：**
- 脚本会备份现有防火墙规则到 `/root/iptables-backup-*.rules`
- 确保 SSH 端口（默认 22）在规则中被允许，避免被锁定
- 根据实际需求修改脚本中的端口和限制参数

#### 3. 配置 Nginx（如果使用）

将 Nginx 配置文件复制到 Nginx 配置目录：

```bash
# 备份原配置
sudo cp /etc/nginx/nginx.conf /etc/nginx/nginx.conf.backup

# 复制防护配置
sudo cp configs/nginx/ddos-protection.conf /etc/nginx/conf.d/

# 修改配置文件中的域名和路径
sudo nano /etc/nginx/conf.d/ddos-protection.conf

# 测试配置
sudo nginx -t

# 重载 Nginx
sudo systemctl reload nginx
```

**配置要点：**
- 修改 `server_name` 为你的域名
- 调整 `limit_req` 和 `limit_conn` 参数
- 根据需要启用/禁用地理位置封禁
- 配置 SSL 证书路径

#### 4. 配置 Fail2ban（推荐）

安装并配置 Fail2ban：

```bash
# 安装 Fail2ban
sudo apt-get install fail2ban  # Ubuntu/Debian
# 或
sudo yum install fail2ban      # CentOS/RHEL

# 复制配置文件
sudo cp configs/fail2ban/jail.local /etc/fail2ban/
sudo cp configs/fail2ban/*.conf /etc/fail2ban/filter.d/

# 启动 Fail2ban
sudo systemctl enable fail2ban
sudo systemctl start fail2ban

# 查看状态
sudo fail2ban-client status
```

#### 5. 部署监控脚本

配置并运行实时监控脚本：

```bash
cd monitoring

# 修改配置（可选）
nano monitor-config.json

# 运行监控（前台测试）
sudo python3 ddos-monitor.py

# 或作为后台服务运行
sudo nohup python3 ddos-monitor.py > /var/log/ddos-monitor.log 2>&1 &
```

**创建 systemd 服务（推荐）：**

```bash
sudo nano /etc/systemd/system/ddos-monitor.service
```

添加以下内容：

```ini
[Unit]
Description=DDoS Monitoring Service
After=network.target

[Service]
Type=simple
User=root
WorkingDirectory=/path/to/ddos-defense/monitoring
ExecStart=/usr/bin/python3 /path/to/ddos-defense/monitoring/ddos-monitor.py
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

启动服务：

```bash
sudo systemctl daemon-reload
sudo systemctl enable ddos-monitor
sudo systemctl start ddos-monitor
sudo systemctl status ddos-monitor
```

## 防御层级说明

### Layer 4 (传输层) 防御

**防火墙规则：**
- SYN Flood 防护：限制 SYN 包速率
- UDP Flood 防护：限制 UDP 流量
- ICMP Flood 防护：限制 ICMP echo 请求
- 连接数限制：限制单个 IP 的并发连接
- 无效包过滤：丢弃畸形数据包

**系统参数优化：**
- 启用 SYN Cookies
- 增加 SYN 队列长度
- 优化 TCP 参数
- 增加连接跟踪表大小

### Layer 7 (应用层) 防御

**Nginx 配置：**
- 请求速率限制（Rate Limiting）
- 连接数限制
- User-Agent 过滤
- 请求方法限制
- 超时设置（防 Slowloris）
- 请求体大小限制

**Fail2ban：**
- HTTP Flood 检测
- 恶意爬虫封禁
- 重复违规者长期封禁

### 实时监控

**监控脚本功能：**
- 实时连接监控
- 请求速率分析
- 自动 IP 封禁
- 统计信息输出
- 日志记录

## 配置调优

### 根据服务器规模调整参数

**小型服务器（1-2 核心，1-2GB RAM）：**
```bash
# Nginx
limit_req rate=5r/s;
limit_conn conn_limit 20;

# 防火墙
--connlimit-above 20
--hitcount 10
```

**中型服务器（4-8 核心，4-8GB RAM）：**
```bash
# Nginx
limit_req rate=10r/s;
limit_conn conn_limit 50;

# 防火墙
--connlimit-above 50
--hitcount 20
```

**大型服务器（16+ 核心，16GB+ RAM）：**
```bash
# Nginx
limit_req rate=20r/s;
limit_conn conn_limit 100;

# 防火墙
--connlimit-above 100
--hitcount 50
```

### 白名单配置

**防火墙白名单：**
```bash
# 在 firewall-setup.sh 中添加
iptables -A INPUT -s 可信IP -j ACCEPT
```

**Nginx 白名单：**
```nginx
geo $whitelist {
    default 0;
    1.2.3.4 1;  # 可信 IP
}

if ($whitelist = 0) {
    # 应用限制
}
```

**监控脚本白名单：**
在 `monitor-config.json` 中添加：
```json
"whitelist": [
    "127.0.0.1",
    "可信IP1",
    "可信IP2"
]
```

## 监控和维护

### 查看防火墙规则

```bash
# 查看所有规则
sudo iptables -L -n -v --line-numbers

# 查看特定链
sudo iptables -L INPUT -n -v --line-numbers

# 查看封禁的 IP
sudo iptables -L INPUT -n | grep DROP
```

### 查看 Fail2ban 状态

```bash
# 查看所有 jail 状态
sudo fail2ban-client status

# 查看特定 jail
sudo fail2ban-client status http-get-dos

# 解封 IP
sudo fail2ban-client set http-get-dos unbanip IP地址
```

### 查看监控日志

```bash
# 实时查看日志
tail -f /var/log/ddos-monitor.log

# 查看 Nginx 访问日志
tail -f /var/log/nginx/access.log

# 查看 Nginx 错误日志
tail -f /var/log/nginx/error.log
```

### 手动封禁/解封 IP

```bash
# 封禁 IP
sudo iptables -I INPUT -s IP地址 -j DROP

# 解封 IP
sudo iptables -D INPUT -s IP地址 -j DROP

# 保存规则
sudo netfilter-persistent save
```

## 应急响应

### 遭受攻击时的处理步骤

1. **确认攻击类型**
```bash
# 查看连接数
netstat -ntu | awk '{print $5}' | cut -d: -f1 | sort | uniq -c | sort -n

# 查看 SYN 连接
netstat -n | grep SYN | wc -l

# 查看 UDP 流量
sudo tcpdump -i eth0 udp -c 100
```

2. **临时封禁攻击源**
```bash
# 封禁单个 IP
sudo iptables -I INPUT -s 攻击IP -j DROP

# 封禁 IP 段
sudo iptables -I INPUT -s 攻击IP段/24 -j DROP
```

3. **启用更严格的限制**
```bash
# 临时降低连接限制
sudo iptables -R INPUT 规则编号 -p tcp --dport 80 -m connlimit --connlimit-above 10 -j REJECT
```

4. **启用 CloudFlare Under Attack Mode**（如果使用 CDN）

5. **联系 ISP 或 DDoS 清洗服务**

### 攻击后分析

```bash
# 分析访问日志
awk '{print $1}' /var/log/nginx/access.log | sort | uniq -c | sort -rn | head -20

# 分析攻击模式
grep "$(date +%d/%b/%Y)" /var/log/nginx/access.log | awk '{print $7}' | sort | uniq -c | sort -rn

# 查看被封禁的 IP 列表
sudo iptables -L INPUT -n | grep DROP | awk '{print $4}' > blocked-ips.txt
```

## 性能影响

- **防火墙规则：** 轻微影响（< 1% CPU）
- **Nginx 限制：** 轻微影响（< 2% CPU）
- **Fail2ban：** 轻微影响（< 1% CPU）
- **监控脚本：** 中等影响（2-5% CPU）

## 故障排除

### 问题：无法访问网站

**检查：**
1. 确认 IP 未被误封
2. 检查防火墙规则是否正确
3. 查看 Nginx 错误日志

**解决：**
```bash
# 临时清空防火墙规则
sudo iptables -F

# 重新加载配置
sudo ./firewall-setup.sh
```

### 问题：Fail2ban 未工作

**检查：**
```bash
sudo systemctl status fail2ban
sudo fail2ban-client ping
```

**解决：**
```bash
sudo systemctl restart fail2ban
sudo fail2ban-client reload
```

### 问题：监控脚本报错

**检查：**
- Python 版本（需要 3.6+）
- 权限（需要 root）
- 日志文件路径是否存在

## 进阶配置

### 集成 CloudFlare

1. 配置源站 IP 白名单
2. 启用 CloudFlare Firewall Rules
3. 配置 Rate Limiting
4. 启用 Bot Fight Mode

### 使用 GeoIP 封禁

```bash
# 安装 GeoIP
sudo apt-get install geoip-database libgeoip1

# 在 iptables 中使用
sudo iptables -A INPUT -m geoip --src-cc CN -j DROP
```

### 配置日志轮转

```bash
sudo nano /etc/logrotate.d/ddos-defense
```

添加：
```
/var/log/ddos-monitor.log {
    daily
    rotate 7
    compress
    delaycompress
    missingok
    notifempty
}
```

## 测试防御效果

**注意：仅在测试环境或获得授权的情况下进行测试！**

### 测试连接限制

```bash
# 使用 ab (Apache Bench)
ab -n 1000 -c 100 http://your-domain.com/

# 使用 siege
siege -c 100 -r 10 http://your-domain.com/
```

### 测试速率限制

```bash
# 快速发送请求
for i in {1..100}; do curl http://your-domain.com/ & done
```

## 更新和维护

### 定期更新

```bash
# 更新系统
sudo apt-get update && sudo apt-get upgrade

# 更新 Fail2ban 规则
sudo fail2ban-client reload

# 检查防火墙规则
sudo iptables -L -n -v
```

### 备份配置

```bash
# 备份防火墙规则
sudo iptables-save > /backup/iptables-$(date +%Y%m%d).rules

# 备份 Nginx 配置
sudo cp -r /etc/nginx /backup/nginx-$(date +%Y%m%d)

# 备份 Fail2ban 配置
sudo cp -r /etc/fail2ban /backup/fail2ban-$(date +%Y%m%d)
```

## 支持和贡献

如有问题或建议，请提交 Issue 或 Pull Request。

## 许可证

MIT License

## 免责声明

本防御系统仅供合法防御用途。使用者需遵守当地法律法规，不得用于非法目的。
