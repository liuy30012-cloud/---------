#!/bin/bash
# CloudFlare 集成脚本 - 自动同步封禁 IP 到 CloudFlare

set -e

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${GREEN}=== CloudFlare 集成配置 ===${NC}"

# 配置文件
CONFIG_FILE="/etc/ddos/cloudflare.conf"

# 创建配置文件
cat > "$CONFIG_FILE" << 'EOF'
# CloudFlare API 配置
# 获取 API Token: https://dash.cloudflare.com/profile/api-tokens

CF_API_TOKEN=""
CF_ZONE_ID=""
CF_EMAIL=""

# 自动同步设置
AUTO_SYNC=true
SYNC_INTERVAL=300  # 秒
EOF

echo -e "${YELLOW}请编辑配置文件: $CONFIG_FILE${NC}"
echo "需要填入 CloudFlare API Token 和 Zone ID"
echo ""

# 创建 CloudFlare 管理脚本
cat > /usr/local/bin/cf-firewall << 'EOFSCRIPT'
#!/bin/bash
# CloudFlare 防火墙管理脚本

CONFIG_FILE="/etc/ddos/cloudflare.conf"
BLACKLIST_FILE="/etc/ddos/blacklist.txt"
LOG_FILE="/var/log/cf-firewall.log"

# 加载配置
if [ -f "$CONFIG_FILE" ]; then
    source "$CONFIG_FILE"
else
    echo "错误: 配置文件不存在"
    exit 1
fi

# 检查配置
if [ -z "$CF_API_TOKEN" ] || [ -z "$CF_ZONE_ID" ]; then
    echo "错误: 请配置 CloudFlare API Token 和 Zone ID"
    exit 1
fi

log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a "$LOG_FILE"
}

# 封禁 IP
ban_ip() {
    local ip=$1
    local note=${2:-"DDoS Protection"}

    log "封禁 IP: $ip"

    response=$(curl -s -X POST "https://api.cloudflare.com/client/v4/zones/$CF_ZONE_ID/firewall/access_rules/rules" \
        -H "Authorization: Bearer $CF_API_TOKEN" \
        -H "Content-Type: application/json" \
        --data "{
            \"mode\": \"block\",
            \"configuration\": {
                \"target\": \"ip\",
                \"value\": \"$ip\"
            },
            \"notes\": \"$note\"
        }")

    if echo "$response" | grep -q '"success":true'; then
        log "成功封禁 IP: $ip"
        return 0
    else
        log "封禁失败: $ip - $response"
        return 1
    fi
}

# 解封 IP
unban_ip() {
    local ip=$1

    log "查找 IP 规则: $ip"

    # 获取规则 ID
    rules=$(curl -s -X GET "https://api.cloudflare.com/client/v4/zones/$CF_ZONE_ID/firewall/access_rules/rules?configuration.value=$ip" \
        -H "Authorization: Bearer $CF_API_TOKEN" \
        -H "Content-Type: application/json")

    rule_id=$(echo "$rules" | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)

    if [ -z "$rule_id" ]; then
        log "未找到 IP 规则: $ip"
        return 1
    fi

    log "解封 IP: $ip (规则ID: $rule_id)"

    response=$(curl -s -X DELETE "https://api.cloudflare.com/client/v4/zones/$CF_ZONE_ID/firewall/access_rules/rules/$rule_id" \
        -H "Authorization: Bearer $CF_API_TOKEN" \
        -H "Content-Type: application/json")

    if echo "$response" | grep -q '"success":true'; then
        log "成功解封 IP: $ip"
        return 0
    else
        log "解封失败: $ip - $response"
        return 1
    fi
}

