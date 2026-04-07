#!/bin/bash
set -euo pipefail

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${GREEN}================================${NC}"
echo -e "${GREEN}  DDoS Defense Deployment${NC}"
echo -e "${GREEN}================================${NC}"
echo

if [ "${EUID}" -ne 0 ]; then
  echo -e "${RED}Please run this script as root.${NC}"
  exit 1
fi

if [ -f /etc/os-release ]; then
  . /etc/os-release
  OS="${ID}"
else
  echo -e "${RED}Unable to detect operating system.${NC}"
  exit 1
fi

INSTALL_FIREWALL=false
INSTALL_OPTIMIZE=false
INSTALL_NGINX=false
INSTALL_FAIL2BAN=false
INSTALL_MONITOR=false

echo "Select deployment scope:"
echo "1) Recommended baseline (firewall + system tuning + nginx + fail2ban)"
echo "2) Firewall only"
echo "3) System tuning only"
echo "4) Nginx only"
echo "5) Fail2ban only"
echo "6) Optional monitoring scripts only"
read -r -p "Choice (1-6): " choice

case "$choice" in
  1)
    INSTALL_FIREWALL=true
    INSTALL_OPTIMIZE=true
    INSTALL_NGINX=true
    INSTALL_FAIL2BAN=true
    ;;
  2) INSTALL_FIREWALL=true ;;
  3) INSTALL_OPTIMIZE=true ;;
  4) INSTALL_NGINX=true ;;
  5) INSTALL_FAIL2BAN=true ;;
  6) INSTALL_MONITOR=true ;;
  *)
    echo -e "${RED}Invalid choice.${NC}"
    exit 1
    ;;
esac

if [ "${INSTALL_OPTIMIZE}" = true ]; then
  echo -e "${GREEN}[1/5] Applying system tuning...${NC}"
  bash scripts/system-optimize.sh
  echo
fi

if [ "${INSTALL_FIREWALL}" = true ]; then
  echo -e "${GREEN}[2/5] Configuring firewall...${NC}"
  bash scripts/firewall-setup.sh
  echo
fi

if [ "${INSTALL_NGINX}" = true ]; then
  echo -e "${GREEN}[3/5] Installing Nginx template...${NC}"

  if ! command -v nginx >/dev/null 2>&1; then
    if [ "${OS}" = "ubuntu" ] || [ "${OS}" = "debian" ]; then
      apt-get update
      apt-get install -y nginx
    elif [ "${OS}" = "centos" ] || [ "${OS}" = "rhel" ]; then
      yum install -y nginx
    fi
  fi

  cp configs/nginx/ddos-protection.conf /etc/nginx/conf.d/library-api-ddos.conf
  echo -e "${YELLOW}Update upstream and set_real_ip_from entries before enabling the site.${NC}"
  echo -e "${YELLOW}Validate with: nginx -t${NC}"
  echo
fi

if [ "${INSTALL_FAIL2BAN}" = true ]; then
  echo -e "${GREEN}[4/5] Installing Fail2ban rules...${NC}"

  if ! command -v fail2ban-client >/dev/null 2>&1; then
    if [ "${OS}" = "ubuntu" ] || [ "${OS}" = "debian" ]; then
      apt-get update
      apt-get install -y fail2ban
    elif [ "${OS}" = "centos" ] || [ "${OS}" = "rhel" ]; then
      yum install -y epel-release
      yum install -y fail2ban
    fi
  fi

  cp configs/fail2ban/jail.local /etc/fail2ban/jail.local
  cp configs/fail2ban/http-get-dos.conf /etc/fail2ban/filter.d/http-get-dos.conf
  cp configs/fail2ban/http-post-dos.conf /etc/fail2ban/filter.d/http-post-dos.conf
  cp configs/fail2ban/nginx-limit-req.conf /etc/fail2ban/filter.d/nginx-limit-req.conf

  systemctl enable fail2ban
  systemctl restart fail2ban
  echo
fi

if [ "${INSTALL_MONITOR}" = true ]; then
  echo -e "${GREEN}[5/5] Installing optional monitoring scripts...${NC}"
  install -d /opt/library-ddos-monitor
  cp monitoring/ddos-monitor.py /opt/library-ddos-monitor/
  cp monitoring/intelligent-defense.py /opt/library-ddos-monitor/
  cp monitoring/monitor-config.json /opt/library-ddos-monitor/
  cp monitoring/intelligent-config.json /opt/library-ddos-monitor/

  cat >/etc/systemd/system/ddos-monitor.service <<'EOF'
[Unit]
Description=Optional DDoS Monitoring Service
After=network.target

[Service]
Type=simple
User=root
WorkingDirectory=/opt/library-ddos-monitor
ExecStart=/usr/bin/python3 /opt/library-ddos-monitor/ddos-monitor.py /opt/library-ddos-monitor/monitor-config.json
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

  systemctl daemon-reload
  echo -e "${YELLOW}Monitoring service installed but not enabled by default.${NC}"
  echo -e "${YELLOW}Review config first, then run: systemctl enable --now ddos-monitor${NC}"
  echo
fi

echo -e "${GREEN}Deployment complete.${NC}"
echo "Next steps:"
echo "1. Edit /etc/nginx/conf.d/library-api-ddos.conf for upstream and trusted proxy CIDRs."
echo "2. Run nginx -t and reload nginx after validation."
echo "3. Confirm fail2ban jails with fail2ban-client status."
echo "4. Keep optional Python monitoring disabled unless you have reviewed the config and unban path."
