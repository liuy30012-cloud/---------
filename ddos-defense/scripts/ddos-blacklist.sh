#!/bin/bash
# DDoS 黑名单管理脚本

BLACKLIST_FILE="/etc/ddos/blacklist.txt"
WHITELIST_FILE="/etc/ddos/whitelist.txt"

if [[ "${1:-}" == "--help" || "${1:-}" == "-h" ]]; then
    echo "Usage: ddos-blacklist.sh <add|remove|list|whitelist> [IP]"
    echo "Manage the blacklist and whitelist used by the DDoS firewall rules."
    exit 0
fi

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
