#!/bin/bash
# GeoIP 封禁脚本 - 基于地理位置的访问控制

set -e

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${GREEN}=== GeoIP 封禁配置 ===${NC}"

# 检查 root 权限
if [ "$EUID" -ne 0 ]; then
    echo -e "${RED}请使用 root 权限运行${NC}"
    exit 1
fi

# 检测操作系统
if [ -f /etc/os-release ]; then
    . /etc/os-release
    OS=$ID
else
    echo -e "${RED}无法检测操作系统${NC}"
    exit 1
fi

echo -e "${YELLOW}安装 GeoIP 工具...${NC}"

# 安装 GeoIP
if [ "$OS" = "ubuntu" ] || [ "$OS" = "debian" ]; then
    apt-get update
    apt-get install -y xtables-addons-common libtext-csv-xs-perl
elif [ "$OS" = "centos" ] || [ "$OS" = "rhel" ]; then
    yum install -y xtables-addons
fi

# 创建 GeoIP 数据目录
mkdir -p /usr/share/xt_geoip

echo -e "${YELLOW}下载 GeoIP 数据库...${NC}"

# 下载并构建 GeoIP 数据库
cd /tmp
wget -q https://download.db-ip.com/free/dbip-country-lite-$(date +%Y-%m).csv.gz
gunzip dbip-country-lite-*.csv.gz

# 转换数据库格式
if [ -f /usr/lib/xtables-addons/xt_geoip_build ]; then
    /usr/lib/xtables-addons/xt_geoip_build -D /usr/share/xt_geoip dbip-country-lite-*.csv
elif [ -f /usr/libexec/xtables-addons/xt_geoip_build ]; then
    /usr/libexec/xtables-addons/xt_geoip_build -D /usr/share/xt_geoip dbip-country-lite-*.csv
fi

echo -e "${YELLOW}配置 GeoIP 规则...${NC}"

# 创建配置文件
cat > /etc/ddos/geoip-config.conf << 'EOF'
# GeoIP 配置文件
# 格式: BLOCK 或 ALLOW 后跟国家代码（ISO 3166-1 alpha-2）

# 示例：封禁特定国家（取消注释以启用）
# BLOCK CN  # 中国
# BLOCK RU  # 俄罗斯
# BLOCK KP  # 朝鲜

# 示例：仅允许特定国家（取消注释以启用）
# ALLOW US  # 美国
# ALLOW GB  # 英国
# ALLOW DE  # 德国

# 默认策略（ACCEPT 或 DROP）
DEFAULT ACCEPT
EOF

# 创建 GeoIP 管理脚本
cat > /usr/local/bin/geoip-manager << 'EOFSCRIPT'
#!/bin/bash
# GeoIP 管理脚本

CONFIG_FILE="/etc/ddos/geoip-config.conf"
CHAIN_NAME="GEOIP_FILTER"

apply_rules() {
    echo "应用 GeoIP 规则..."

    # 创建或清空链
    iptables -N $CHAIN_NAME 2>/dev/null || iptables -F $CHAIN_NAME

    # 读取配置
    while IFS= read -r line; do
        # 跳过注释和空行
        [[ "$line" =~ ^#.*$ ]] && continue
        [[ -z "$line" ]] && continue

        # 解析规则
        action=$(echo "$line" | awk '{print $1}')
        country=$(echo "$line" | awk '{print $2}')

        if [ "$action" = "BLOCK" ]; then
            iptables -A $CHAIN_NAME -m geoip --src-cc "$country" -j DROP
            echo "已封禁国家: $country"
        elif [ "$action" = "ALLOW" ]; then
            iptables -A $CHAIN_NAME -m geoip --src-cc "$country" -j ACCEPT
            echo "已允许国家: $country"
        elif [ "$action" = "DEFAULT" ]; then
            if [ "$country" = "DROP" ]; then
                iptables -A $CHAIN_NAME -j DROP
                echo "默认策略: DROP"
            else
                iptables -A $CHAIN_NAME -j ACCEPT
                echo "默认策略: ACCEPT"
            fi
        fi
    done < "$CONFIG_FILE"

    # 将链添加到 INPUT
    iptables -D INPUT -j $CHAIN_NAME 2>/dev/null
    iptables -I INPUT -j $CHAIN_NAME

    echo "GeoIP 规则已应用"
}

remove_rules() {
    echo "移除 GeoIP 规则..."
    iptables -D INPUT -j $CHAIN_NAME 2>/dev/null
    iptables -F $CHAIN_NAME 2>/dev/null
    iptables -X $CHAIN_NAME 2>/dev/null
    echo "GeoIP 规则已移除"
}

show_stats() {
    echo "=== GeoIP 统计 ==="
    iptables -L $CHAIN_NAME -n -v 2>/dev/null || echo "未应用规则"
}

case "$1" in
    apply)
        apply_rules
        ;;
    remove)
        remove_rules
        ;;
    reload)
        remove_rules
        apply_rules
        ;;
    stats)
        show_stats
        ;;
    *)
        echo "用法: geoip-manager {apply|remove|reload|stats}"
        exit 1
        ;;
esac
EOFSCRIPT

chmod +x /usr/local/bin/geoip-manager

echo -e "${GREEN}GeoIP 配置完成！${NC}"
echo ""
echo -e "${YELLOW}使用方法：${NC}"
echo "1. 编辑配置文件: nano /etc/ddos/geoip-config.conf"
echo "2. 应用规则: geoip-manager apply"
echo "3. 查看统计: geoip-manager stats"
echo "4. 重载规则: geoip-manager reload"
echo "5. 移除规则: geoip-manager remove"
echo ""
echo -e "${YELLOW}国家代码参考：${NC}"
echo "CN - 中国, US - 美国, RU - 俄罗斯, GB - 英国"
echo "DE - 德国, FR - 法国, JP - 日本, KR - 韩国"
echo ""
echo -e "${YELLOW}注意：${NC}"
echo "- 修改配置后需要运行 'geoip-manager reload' 使其生效"
echo "- 建议先测试，避免封禁自己的 IP"
