from __future__ import annotations

from pathlib import Path
from typing import Any

import requests

from .settings import Settings


class BackendImporter:
    def __init__(self, settings: Settings, session: requests.Session | None = None) -> None:
        self.settings = settings
        self.session = session or requests.Session()
        self.token: str | None = None
        self.session.headers.update({
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36'
        })

    def build_login_payload(self) -> dict[str, str]:
        return {
            'studentId': self.settings.admin_username,
            'password': self.settings.admin_password,
        }

    def login(self) -> bool:
        response = self.session.post(
            f'{self.settings.backend_api_base}/auth/login',
            json=self.build_login_payload(),
            timeout=10,
        )
        response.raise_for_status()
        payload = response.json()
        token = payload.get('data', {}).get('token') if payload.get('success') else None
        if not token:
            return False
        self.token = token
        return True

    def import_file(self, excel_file: Path) -> dict[str, Any]:
        if not self.token:
            return {'file': excel_file.name, 'success': False, 'error': 'No token available'}

        headers = {'Authorization': f'Bearer {self.token}'}
        with excel_file.open('rb') as file_handle:
            response = self.session.post(
                f'{self.settings.backend_api_base}/books/import',
                headers=headers,
                files={'file': (excel_file.name, file_handle, 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet')},
                timeout=300,
            )
        response.raise_for_status()
        payload = response.json()
        if not payload.get('success'):
            return {'file': excel_file.name, 'success': False, 'error': payload.get('message', 'Unknown error')}

        result_data = payload.get('data', {})
        return {
            'file': excel_file.name,
            'success': True,
            'successCount': result_data.get('successCount', 0),
            'failureCount': result_data.get('failedCount', 0),
        }
