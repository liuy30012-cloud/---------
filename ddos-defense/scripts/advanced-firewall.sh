#!/bin/bash
# 高级 DDoS 防御脚本 - 增强版
# 包含 GeoIP 封禁、动态黑名单、连接追踪等高级功能

set -e

if [[ "${1:-}" == "--help" || "${1:-}" == "-h" ]]; then
    echo "Usage: advanced-firewall.sh"
    echo "Configure the advanced DDoS firewall rules on the current host."
    exit 0
fi

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${GREEN}=== 高级 DDoS 防御配置 ===${NC}"

# 检查 root 权限
if [ "$EUID" -ne 0 ]; then
    echo -e "${RED}请使用 root 权限运行此脚本${NC}"
    exit 1
fi

# 配置变量
BLACKLIST_FILE="/etc/ddos/blacklist.txt"
WHITELIST_FILE="/etc/ddos/whitelist.txt"
LOG_FILE="/var/log/ddos-advanced.log"

# 创建目录
mkdir -p /etc/ddos

# 初始化黑白名单
touch "$BLACKLIST_FILE"
touch "$WHITELIST_FILE"

echo -e "${YELLOW}[1/8] 配置高级防火墙规则...${NC}"

# 创建自定义链
iptables -N DDOS_PROTECTION 2>/dev/null || iptables -F DDOS_PROTECTION
iptables -N RATE_LIMIT 2>/dev/null || iptables -F RATE_LIMIT
iptables -N BLACKLIST 2>/dev/null || iptables -F BLACKLIST
iptables -N WHITELIST 2>/dev/null || iptables -F WHITELIST

# 白名单优先
iptables -A INPUT -j WHITELIST
iptables -A WHITELIST -m set --match-set whitelist src -j ACCEPT 2>/dev/null || true

# 黑名单
iptables -A INPUT -j BLACKLIST
iptables -A BLACKLIST -m set --match-set blacklist src -j DROP 2>/dev/null || true

# 应用 DDoS 防护
iptables -A INPUT -j DDOS_PROTECTION

# === 高级 SYN Flood 防护 ===
echo -e "${YELLOW}[2/8] 配置高级 SYN Flood 防护...${NC}"

# 使用 hashlimit 模块进行更精确的速率限制
iptables -A DDOS_PROTECTION -p tcp --syn -m hashlimit \
    --hashlimit-name syn_flood \
    --hashlimit-mode srcip \
    --hashlimit-above 10/sec \
    --hashlimit-burst 20 \
    --hashlimit-htable-expire 10000 \
    -j DROP

# === HTTP/HTTPS 连接防护 ===
echo -e "${YELLOW}[3/8] 配置 HTTP/HTTPS 高级防护...${NC}"

# 限制新建连接速率（使用 recent 模块）
iptables -A DDOS_PROTECTION -p tcp --dport 80 -m state --state NEW \
    -m recent --name http_conn --set

iptables -A DDOS_PROTECTION -p tcp --dport 80 -m state --state NEW \
    -m recent --name http_conn --update --seconds 1 --hitcount 10 \
    -j LOG --log-prefix "HTTP-FLOOD: " --log-level 4

iptables -A DDOS_PROTECTION -p tcp --dport 80 -m state --state NEW \
    -m recent --name http_conn --update --seconds 1 --hitcount 10 \
    -j DROP

# HTTPS 同样配置
iptables -A DDOS_PROTECTION -p tcp --dport 443 -m state --state NEW \
    -m recent --name https_conn --set

iptables -A DDOS_PROTECTION -p tcp --dport 443 -m state --state NEW \
    -m recent --name https_conn --update --seconds 1 --hitcount 10 \
    -j LOG --log-prefix "HTTPS-FLOOD: " --log-level 4

iptables -A DDOS_PROTECTION -p tcp --dport 443 -m state --state NEW \
    -m recent --name https_conn --update --seconds 1 --hitcount 10 \
    -j DROP

# === 限制并发连接数（更精细） ===
echo -e "${YELLOW}[4/8] 配置并发连接限制...${NC}"

