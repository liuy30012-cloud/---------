#!/bin/bash
# DDoS 防御防火墙配置脚本
# 针对 MHDDoS 攻击的综合防护

set -e

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${GREEN}=== DDoS 防御防火墙配置脚本 ===${NC}"

# 检查是否为 root 用户
if [ "$EUID" -ne 0 ]; then
    echo -e "${RED}请使用 root 权限运行此脚本${NC}"
    exit 1
fi

# 备份现有规则
echo -e "${YELLOW}备份现有防火墙规则...${NC}"
iptables-save > /root/iptables-backup-$(date +%Y%m%d-%H%M%S).rules

# 清空现有规则
echo -e "${YELLOW}清空现有规则...${NC}"
iptables -F
iptables -X
iptables -t nat -F
iptables -t nat -X
iptables -t mangle -F
iptables -t mangle -X

# 设置默认策略
iptables -P INPUT DROP
iptables -P FORWARD DROP
iptables -P OUTPUT ACCEPT

# 允许本地回环
iptables -A INPUT -i lo -j ACCEPT
iptables -A OUTPUT -o lo -j ACCEPT

# 允许已建立的连接
iptables -A INPUT -m state --state ESTABLISHED,RELATED -j ACCEPT

# === Layer 4 防御 ===

# 1. SYN Flood 防护
echo -e "${YELLOW}配置 SYN Flood 防护...${NC}"
iptables -N syn_flood
iptables -A INPUT -p tcp --syn -j syn_flood
iptables -A syn_flood -m limit --limit 1/s --limit-burst 3 -j RETURN
iptables -A syn_flood -j DROP

# 2. 限制单个 IP 的并发连接数
echo -e "${YELLOW}配置连接数限制...${NC}"
iptables -A INPUT -p tcp --dport 80 -m connlimit --connlimit-above 50 --connlimit-mask 32 -j REJECT --reject-with tcp-reset
iptables -A INPUT -p tcp --dport 443 -m connlimit --connlimit-above 50 --connlimit-mask 32 -j REJECT --reject-with tcp-reset

# 3. 限制新连接速率（防止 HTTP Flood）
echo -e "${YELLOW}配置连接速率限制...${NC}"
iptables -A INPUT -p tcp --dport 80 -m state --state NEW -m recent --set
iptables -A INPUT -p tcp --dport 80 -m state --state NEW -m recent --update --seconds 10 --hitcount 20 -j DROP

iptables -A INPUT -p tcp --dport 443 -m state --state NEW -m recent --set
iptables -A INPUT -p tcp --dport 443 -m state --state NEW -m recent --update --seconds 10 --hitcount 20 -j DROP

# 4. UDP Flood 防护
echo -e "${YELLOW}配置 UDP Flood 防护...${NC}"
iptables -A INPUT -p udp -m limit --limit 10/s --limit-burst 20 -j ACCEPT
iptables -A INPUT -p udp -j DROP

# 5. ICMP Flood 防护
echo -e "${YELLOW}配置 ICMP Flood 防护...${NC}"
iptables -A INPUT -p icmp --icmp-type echo-request -m limit --limit 1/s --limit-burst 2 -j ACCEPT
iptables -A INPUT -p icmp --icmp-type echo-request -j DROP
iptables -A INPUT -p icmp -j ACCEPT

# 6. 防止端口扫描
echo -e "${YELLOW}配置端口扫描防护...${NC}"
iptables -N port-scanning
iptables -A port-scanning -p tcp --tcp-flags SYN,ACK,FIN,RST RST -m limit --limit 1/s --limit-burst 2 -j RETURN
iptables -A port-scanning -j DROP

# 7. 丢弃无效数据包
iptables -A INPUT -m state --state INVALID -j DROP

# 8. 防止 NULL 包攻击
iptables -A INPUT -p tcp --tcp-flags ALL NONE -j DROP

# 9. 防止 XMAS 包攻击
iptables -A INPUT -p tcp --tcp-flags ALL ALL -j DROP

# 10. 防止 FIN 扫描
iptables -A INPUT -p tcp --tcp-flags ALL FIN -j DROP

# 11. 防止 SYN/RST 攻击
iptables -A INPUT -p tcp --tcp-flags SYN,RST SYN,RST -j DROP

# 12. 防止 SYN/FIN 攻击
iptables -A INPUT -p tcp --tcp-flags SYN,FIN SYN,FIN -j DROP

# === 允许必要的服务 ===

# SSH (建议修改默认端口)
echo -e "${YELLOW}允许 SSH 连接...${NC}"
iptables -A INPUT -p tcp --dport 22 -m state --state NEW -m recent --set
iptables -A INPUT -p tcp --dport 22 -m state --state NEW -m recent --update --seconds 60 --hitcount 4 -j DROP
iptables -A INPUT -p tcp --dport 22 -j ACCEPT

# HTTP/HTTPS
echo -e "${YELLOW}允许 HTTP/HTTPS 连接...${NC}"
iptables -A INPUT -p tcp --dport 80 -j ACCEPT
iptables -A INPUT -p tcp --dport 443 -j ACCEPT

# DNS (如果运行 DNS 服务器)
# iptables -A INPUT -p udp --dport 53 -j ACCEPT
# iptables -A INPUT -p tcp --dport 53 -j ACCEPT

# 保存规则
echo -e "${YELLOW}保存防火墙规则...${NC}"
if command -v netfilter-persistent &> /dev/null; then
    netfilter-persistent save
elif command -v iptables-save &> /dev/null; then
    iptables-save > /etc/iptables/rules.v4
fi

# 显示当前规则
echo -e "${GREEN}防火墙规则配置完成！${NC}"
echo -e "${YELLOW}当前规则：${NC}"
iptables -L -n -v --line-numbers

echo -e "${GREEN}=== 配置完成 ===${NC}"
echo -e "${YELLOW}提示：${NC}"
echo -e "1. 请确保 SSH 端口可访问，避免被锁定"
echo -e "2. 根据实际需求调整端口和限制参数"
echo -e "3. 备份文件保存在 /root/iptables-backup-*.rules"
