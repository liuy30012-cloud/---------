#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "============================================"
echo "  图书馆定位系统 - 前端开发服务器启动"
echo "  Port: 5173"
echo "  Ctrl+C to stop"
echo "============================================"
echo

cd "$ROOT_DIR/frontend"
exec npm run dev
