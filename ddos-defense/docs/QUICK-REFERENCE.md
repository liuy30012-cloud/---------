# 快速参考指南

## 常用命令

### 部署
```bash
# 完整部署
sudo ./deploy.sh

# 仅防火墙
sudo ./scripts/firewall-setup.sh

# 仅系统优化
sudo ./scripts/system-optimize.sh

# 状态检查
sudo ./scripts/status-check.sh
```

### 监控
```bash
# 查看实时连接
watch -n 1 'netstat -ntu | awk "{print \$5}" | cut -d: -f1 | sort | uniq -c | sort -rn | head -10'

# 查看监控日志
tail -f /var/log/ddos-monitor.log

# 查看 Nginx 日志
tail -f /var/log/nginx/access.log
tail -f /var/log/nginx/error.log
```

### 封禁管理
```bash
# 封禁 IP
sudo iptables -I INPUT -s <IP> -j DROP

# 解封 IP
sudo iptables -D INPUT -s <IP> -j DROP

# 查看已封禁 IP
sudo iptables -L INPUT -n | grep DROP

# Fail2ban 解封
sudo fail2ban-client set <jail> unbanip <IP>
```

### 服务管理
```bash
# Nginx
sudo systemctl status nginx
sudo systemctl reload nginx
sudo nginx -t

# Fail2ban
sudo systemctl status fail2ban
sudo fail2ban-client status
sudo fail2ban-client reload

# 监控服务
sudo systemctl status ddos-monitor
sudo systemctl restart ddos-monitor
```

## 配置文件位置

| 组件 | 配置文件 |
|------|---------|
| 防火墙 | `/etc/iptables/rules.v4` |
| Nginx | `/etc/nginx/conf.d/ddos-protection.conf` |
| Fail2ban | `/etc/fail2ban/jail.local` |
| 系统参数 | `/etc/sysctl.d/99-ddos-protection.conf` |
| 监控 | `monitoring/monitor-config.json` |

## 关键参数

### Nginx 速率限制
```nginx
limit_req_zone $binary_remote_addr zone=general_limit:10m rate=10r/s;
limit_conn_zone $binary_remote_addr zone=conn_limit:10m;
```

### 防火墙连接限制
```bash
--connlimit-above 50    # 单 IP 最大连接数
--hitcount 20           # 触发封禁的请求数
--seconds 10            # 时间窗口
```

### 系统参数
```bash
net.ipv4.tcp_syncookies = 1              # SYN Cookies
net.ipv4.tcp_max_syn_backlog = 8192      # SYN 队列
net.netfilter.nf_conntrack_max = 1000000 # 连接跟踪
```

## 故障排除

### 无法访问网站
1. 检查 IP 是否被封禁
2. 查看防火墙规则
3. 检查 Nginx 配置
4. 查看错误日志

### Fail2ban 不工作
```bash
sudo systemctl restart fail2ban
sudo fail2ban-client reload
sudo tail -f /var/log/fail2ban.log
```

### 监控脚本报错
1. 检查 Python 版本
2. 确认 root 权限
3. 验证配置文件路径

## 性能调优建议

| 服务器规模 | 连接限制 | 请求速率 | SYN Backlog |
|-----------|---------|---------|------------|
| 小型 (1-2核) | 20-30 | 5-10 r/s | 2048 |
| 中型 (4-8核) | 50-100 | 10-20 r/s | 4096 |
| 大型 (16+核) | 100-200 | 20-50 r/s | 8192 |

## 应急响应流程

1. **识别攻击**
   ```bash
   sudo ./scripts/status-check.sh
   ```

2. **分析攻击源**
   ```bash
   netstat -ntu | awk '{print $5}' | cut -d: -f1 | sort | uniq -c | sort -rn | head -20
   ```

3. **临时封禁**
   ```bash
   sudo iptables -I INPUT -s <攻击IP> -j DROP
   ```

4. **启用严格模式**
   - 降低连接限制
   - 启用 CDN Under Attack Mode
   - 联系 ISP

5. **事后分析**
   ```bash
   grep "$(date +%d/%b/%Y)" /var/log/nginx/access.log | awk '{print $1}' | sort | uniq -c | sort -rn
   ```

## 维护计划

### 每日
- 检查监控日志
- 查看封禁 IP 列表
- 监控系统负载

### 每周
- 审查 Fail2ban 统计
- 分析攻击模式
- 更新黑名单

### 每月
- 备份配置文件
- 更新系统和软件
- 优化防御参数
- 清理过期日志
