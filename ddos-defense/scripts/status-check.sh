#!/bin/bash
set -u

echo "=== DDoS Defense Status Check ==="
echo

echo "1. Firewall DROP/REJECT rule count"
iptables -L INPUT -n | grep -c "DROP\|REJECT" || true

echo
echo "2. Current connection statistics"
echo "Total sockets: $(netstat -ntu 2>/dev/null | wc -l)"
echo "Established: $(netstat -ntu 2>/dev/null | grep ESTABLISHED | wc -l)"
echo "SYN_RECV: $(netstat -ntu 2>/dev/null | grep SYN_RECV | wc -l)"

echo
echo "3. Top connection IPs"
netstat -ntu 2>/dev/null | awk '{print $5}' | cut -d: -f1 | sort | uniq -c | sort -rn | head -10

echo
echo "4. Fail2ban status"
if command -v fail2ban-client >/dev/null 2>&1; then
  fail2ban-client status 2>/dev/null | grep "Jail list" || echo "Fail2ban not running"
else
  echo "Fail2ban not installed"
fi

echo
echo "5. Nginx status"
if command -v nginx >/dev/null 2>&1; then
  systemctl is-active nginx 2>/dev/null || echo "Nginx not running"
else
  echo "Nginx not installed"
fi

echo
echo "6. Optional monitoring services"
if systemctl list-unit-files ddos-monitor.service >/dev/null 2>&1; then
  systemctl is-active ddos-monitor 2>/dev/null || echo "ddos-monitor installed but not running (optional)"
else
  echo "ddos-monitor not installed (optional)"
fi

echo
echo "7. System load"
uptime

echo
echo "=== Status check complete ==="
