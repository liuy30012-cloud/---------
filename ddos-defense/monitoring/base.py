"""Shared utilities for DDoS monitoring scripts."""

from __future__ import annotations

import json
import os
import subprocess
import time
from datetime import datetime
from pathlib import Path


class BaseMonitor:
    """Base class with common config I/O, log tailing, iptables and unban logic."""

    def __init__(self, config_path: str):
        self.config_path = config_path
        self.config = self._load_config()
        self.offsets = self._load_json(self.config.get("state_file", ""), {})
        self.banned_until = self._load_json(self.config.get("ban_file", ""), {})
        self._inodes: dict[str, int] = {}

    def _load_config(self) -> dict:
        raise NotImplementedError

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

        current_inode = os.stat(path).st_ino
        previous_inode = self._inodes.get(state_key)
        previous_offset = int(self.offsets.get(state_key, 0))
        file_size = os.path.getsize(path)

        if previous_inode is not None and current_inode != previous_inode:
            self.log(f"Log rotated for {path} (inode changed), resetting offset", "INFO")
            previous_offset = 0
        elif file_size < previous_offset:
            previous_offset = 0

        with open(path, "r", encoding="utf-8", errors="ignore") as handle:
            handle.seek(previous_offset)
            lines = handle.readlines()
            self.offsets[state_key] = handle.tell()

        self._inodes[state_key] = current_inode
        return lines

    def _iptables(self, action: str, ip: str) -> bool:
        command = ["iptables", action, "INPUT", "-s", ip, "-j", "DROP"]
        result = subprocess.run(command, capture_output=True, text=True)
        return result.returncode == 0

    def _unban_expired(self) -> None:
        now = time.time()
        expired = [ip for ip, expiry in self.banned_until.items() if expiry <= now]
        for ip in expired:
            if self.config.get("auto_ban"):
                self._iptables("-D", ip)
            del self.banned_until[ip]
            self.log(f"Expired ban removed for ip={ip}")
