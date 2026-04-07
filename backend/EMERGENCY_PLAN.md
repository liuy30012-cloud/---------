# 图书馆书籍定位系统 - 应急预案

## 📋 目录
1. [应急响应流程](#应急响应流程)
2. [常见故障处理](#常见故障处理)
3. [数据备份与恢复](#数据备份与恢复)
4. [性能问题排查](#性能问题排查)
5. [安全事件响应](#安全事件响应)
6. [联系人信息](#联系人信息)

---

## 🚨 应急响应流程

### 1. 故障分级

| 级别 | 影响范围 | 响应时间 | 处理人员 |
|------|----------|----------|----------|
| **P0 - 紧急** | 服务完全不可用 | 15分钟内 | 全体技术团队 |
| **P1 - 严重** | 核心功能不可用 | 1小时内 | 后端负责人 + 运维 |
| **P2 - 重要** | 部分功能异常 | 4小时内 | 相关开发人员 |
| **P3 - 一般** | 性能下降或小问题 | 24小时内 | 值班人员 |

### 2. 响应步骤

```
1. 告警接收 → 2. 问题确认 → 3. 影响评估 → 4. 应急处理 → 5. 问题解决 → 6. 复盘总结
```

#### 步骤详解：
1. **告警接收**（0-5分钟）
   - 确认告警真实性
   - 记录告警时间和内容
   - 通知相关人员

2. **问题确认**（5-15分钟）
   - 检查服务状态
   - 查看监控指标
   - 分析日志文件

3. **影响评估**（15-30分钟）
   - 确定影响范围
   - 评估业务损失
   - 决定处理优先级

4. **应急处理**（30分钟-2小时）
   - 执行应急方案
   - 实时监控恢复情况
   - 保持沟通畅通

5. **问题解决**（2-4小时）
   - 根本原因分析
   - 实施永久修复
   - 验证修复效果

6. **复盘总结**（24小时内）
   - 编写故障报告
   - 更新应急预案
   - 优化监控告警

---

## 🔧 常见故障处理

### 故障1: 服务无法启动

**症状**: 
- 应用启动失败
- 端口被占用
- 数据库连接失败

**排查步骤**:
```bash
# 1. 检查端口占用
netstat -ano | findstr :8080
# 如果被占用，杀死进程
taskkill /PID <PID> /F

# 2. 检查数据库连接
mysql -h localhost -u root -p
# 测试连接是否正常

# 3. 查看应用日志
tail -f logs/library-backend.log

# 4. 检查JVM内存
jps -lvm
jstat -gc <pid>
```

**解决方案**:
```bash
# 方案1: 重启服务
systemctl restart library-backend

# 方案2: 修改端口（如果端口冲突）
export SERVER_PORT=8081
java -jar library-backend.jar

# 方案3: 修复数据库连接
# 检查application.yml中的数据库配置
# 确保MySQL服务正在运行
systemctl start mysql
```

---

### 故障2: 数据库连接池耗尽

**症状**:
- 请求超时
- 日志显示 "Connection pool exhausted"
- 监控显示连接数达到上限

**排查步骤**:
```bash
# 1. 查看当前连接数
mysql -u root -p -e "SHOW PROCESSLIST;"

# 2. 查看慢查询
mysql -u root -p -e "SHOW FULL PROCESSLIST;" | grep "Query"

# 3. 检查应用日志
grep "Connection" logs/library-backend.log
```

**解决方案**:
```yaml
# 临时方案：增加连接池大小（application.yml）
spring:
  datasource:
    hikari:
      maximum-pool-size: 30  # 从20增加到30
      connection-timeout: 60000

# 永久方案：优化慢查询
# 1. 添加索引
# 2. 优化SQL语句
# 3. 使用缓存减少数据库访问
```

**紧急处理**:
```bash
# 重启应用释放连接
systemctl restart library-backend

# 或者杀死长时间运行的查询
mysql -u root -p -e "KILL <process_id>;"
```

---

### 故障3: 内存溢出 (OutOfMemoryError)

**症状**:
- 应用崩溃
- 日志显示 "java.lang.OutOfMemoryError"
- 响应变慢

**排查步骤**:
```bash
# 1. 生成堆转储文件（如果应用还在运行）
jmap -dump:live,format=b,file=heap.bin <pid>

# 2. 分析堆转储
# 使用Eclipse MAT或VisualVM分析heap.bin

# 3. 查看GC日志
jstat -gcutil <pid> 1000
```

**解决方案**:
```bash
# 临时方案：增加堆内存
export JAVA_OPTS="-Xms2g -Xmx4g"
java $JAVA_OPTS -jar library-backend.jar

# 永久方案：修复内存泄漏
# 1. 检查是否有大对象未释放
# 2. 优化缓存策略
# 3. 修复代码中的内存泄漏
```

**紧急处理**:
```bash
# 立即重启服务
systemctl restart library-backend

# 启用GC日志以便后续分析
java -Xlog:gc*:file=gc.log -jar library-backend.jar
```

---

### 故障4: CPU使用率过高

**症状**:
- CPU持续100%
- 响应变慢
- 请求超时

**排查步骤**:
```bash
# 1. 找到占用CPU的线程
top -H -p <pid>

# 2. 获取线程堆栈
jstack <pid> > thread_dump.txt

# 3. 分析热点代码
# 将top显示的线程ID转换为16进制
printf "%x\n" <thread_id>
# 在thread_dump.txt中搜索该线程ID
```

**解决方案**:
```bash
# 临时方案：限制CPU使用
# 使用cgroups限制CPU
systemctl set-property library-backend.service CPUQuota=80%

# 永久方案：优化代码
# 1. 优化死循环或无限递归
# 2. 使用异步处理减少阻塞
# 3. 优化算法复杂度
```

---

### 故障5: 磁盘空间不足

**症状**:
- 日志写入失败
- 数据库操作失败
- 应用崩溃

**排查步骤**:
```bash
# 1. 检查磁盘使用情况
df -h

# 2. 查找大文件
du -sh /* | sort -rh | head -10

# 3. 查找日志文件
find /var/log -type f -size +100M
```

**解决方案**:
```bash
# 紧急清理：删除旧日志
find logs/ -name "*.log.*" -mtime +7 -delete

# 清理临时文件
rm -rf /tmp/*

# 压缩旧日志
gzip logs/*.log.2026-*

# 永久方案：配置日志轮转
# 在logback.xml中配置
<rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
    <fileNamePattern>logs/library-backend.%d{yyyy-MM-dd}.log</fileNamePattern>
    <maxHistory>30</maxHistory>
    <totalSizeCap>10GB</totalSizeCap>
</rollingPolicy>
```

---

## 💾 数据备份与恢复

### 1. 数据库备份

#### 自动备份脚本
```bash
#!/bin/bash
# backup_database.sh

BACKUP_DIR="/backup/mysql"
DATE=$(date +%Y%m%d_%H%M%S)
DB_NAME="library"
DB_USER="root"
DB_PASS="your_password"

# 创建备份目录
mkdir -p $BACKUP_DIR

# 执行备份
mysqldump -u$DB_USER -p$DB_PASS $DB_NAME | gzip > $BACKUP_DIR/library_$DATE.sql.gz

# 删除7天前的备份
find $BACKUP_DIR -name "library_*.sql.gz" -mtime +7 -delete

echo "备份完成: library_$DATE.sql.gz"
```

#### 设置定时备份
```bash
# 添加到crontab
crontab -e

# 每天凌晨2点执行备份
0 2 * * * /path/to/backup_database.sh
```

### 2. 数据恢复

```bash
# 恢复最新备份
gunzip < /backup/mysql/library_20260405_020000.sql.gz | mysql -u root -p library

# 恢复到指定时间点（需要开启binlog）
mysqlbinlog --start-datetime="2026-04-05 10:00:00" \
            --stop-datetime="2026-04-05 12:00:00" \
            /var/lib/mysql/mysql-bin.000001 | mysql -u root -p library
```

### 3. 应用配置备份

```bash
# 备份配置文件
tar -czf config_backup_$(date +%Y%m%d).tar.gz \
    application.yml \
    application-prod.yml \
    prometheus.yml \
    alertmanager.yml

# 上传到远程服务器
scp config_backup_*.tar.gz backup@backup-server:/backups/
```

---

## 🔍 性能问题排查

### 1. 慢查询分析

```bash
# 开启慢查询日志
mysql -u root -p -e "SET GLOBAL slow_query_log = 'ON';"
mysql -u root -p -e "SET GLOBAL long_query_time = 1;"

# 分析慢查询
mysqldumpslow -s t -t 10 /var/lib/mysql/slow-query.log

# 使用EXPLAIN分析查询
mysql -u root -p library -e "EXPLAIN SELECT * FROM borrow_record WHERE user_id = 1;"
```

### 2. 线程分析

```bash
# 生成线程转储
jstack <pid> > thread_dump_$(date +%Y%m%d_%H%M%S).txt

# 分析死锁
grep -A 10 "Found one Java-level deadlock" thread_dump_*.txt

# 分析线程状态
grep "java.lang.Thread.State" thread_dump_*.txt | sort | uniq -c
```

### 3. 网络问题排查

```bash
# 检查端口连通性
telnet localhost 8080

# 检查网络延迟
ping -c 10 database-server

# 查看网络连接
netstat -an | grep 8080

# 抓包分析
tcpdump -i any port 8080 -w capture.pcap
```

---

## 🛡️ 安全事件响应

### 1. DDoS攻击

**识别特征**:
- 请求速率异常增高
- 来自大量不同IP
- 服务响应变慢

**应急处理**:
```bash
# 1. 启用IP黑名单
# 在防火墙添加规则
iptables -A INPUT -s <攻击IP> -j DROP

# 2. 限制连接数
iptables -A INPUT -p tcp --dport 8080 -m connlimit --connlimit-above 20 -j REJECT

# 3. 启用CDN防护
# 将流量切换到CDN（如Cloudflare）

# 4. 降级非核心功能
# 临时关闭搜索、统计等功能
```

### 2. SQL注入攻击

**识别特征**:
- 日志中出现异常SQL语句
- 包含 `' OR '1'='1` 等模式

**应急处理**:
```bash
# 1. 立即封禁攻击IP
iptables -A INPUT -s <攻击IP> -j DROP

# 2. 检查数据库是否被篡改
mysql -u root -p -e "SELECT * FROM user ORDER BY updated_at DESC LIMIT 10;"

# 3. 恢复数据（如果被篡改）
# 从最近的备份恢复

# 4. 修复代码漏洞
# 确保所有SQL使用参数化查询
```

### 3. 暴力破解攻击

**识别特征**:
- 大量登录失败记录
- 来自同一IP的频繁请求

**应急处理**:
```bash
# 1. 临时封禁攻击IP
# 已在代码中实现自动封禁机制

# 2. 强制所有用户修改密码
# 发送邮件通知

# 3. 启用验证码
# 在登录页面强制启用验证码

# 4. 启用双因素认证
# 为管理员账户启用2FA
```

---

## 📞 联系人信息

### 技术团队

| 角色 | 姓名 | 电话 | 邮箱 | 职责 |
|------|------|------|------|------|
| **技术负责人** | 张三 | 138-xxxx-xxxx | zhangsan@example.com | 整体协调 |
| **后端负责人** | 李四 | 139-xxxx-xxxx | lisi@example.com | 后端问题 |
| **运维负责人** | 王五 | 137-xxxx-xxxx | wangwu@example.com | 服务器/数据库 |
| **安全负责人** | 赵六 | 136-xxxx-xxxx | zhaoliu@example.com | 安全事件 |

### 值班表

| 时间段 | 值班人员 | 联系方式 |
|--------|----------|----------|
| 周一-周五 9:00-18:00 | 李四 | 139-xxxx-xxxx |
| 周一-周五 18:00-次日9:00 | 王五 | 137-xxxx-xxxx |
| 周六-周日 全天 | 轮值 | 见值班群 |

### 外部支持

| 服务商 | 联系方式 | 服务内容 |
|--------|----------|----------|
| 阿里云 | 95187 | 云服务器支持 |
| MySQL官方 | support@mysql.com | 数据库技术支持 |
| CDN服务商 | 400-xxx-xxxx | CDN防护 |

---

## 📝 故障报告模板

```markdown
# 故障报告

## 基本信息
- **故障时间**: 2026-04-05 14:30:00
- **恢复时间**: 2026-04-05 15:45:00
- **故障级别**: P1
- **影响范围**: 所有用户无法登录

## 故障描述
[详细描述故障现象]

## 根本原因
[分析故障的根本原因]

## 处理过程
1. 14:30 - 收到告警
2. 14:35 - 确认问题
3. 14:40 - 开始处理
4. 15:45 - 问题解决

## 解决方案
[描述如何解决的]

## 预防措施
1. [措施1]
2. [措施2]

## 经验教训
[总结经验教训]

## 后续行动
- [ ] 优化监控告警
- [ ] 更新应急预案
- [ ] 代码修复
```

---

## ✅ 应急演练计划

### 每月演练项目
- [ ] 数据库故障切换演练
- [ ] 服务重启演练
- [ ] 数据恢复演练
- [ ] DDoS攻击应对演练

### 演练记录
| 日期 | 演练项目 | 参与人员 | 结果 | 改进建议 |
|------|----------|----------|------|----------|
| 2026-04-01 | 数据库切换 | 全体 | 成功 | 优化切换脚本 |

---

**最后更新**: 2026-04-05  
**版本**: v1.0  
**维护人**: 技术团队
