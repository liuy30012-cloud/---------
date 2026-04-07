#!/usr/bin/env python3
"""
Optional higher-level traffic analyzer.

This script is not enabled by deployment by default. Keep `auto_ban = false`
until log paths, thresholds and unban behavior are verified in your environment.
"""

from __future__ import annotations

import json
import os
import re
import subprocess
import sys
import time
from collections import defaultdict, deque
from datetime import datetime
from pathlib import Path


ACCESS_RE = re.compile(r'^(?P<ip>\S+) .* "(?P<method>[A-Z]+) (?P<path>\S+) .*" (?P<status>\d{3}) .* "(?P<referer>[^"]*)" "(?P<ua>[^"]*)"')


class IntelligentDefense:
    def __init__(self, config_path: str):
        self.config_path = config_path
        self.config = self._load_config()
        self.offsets = self._load_json(self.config["state_file"], {})
        self.banned_until = self._load_json(self.config["ban_file"], {})
        self.requests: dict[str, deque[float]] = defaultdict(deque)
        self.status_429: dict[str, deque[float]] = defaultdict(deque)
        self.unique_paths: dict[str, set[str]] = defaultdict(set)
        self.user_agents: dict[str, set[str]] = defaultdict(set)

    def _load_config(self) -> dict:
        default_config = {
            "access_log": "/var/log/nginx/library-api-access.log",
            "state_file": "/var/lib/library-ddos-monitor/intelligent-state.json",
            "ban_file": "/var/lib/library-ddos-monitor/intelligent-bans.json",
            "log_file": "/var/log/intelligent-ddos.log",
            "check_interval": 5,
            "window_seconds": 300,
            "request_threshold": 300,
            "path_spread_threshold": 80,
            "status_429_threshold": 50,
            "threat_threshold": 70,
            "ban_duration": 3600,
            "auto_ban": False,
            "whitelist": ["127.0.0.1", "::1"],
        }
        if os.path.exists(self.config_path):
            with open(self.config_path, "r", encoding="utf-8") as handle:
                default_config.update(json.load(handle))
        return default_config

    def _load_json(self, path: str, default):
        try:
            with open(path, "r", encoding="utf-8") as handle:
                return json.load(handle)
        except FileNotFoundError:
            return default

    def _save_json(self, path: str, payload) -> None:
        Path(path).parent.mkdir(parents=True, exist_ok=True)
        with open(path, "w", encoding="utf-8") as handle:
            json.dump(payload, handle, indent=2, sort_keys=True)

    def log(self, message: str, level: str = "INFO") -> None:
        entry = f"[{datetime.now().strftime('%Y-%m-%d %H:%M:%S')}] [{level}] {message}"
        print(entry)
        try:
            Path(self.config["log_file"]).parent.mkdir(parents=True, exist_ok=True)
            with open(self.config["log_file"], "a", encoding="utf-8") as handle:
                handle.write(entry + "\n")
        except OSError:
            pass

    def _tail_incremental(self, path: str, state_key: str) -> list[str]:
        if not os.path.exists(path):
            return []

        previous_offset = int(self.offsets.get(state_key, 0))
        file_size = os.path.getsize(path)
        if file_size < previous_offset:
            previous_offset = 0

        with open(path, "r", encoding="utf-8", errors="ignore") as handle:
            handle.seek(previous_offset)
            lines = handle.readlines()
            self.offsets[state_key] = handle.tell()
        return lines

    def _consume_logs(self) -> None:
        now = time.time()
        for line in self._tail_incremental(self.config["access_log"], "access_log"):
            match = ACCESS_RE.match(line)
            if not match:
                continue

            ip = match.group("ip")
            if ip in self.config["whitelist"]:
                continue

            self.requests[ip].append(now)
            if match.group("status") == "429":
                self.status_429[ip].append(now)
            self.unique_paths[ip].add(match.group("path"))
            self.user_agents[ip].add(match.group("ua"))

    def _prune(self) -> None:
        cutoff = time.time() - int(self.config["window_seconds"])
        for bucket in (self.requests, self.status_429):
            stale = []
            for ip, timestamps in bucket.items():
                while timestamps and timestamps[0] < cutoff:
                    timestamps.popleft()
                if not timestamps:
                    stale.append(ip)
            for ip in stale:
                del bucket[ip]

    def _threat_score(self, ip: str) -> int:
        score = 0
        request_count = len(self.requests.get(ip, ()))
        status_429_count = len(self.status_429.get(ip, ()))
        path_count = len(self.unique_paths.get(ip, ()))
        ua_count = len(self.user_agents.get(ip, ()))

        if request_count >= int(self.config["request_threshold"]):
            score += 40
        elif request_count >= int(self.config["request_threshold"]) // 2:
            score += 20

        if status_429_count >= int(self.config["status_429_threshold"]):
            score += 35
        elif status_429_count >= int(self.config["status_429_threshold"]) // 2:
            score += 15

        if path_count >= int(self.config["path_spread_threshold"]):
            score += 20

        if ua_count <= 1:
            score += 5

        return min(score, 100)

    def _iptables(self, action: str, ip: str) -> bool:
        result = subprocess.run(["iptables", action, "INPUT", "-s", ip, "-j", "DROP"], capture_output=True, text=True)
        return result.returncode == 0

    def _ban(self, ip: str, score: int) -> None:
        expiry = time.time() + int(self.config["ban_duration"])
        self.banned_until[ip] = expiry

        if not self.config["auto_ban"]:
            self.log(f"Observe-only hit: ip={ip} score={score}", "WARNING")
            return

        if self._iptables("-I", ip):
            self.log(f"Banned ip={ip} score={score}", "WARNING")
        else:
            self.log(f"Failed to ban ip={ip} score={score}", "ERROR")

    def _unban_expired(self) -> None:
        now = time.time()
        expired = [ip for ip, expiry in self.banned_until.items() if expiry <= now]
        for ip in expired:
            if self.config["auto_ban"]:
                self._iptables("-D", ip)
            del self.banned_until[ip]
            self.log(f"Expired ban removed for ip={ip}")

    def run(self) -> None:
        self.log(f"Intelligent defense started with auto_ban={self.config['auto_ban']}")
        while True:
            self._consume_logs()
            self._prune()
            self._unban_expired()

            for ip in list(self.requests.keys()):
                score = self._threat_score(ip)
                if score >= int(self.config["threat_threshold"]):
                    self._ban(ip, score)

            self._save_json(self.config["state_file"], self.offsets)
            self._save_json(self.config["ban_file"], self.banned_until)
            time.sleep(int(self.config["check_interval"]))


def main() -> None:
    config_path = sys.argv[1] if len(sys.argv) > 1 else "intelligent-config.json"
    IntelligentDefense(config_path).run()


if __name__ == "__main__":
    main()
