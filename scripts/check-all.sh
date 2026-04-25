#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

echo "==> Backend tests"
(
  cd "$repo_root/backend"
  ./mvnw test
)

echo "==> Frontend typecheck/build/tests"
(
  cd "$repo_root/frontend"
  npm run build
  npx vue-tsc --noEmit
  npm run test:run
)

echo "==> Book import tool tests"
(
  cd "$repo_root/book-import-tool"
  python -m pytest
)

echo "==> DDoS defense shell syntax"
(
  cd "$repo_root/ddos-defense"
  bash -n scripts/*.sh
)
