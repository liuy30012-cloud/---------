#!/bin/bash
# 系统参数优化脚本 - 针对 DDoS 防御
# 优化 Linux 内核参数以提高抗 DDoS 能力

set -e

if [[ "${1:-}" == "--help" || "${1:-}" == "-h" ]]; then
    echo "Usage: system-optimize.sh"
    echo "Apply the recommended Linux kernel tuning for DDoS defense."
    exit 0
fi

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${GREEN}=== 系统参数优化脚本 ===${NC}"

# 检查 root 权限
if [ "$EUID" -ne 0 ]; then
    echo -e "${RED}请使用 root 权限运行此脚本${NC}"
    exit 1
fi

# 备份原始配置
BACKUP_FILE="/etc/sysctl.conf.backup-$(date +%Y%m%d-%H%M%S)"
echo -e "${YELLOW}备份原始配置到: ${BACKUP_FILE}${NC}"
cp /etc/sysctl.conf "$BACKUP_FILE"

# 创建优化配置
SYSCTL_CONF="/etc/sysctl.d/99-ddos-protection.conf"

cat > "$SYSCTL_CONF" << 'EOF'
# DDoS 防御系统参数优化
# 生成时间: $(date)

# === TCP/IP 栈优化 ===

# 启用 SYN Cookies (防止 SYN Flood)
net.ipv4.tcp_syncookies = 1

# 增加 SYN 队列长度
net.ipv4.tcp_max_syn_backlog = 8192
net.core.netdev_max_backlog = 5000

# 减少 SYN-ACK 重试次数
net.ipv4.tcp_synack_retries = 2
net.ipv4.tcp_syn_retries = 2

# 加快 TIME_WAIT 套接字回收
net.ipv4.tcp_fin_timeout = 15
net.ipv4.tcp_tw_reuse = 1

# 增加本地端口范围
net.ipv4.ip_local_port_range = 1024 65535

# 增加 TCP 连接跟踪表大小
net.netfilter.nf_conntrack_max = 1000000
net.nf_conntrack_max = 1000000

# === 内存和缓冲区优化 ===

# 增加接收/发送缓冲区
net.core.rmem_default = 262144
net.core.rmem_max = 16777216
net.core.wmem_default = 262144
net.core.wmem_max = 16777216

# TCP 缓冲区优化
net.ipv4.tcp_rmem = 4096 87380 16777216
net.ipv4.tcp_wmem = 4096 65536 16777216
net.ipv4.tcp_mem = 786432 1048576 26777216

# === 连接跟踪优化 ===

# 连接跟踪超时设置
net.netfilter.nf_conntrack_tcp_timeout_established = 600
net.netfilter.nf_conntrack_tcp_timeout_time_wait = 30
net.netfilter.nf_conntrack_tcp_timeout_close_wait = 15
net.netfilter.nf_conntrack_tcp_timeout_fin_wait = 30

# === ICMP 防护 ===

# 忽略 ICMP 广播请求
net.ipv4.icmp_echo_ignore_broadcasts = 1

# 忽略错误的 ICMP 响应
net.ipv4.icmp_ignore_bogus_error_responses = 1

# 限制 ICMP 速率
net.ipv4.icmp_ratelimit = 100
net.ipv4.icmp_ratemask = 6168

# === IP 转发和路由 ===

# 禁用 IP 转发（如果不是路由器）
net.ipv4.ip_forward = 0
net.ipv6.conf.all.forwarding = 0

# 启用反向路径过滤（防止 IP 欺骗）
net.ipv4.conf.all.rp_filter = 1
net.ipv4.conf.default.rp_filter = 1

# 禁用源路由
net.ipv4.conf.all.accept_source_route = 0
net.ipv4.conf.default.accept_source_route = 0

# 禁用 ICMP 重定向
net.ipv4.conf.all.accept_redirects = 0
net.ipv4.conf.default.accept_redirects = 0
net.ipv4.conf.all.secure_redirects = 0
net.ipv4.conf.default.secure_redirects = 0
net.ipv4.conf.all.send_redirects = 0
net.ipv4.conf.default.send_redirects = 0

# === TCP 性能优化 ===

# 启用 TCP Fast Open
net.ipv4.tcp_fastopen = 3

# 启用 TCP 窗口缩放
net.ipv4.tcp_window_scaling = 1

# 启用 TCP 时间戳
net.ipv4.tcp_timestamps = 1

# 启用选择性确认
net.ipv4.tcp_sack = 1

# TCP keepalive 设置
net.ipv4.tcp_keepalive_time = 600
net.ipv4.tcp_keepalive_intvl = 60
net.ipv4.tcp_keepalive_probes = 3

# === 文件描述符限制 ===

# 增加文件描述符限制
fs.file-max = 2097152

# === 其他安全设置 ===

# 禁用 IPv6（如果不使用）
# net.ipv6.conf.all.disable_ipv6 = 1
# net.ipv6.conf.default.disable_ipv6 = 1

# 记录可疑数据包
net.ipv4.conf.all.log_martians = 1
net.ipv4.conf.default.log_martians = 1

# 增加 ARP 缓存大小
net.ipv4.neigh.default.gc_thresh1 = 1024
net.ipv4.neigh.default.gc_thresh2 = 2048
net.ipv4.neigh.default.gc_thresh3 = 4096

# 虚拟内存优化
vm.swappiness = 10
vm.dirty_ratio = 15
vm.dirty_background_ratio = 5

EOF

echo -e "${YELLOW}应用系统参数...${NC}"
sysctl -p "$SYSCTL_CONF"

# 优化文件描述符限制
echo -e "${YELLOW}配置文件描述符限制...${NC}"
cat >> /etc/security/limits.conf << 'EOF'

# DDoS 防御 - 文件描述符限制
* soft nofile 1000000
* hard nofile 1000000
root soft nofile 1000000
root hard nofile 1000000

# 进程数限制
* soft nproc 100000
* hard nproc 100000

EOF

# 配置 systemd 限制
if [ -d /etc/systemd/system.conf.d ]; then
    mkdir -p /etc/systemd/system.conf.d
    cat > /etc/systemd/system.conf.d/limits.conf << 'EOF'
[Manager]
DefaultLimitNOFILE=1000000
DefaultLimitNPROC=100000
EOF
fi

# 加载 conntrack 模块
echo -e "${YELLOW}加载 conntrack 模块...${NC}"
modprobe nf_conntrack
echo "nf_conntrack" >> /etc/modules-load.d/nf_conntrack.conf

# 显示当前设置
echo -e "${GREEN}=== 优化完成 ===${NC}"
echo -e "${YELLOW}关键参数当前值：${NC}"
echo "SYN Cookies: $(sysctl -n net.ipv4.tcp_syncookies)"
echo "SYN Backlog: $(sysctl -n net.ipv4.tcp_max_syn_backlog)"
echo "Conntrack Max: $(sysctl -n net.nf_conntrack_max 2>/dev/null || echo 'N/A')"
echo "File Max: $(sysctl -n fs.file-max)"

echo -e "${GREEN}系统参数优化完成！${NC}"
echo -e "${YELLOW}建议重启系统以确保所有设置生效${NC}"