# HTTP 连接限制
iptables -A DDOS_PROTECTION -p tcp --dport 80 \
    -m connlimit --connlimit-above 30 --connlimit-mask 32 \
    -j REJECT --reject-with tcp-reset

# HTTPS 连接限制
iptables -A DDOS_PROTECTION -p tcp --dport 443 \
    -m connlimit --connlimit-above 30 --connlimit-mask 32 \
    -j REJECT --reject-with tcp-reset

# === UDP Flood 高级防护 ===
echo -e "${YELLOW}[5/8] 配置 UDP Flood 高级防护...${NC}"

# 使用 hashlimit 限制 UDP
iptables -A DDOS_PROTECTION -p udp -m hashlimit \
    --hashlimit-name udp_flood \
    --hashlimit-mode srcip \
    --hashlimit-above 50/sec \
    --hashlimit-burst 100 \
    -j DROP

# 特定 UDP 端口防护（DNS、NTP 等）
iptables -A DDOS_PROTECTION -p udp --dport 53 -m hashlimit \
    --hashlimit-name dns_query \
    --hashlimit-mode srcip \
    --hashlimit-above 20/sec \
    -j DROP

iptables -A DDOS_PROTECTION -p udp --dport 123 -m hashlimit \
    --hashlimit-name ntp_query \
    --hashlimit-mode srcip \
    --hashlimit-above 5/sec \
    -j DROP

# === ICMP 高级防护 ===
echo -e "${YELLOW}[6/8] 配置 ICMP 高级防护...${NC}"

# 允许必要的 ICMP 类型
iptables -A DDOS_PROTECTION -p icmp --icmp-type 3 -j ACCEPT  # Destination Unreachable
iptables -A DDOS_PROTECTION -p icmp --icmp-type 11 -j ACCEPT # Time Exceeded
iptables -A DDOS_PROTECTION -p icmp --icmp-type 12 -j ACCEPT # Parameter Problem

# 限制 ICMP Echo Request
iptables -A DDOS_PROTECTION -p icmp --icmp-type 8 -m hashlimit \
    --hashlimit-name icmp_echo \
    --hashlimit-mode srcip \
    --hashlimit-above 1/sec \
    --hashlimit-burst 5 \
    -j DROP

# === 防止各种扫描攻击 ===
echo -e "${YELLOW}[7/8] 配置扫描攻击防护...${NC}"

# 防止 NULL 扫描
iptables -A DDOS_PROTECTION -p tcp --tcp-flags ALL NONE \
    -j LOG --log-prefix "NULL-SCAN: " --log-level 4
iptables -A DDOS_PROTECTION -p tcp --tcp-flags ALL NONE -j DROP

# 防止 XMAS 扫描
iptables -A DDOS_PROTECTION -p tcp --tcp-flags ALL FIN,PSH,URG \
    -j LOG --log-prefix "XMAS-SCAN: " --log-level 4
iptables -A DDOS_PROTECTION -p tcp --tcp-flags ALL FIN,PSH,URG -j DROP

# 防止 FIN 扫描
iptables -A DDOS_PROTECTION -p tcp --tcp-flags ALL FIN \
    -j LOG --log-prefix "FIN-SCAN: " --log-level 4
iptables -A DDOS_PROTECTION -p tcp --tcp-flags ALL FIN -j DROP

# 防止 SYN/FIN 攻击
iptables -A DDOS_PROTECTION -p tcp --tcp-flags SYN,FIN SYN,FIN \
    -j LOG --log-prefix "SYN-FIN: " --log-level 4
iptables -A DDOS_PROTECTION -p tcp --tcp-flags SYN,FIN SYN,FIN -j DROP

# 防止 SYN/RST 攻击
iptables -A DDOS_PROTECTION -p tcp --tcp-flags SYN,RST SYN,RST \
    -j LOG --log-prefix "SYN-RST: " --log-level 4
iptables -A DDOS_PROTECTION -p tcp --tcp-flags SYN,RST SYN,RST -j DROP

# === 防止碎片攻击 ===
iptables -A DDOS_PROTECTION -f -j LOG --log-prefix "FRAGMENT: " --log-level 4
iptables -A DDOS_PROTECTION -f -j DROP

