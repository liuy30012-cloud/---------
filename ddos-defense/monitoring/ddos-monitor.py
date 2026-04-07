#!/usr/bin/env python3
"""
Optional 429/limit_req monitor.

Default mode is observe-only (`auto_ban = false`). If you turn on auto_ban,
review the log paths, thresholds, whitelist and unban path first.
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


ACCESS_RE = re.compile(r'^(?P<ip>\S+) .* "(?P<method>[A-Z]+) .*" (?P<status>\d{3}) ')
ERROR_RE = re.compile(r'client:\s*(?P<ip>[^,]+)')


class Monitor:
    def __init__(self, config_path: str):
        self.config_path = config_path
        self.config = self._load_config()
        self.offsets = self._load_json(self.config["state_file"], {})
        self.banned_until = self._load_json(self.config["ban_file"], {})
        self.events: dict[str, deque[float]] = defaultdict(deque)
        self.limit_req_events: dict[str, deque[float]] = defaultdict(deque)

    def _load_config(self) -> dict:
        default_config = {
            "access_log": "/var/log/nginx/library-api-access.log",
            "error_log": "/var/log/nginx/library-api-error.log",
            "state_file": "/var/lib/library-ddos-monitor/state.json",
            "ban_file": "/var/lib/library-ddos-monitor/bans.json",
            "log_file": "/var/log/ddos-monitor.log",
            "check_interval": 5,
            "window_seconds": 300,
            "status_429_threshold": 40,
            "limit_req_threshold": 10,
            "ban_duration": 1800,
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

    def _prune_events(self) -> None:
        cutoff = time.time() - int(self.config["window_seconds"])
        for bucket in (self.events, self.limit_req_events):
            stale_keys = []
            for ip, timestamps in bucket.items():
                while timestamps and timestamps[0] < cutoff:
                    timestamps.popleft()
                if not timestamps:
                    stale_keys.append(ip)
            for ip in stale_keys:
                del bucket[ip]

    def _consume_logs(self) -> None:
        now = time.time()

        for line in self._tail_incremental(self.config["access_log"], "access_log"):
            match = ACCESS_RE.match(line)
            if not match:
                continue
            ip = match.group("ip")
            if ip in self.config["whitelist"]:
                continue
            if match.group("status") == "429":
                self.events[ip].append(now)

        for line in self._tail_incremental(self.config["error_log"], "error_log"):
            if "limiting requests" not in line:
                continue
            match = ERROR_RE.search(line)
            if not match:
                continue
            ip = match.group("ip").strip()
            if ip in self.config["whitelist"]:
                continue
            self.limit_req_events[ip].append(now)

    def _iptables(self, action: str, ip: str) -> bool:
        command = ["iptables", action, "INPUT", "-s", ip, "-j", "DROP"]
        result = subprocess.run(command, capture_output=True, text=True)
        return result.returncode == 0

    def _apply_or_log_ban(self, ip: str, reason: str) -> None:
        if ip in self.config["whitelist"]:
            return

        expiry = time.time() + int(self.config["ban_duration"])
        self.banned_until[ip] = expiry

        if not self.config["auto_ban"]:
            self.log(f"Observe-only hit: ip={ip} reason={reason}", "WARNING")
            return

        if self._iptables("-I", ip):
            self.log(f"Banned ip={ip} reason={reason}", "WARNING")
        else:
            self.log(f"Failed to ban ip={ip} reason={reason}", "ERROR")

    def _unban_expired(self) -> None:
        now = time.time()
        expired = [ip for ip, expiry in self.banned_until.items() if expiry <= now]
        for ip in expired:
            if self.config["auto_ban"]:
                self._iptables("-D", ip)
            del self.banned_until[ip]
            self.log(f"Expired ban removed for ip={ip}")

    def _detect(self) -> None:
        for ip, timestamps in self.events.items():
            if len(timestamps) >= int(self.config["status_429_threshold"]):
                self._apply_or_log_ban(ip, f"too many 429 responses ({len(timestamps)})")

        for ip, timestamps in self.limit_req_events.items():
            if len(timestamps) >= int(self.config["limit_req_threshold"]):
                self._apply_or_log_ban(ip, f"too many nginx limit_req events ({len(timestamps)})")

    def run(self) -> None:
        self.log(f"Monitor started with auto_ban={self.config['auto_ban']}")
        while True:
            self._consume_logs()
            self._prune_events()
            self._unban_expired()
            self._detect()
            self._save_json(self.config["state_file"], self.offsets)
            self._save_json(self.config["ban_file"], self.banned_until)
            time.sleep(int(self.config["check_interval"]))


def main() -> None:
    config_path = sys.argv[1] if len(sys.argv) > 1 else "monitor-config.json"
    monitor = Monitor(config_path)
    monitor.run()


if __name__ == "__main__":
    main()