# 同步本地黑名单到 CloudFlare
sync_blacklist() {
    log "开始同步黑名单到 CloudFlare..."

    if [ ! -f "$BLACKLIST_FILE" ]; then
        log "黑名单文件不存在"
        return 1
    fi

    local count=0
    while IFS= read -r line; do
        # 跳过注释和空行
        [[ "$line" =~ ^#.*$ ]] && continue
        [[ -z "$line" ]] && continue

        # 提取 IP
        ip=$(echo "$line" | awk '{print $1}')

        # 验证 IP 格式
        if [[ $ip =~ ^[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}$ ]]; then
            ban_ip "$ip" "Auto-sync from local blacklist"
            ((count++))
            sleep 1  # 避免 API 速率限制
        fi
    done < "$BLACKLIST_FILE"

    log "同步完成，共处理 $count 个 IP"
}

# 列出 CloudFlare 规则
list_rules() {
    log "获取 CloudFlare 防火墙规则..."

    response=$(curl -s -X GET "https://api.cloudflare.com/client/v4/zones/$CF_ZONE_ID/firewall/access_rules/rules?per_page=100" \
        -H "Authorization: Bearer $CF_API_TOKEN" \
        -H "Content-Type: application/json")

    echo "$response" | python3 -m json.tool 2>/dev/null || echo "$response"
}

# 启用 Under Attack Mode
under_attack_on() {
    log "启用 Under Attack Mode..."

    response=$(curl -s -X PATCH "https://api.cloudflare.com/client/v4/zones/$CF_ZONE_ID/settings/security_level" \
        -H "Authorization: Bearer $CF_API_TOKEN" \
        -H "Content-Type: application/json" \
        --data '{"value":"under_attack"}')

    if echo "$response" | grep -q '"success":true'; then
        log "Under Attack Mode 已启用"
    else
        log "启用失败: $response"
    fi
}

# 禁用 Under Attack Mode
under_attack_off() {
    log "禁用 Under Attack Mode..."

    response=$(curl -s -X PATCH "https://api.cloudflare.com/client/v4/zones/$CF_ZONE_ID/settings/security_level" \
        -H "Authorization: Bearer $CF_API_TOKEN" \
        -H "Content-Type: application/json" \
        --data '{"value":"medium"}')

    if echo "$response" | grep -q '"success":true'; then
        log "Under Attack Mode 已禁用"
    else
        log "禁用失败: $response"
    fi
}

# 获取分析数据
get_analytics() {
    log "获取 CloudFlare 分析数据..."

    since=$(date -u -d '1 hour ago' '+%Y-%m-%dT%H:%M:%SZ')

    response=$(curl -s -X GET "https://api.cloudflare.com/client/v4/zones/$CF_ZONE_ID/analytics/dashboard?since=$since" \
        -H "Authorization: Bearer $CF_API_TOKEN" \
        -H "Content-Type: application/json")

    echo "$response" | python3 -m json.tool 2>/dev/null || echo "$response"
}

case "$1" in
    ban)
        if [ -z "$2" ]; then
            echo "用法: cf-firewall ban <IP> [备注]"
            exit 1
        fi
        ban_ip "$2" "$3"
        ;;
    unban)
        if [ -z "$2" ]; then
            echo "用法: cf-firewall unban <IP>"
            exit 1
        fi
        unban_ip "$2"
        ;;
    sync)
        sync_blacklist
        ;;
    list)
        list_rules
        ;;
    under-attack-on)
        under_attack_on
        ;;
    under-attack-off)
        under_attack_off
        ;;
    analytics)
        get_analytics
        ;;
    *)
        echo "CloudFlare 防火墙管理工具"
        echo ""
        echo "用法: cf-firewall {ban|unban|sync|list|under-attack-on|under-attack-off|analytics}"
        echo ""
        echo "命令:"
        echo "  ban <IP> [备注]      - 封禁 IP"
        echo "  unban <IP>           - 解封 IP"
        echo "  sync                 - 同步本地黑名单到 CloudFlare"
        echo "  list                 - 列出所有规则"
        echo "  under-attack-on      - 启用 Under Attack Mode"
        echo "  under-attack-off     - 禁用 Under Attack Mode"
        echo "  analytics            - 查看分析数据"
        exit 1
        ;;
esac
EOFSCRIPT

chmod +x /usr/local/bin/cf-firewall

# 创建自动同步服务
cat > /etc/systemd/system/cf-sync.service << 'EOF'
[Unit]
Description=CloudFlare Blacklist Sync Service
After=network.target

[Service]
Type=simple
ExecStart=/usr/local/bin/cf-firewall sync
Restart=on-failure
RestartSec=300

[Install]
WantedBy=multi-user.target
EOF

cat > /etc/systemd/system/cf-sync.timer << 'EOF'
[Unit]
Description=CloudFlare Blacklist Sync Timer

[Timer]
OnBootSec=5min
OnUnitActiveSec=5min

[Install]
WantedBy=timers.target
EOF

echo -e "${GREEN}CloudFlare 集成配置完成！${NC}"
echo ""
echo -e "${YELLOW}配置步骤：${NC}"
echo "1. 编辑配置文件: nano /etc/ddos/cloudflare.conf"
echo "2. 填入 CloudFlare API Token 和 Zone ID"
echo "3. 测试连接: cf-firewall list"
echo ""
echo -e "${YELLOW}使用方法：${NC}"
echo "封禁 IP: cf-firewall ban <IP>"
echo "解封 IP: cf-firewall unban <IP>"
echo "同步黑名单: cf-firewall sync"
echo "启用 Under Attack: cf-firewall under-attack-on"
echo "查看分析: cf-firewall analytics"
echo ""
echo -e "${YELLOW}启用自动同步：${NC}"
echo "systemctl enable cf-sync.timer"
echo "systemctl start cf-sync.timer"