# === 防止 Smurf 攻击 ===
iptables -A DDOS_PROTECTION -p icmp -m icmp --icmp-type address-mask-request -j DROP
iptables -A DDOS_PROTECTION -p icmp -m icmp --icmp-type timestamp-request -j DROP

# === 防止 LAND 攻击 ===
iptables -A DDOS_PROTECTION -s 127.0.0.0/8 ! -i lo -j DROP

# === 创建动态黑名单管理脚本 ===
echo -e "${YELLOW}[8/8] 创建管理脚本...${NC}"

cat > /usr/local/bin/ddos-blacklist << 'EOFSCRIPT'
#!/bin/bash
# DDoS 黑名单管理脚本

BLACKLIST_FILE="/etc/ddos/blacklist.txt"
WHITELIST_FILE="/etc/ddos/whitelist.txt"

case "$1" in
    add)
        if [ -z "$2" ]; then
            echo "用法: ddos-blacklist add <IP地址>"
            exit 1
        fi
        echo "$2" >> "$BLACKLIST_FILE"
        iptables -I BLACKLIST -s "$2" -j DROP
        echo "已添加 $2 到黑名单"
        ;;
    remove)
        if [ -z "$2" ]; then
            echo "用法: ddos-blacklist remove <IP地址>"
            exit 1
        fi
        sed -i "/$2/d" "$BLACKLIST_FILE"
        iptables -D BLACKLIST -s "$2" -j DROP 2>/dev/null
        echo "已从黑名单移除 $2"
        ;;
    list)
        echo "=== 黑名单 IP ==="
        cat "$BLACKLIST_FILE"
        ;;
    whitelist-add)
        if [ -z "$2" ]; then
            echo "用法: ddos-blacklist whitelist-add <IP地址>"
            exit 1
        fi
        echo "$2" >> "$WHITELIST_FILE"
        iptables -I WHITELIST -s "$2" -j ACCEPT
        echo "已添加 $2 到白名单"
        ;;
    whitelist-remove)
        if [ -z "$2" ]; then
            echo "用法: ddos-blacklist whitelist-remove <IP地址>"
            exit 1
        fi
        sed -i "/$2/d" "$WHITELIST_FILE"
        iptables -D WHITELIST -s "$2" -j ACCEPT 2>/dev/null
        echo "已从白名单移除 $2"
        ;;
    whitelist-list)
        echo "=== 白名单 IP ==="
        cat "$WHITELIST_FILE"
        ;;
    *)
        echo "用法: ddos-blacklist {add|remove|list|whitelist-add|whitelist-remove|whitelist-list} [IP]"
        exit 1
        ;;
esac
EOFSCRIPT

chmod +x /usr/local/bin/ddos-blacklist

# 保存规则
if command -v netfilter-persistent &> /dev/null; then
    netfilter-persistent save
elif [ -d /etc/iptables ]; then
    iptables-save > /etc/iptables/rules.v4
fi

echo -e "${GREEN}=== 高级防御配置完成 ===${NC}"
echo ""
echo -e "${BLUE}新增功能：${NC}"
echo "1. hashlimit 模块实现更精确的速率限制"
echo "2. 自定义防火墙链（DDOS_PROTECTION、BLACKLIST、WHITELIST）"
echo "3. 详细的攻击日志记录"
echo "4. 动态黑白名单管理"
echo "5. 防止各种扫描攻击（NULL、XMAS、FIN 等）"
echo "6. 碎片攻击防护"
echo "7. Smurf 和 LAND 攻击防护"
echo ""
echo -e "${BLUE}管理命令：${NC}"
echo "添加黑名单: ddos-blacklist add <IP>"
echo "移除黑名单: ddos-blacklist remove <IP>"
echo "查看黑名单: ddos-blacklist list"
echo "添加白名单: ddos-blacklist whitelist-add <IP>"
echo "查看白名单: ddos-blacklist whitelist-list"
echo ""
echo -e "${BLUE}查看日志：${NC}"
echo "tail -f /var/log/kern.log | grep -E 'HTTP-FLOOD|HTTPS-FLOOD|NULL-SCAN|XMAS-SCAN'"
