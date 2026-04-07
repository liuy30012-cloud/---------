# DDoS 防御系统

针对 MHDDoS 等 DDoS 攻击工具的综合防御解决方案，提供多层防护机制。

## 特性

- ✅ **Layer 4 防御**：防火墙规则防护 SYN/UDP/ICMP Flood
- ✅ **Layer 7 防御**：Nginx 速率限制和连接控制
- ✅ **系统优化**：内核参数调优提升抗攻击能力
- ✅ **自动封禁**：Fail2ban 自动检测和封禁恶意 IP
- ✅ **实时监控**：Python 监控脚本实时分析流量
- ✅ **一键部署**：自动化部署脚本快速配置

## 防御能力

针对以下攻击类型提供防护：

### Layer 7 攻击
- HTTP GET/POST Flood
- Slowloris 慢速攻击
- HTTP Header 攻击
- Cookie 攻击
- User-Agent 攻击
- WordPress XMLRPC 攻击

### Layer 4 攻击
- SYN Flood
- UDP Flood
- ICMP Flood
- TCP Connection Flood
- 端口扫描

### 其他防护
- 恶意爬虫过滤
- 代理检测
- 地理位置封禁
- IP 黑名单

## 快速开始

### 一键部署

```bash
git clone <repository-url>
cd ddos-defense
chmod +x deploy.sh
sudo ./deploy.sh
```

### 手动部署

```bash
# 1. 系统优化
sudo ./scripts/system-optimize.sh

# 2. 配置防火墙
sudo ./scripts/firewall-setup.sh

# 3. 配置 Nginx
sudo cp configs/nginx/ddos-protection.conf /etc/nginx/conf.d/
sudo nginx -t && sudo systemctl reload nginx

# 4. 配置 Fail2ban
sudo cp configs/fail2ban/jail.local /etc/fail2ban/
sudo cp configs/fail2ban/*.conf /etc/fail2ban/filter.d/
sudo systemctl restart fail2ban

# 5. 启动监控
sudo python3 monitoring/ddos-monitor.py
```

## 目录结构

```
ddos-defense/
├── deploy.sh                   # 一键部署脚本
├── scripts/                    # 可执行脚本
│   ├── firewall-setup.sh      # 防火墙配置
│   └── system-optimize.sh     # 系统优化
├── configs/                    # 配置文件
│   ├── nginx/                 # Nginx 配置
│   ├── fail2ban/              # Fail2ban 配置
│   └── firewall/              # 防火墙配置
├── monitoring/                 # 监控工具
│   ├── ddos-monitor.py        # 监控脚本
│   └── monitor-config.json    # 监控配置
└── docs/                       # 文档
    └── README.md              # 详细文档
```

## 系统要求

- Linux 系统（Ubuntu 20.04+ / CentOS 7+ / Debian 10+）
- Root 权限
- 2GB+ RAM（推荐 4GB+）
- iptables
- Python 3.6+（用于监控脚本）

## 配置说明

### 防火墙规则

编辑 `scripts/firewall-setup.sh` 调整：
- 连接数限制
- 速率限制
- 允许的端口
- SSH 端口

### Nginx 配置

编辑 `configs/nginx/ddos-protection.conf` 调整：
- 域名
- 速率限制参数
- 连接数限制
- 超时设置
- User-Agent 黑名单

### 监控配置

编辑 `monitoring/monitor-config.json` 调整：
- 最大连接数
- 最大请求速率
- 封禁时长
- 检查间隔
- 白名单 IP

## 监控和管理

### 查看防火墙状态

```bash
sudo iptables -L -n -v --line-numbers
```

### 查看 Fail2ban 状态

```bash
sudo fail2ban-client status
sudo fail2ban-client status http-get-dos
```

### 查看监控日志

```bash
tail -f /var/log/ddos-monitor.log
```

### 手动封禁 IP

```bash
sudo iptables -I INPUT -s <IP地址> -j DROP
```

### 解封 IP

```bash
sudo iptables -D INPUT -s <IP地址> -j DROP
sudo fail2ban-client set <jail名称> unbanip <IP地址>
```

## 性能调优

根据服务器规模调整参数：

**小型服务器（1-2 核心）：**
- 连接限制：20-30
- 请求速率：5-10 r/s

**中型服务器（4-8 核心）：**
- 连接限制：50-100
- 请求速率：10-20 r/s

**大型服务器（16+ 核心）：**
- 连接限制：100-200
- 请求速率：20-50 r/s

## 应急响应

遭受攻击时：

1. 查看攻击源：
```bash
netstat -ntu | awk '{print $5}' | cut -d: -f1 | sort | uniq -c | sort -rn | head -20
```

2. 临时封禁：
```bash
sudo iptables -I INPUT -s <攻击IP> -j DROP
```

3. 启用更严格限制：
```bash
# 降低连接限制
# 启用 CloudFlare Under Attack Mode
# 联系 ISP 或 DDoS 清洗服务
```

## 测试

**警告：仅在测试环境或获得授权的情况下测试！**

```bash
# 测试连接限制
ab -n 1000 -c 100 http://your-domain.com/

# 测试速率限制
for i in {1..100}; do curl http://your-domain.com/ & done
```

## 文档

详细文档请查看 [docs/README.md](docs/README.md)

## 注意事项

1. **备份配置**：部署前备份现有配置
2. **测试环境**：建议先在测试环境验证
3. **SSH 访问**：确保 SSH 端口不被封禁
4. **白名单**：添加可信 IP 到白名单
5. **监控日志**：定期检查日志和封禁记录

## 更新日志

- v1.0.0 - 初始版本
  - 防火墙规则配置
  - Nginx 防护配置
  - Fail2ban 集成
  - 实时监控脚本
  - 系统参数优化

## 贡献

欢迎提交 Issue 和 Pull Request。

## 许可证

MIT License

## 免责声明

本系统仅供合法防御用途。使用者需遵守当地法律法规。
