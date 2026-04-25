from __future__ import annotations

import os
from dataclasses import dataclass
from pathlib import Path

from dotenv import load_dotenv


@dataclass(frozen=True)
class Settings:
    base_dir: Path
    douban_api_base: str
    request_interval_min: int
    request_interval_max: int
    max_retries: int
    batch_size: int
    total_books: int
    excel_batch_size: int
    backend_api_base: str
    admin_username: str
    admin_password: str
    max_concurrent_imports: int
    default_location: str
    default_status: str
    default_language: str
    default_availability: str
    default_circulation_policy: str
    default_total_copies: int

    @property
    def data_dir(self) -> Path:
        return self.base_dir / 'data'

    @property
    def raw_data_dir(self) -> Path:
        return self.data_dir / 'raw'

    @property
    def processed_data_dir(self) -> Path:
        return self.data_dir / 'processed'

    @property
    def logs_dir(self) -> Path:
        return self.base_dir / 'logs'

    @property
    def progress_file(self) -> Path:
        return self.data_dir / 'progress.json'

    @property
    def failed_file(self) -> Path:
        return self.data_dir / 'failed.json'


def load_settings(base_dir: Path | None = None) -> Settings:
    resolved_base_dir = (base_dir or Path(__file__).resolve().parents[1]).resolve()
    load_dotenv(resolved_base_dir / '.env', override=False)

    return Settings(
        base_dir=resolved_base_dir,
        douban_api_base=os.getenv('DOUBAN_API_BASE', 'https://api.douban.com/v2/book'),
        request_interval_min=int(os.getenv('REQUEST_INTERVAL_MIN', '2')),
        request_interval_max=int(os.getenv('REQUEST_INTERVAL_MAX', '5')),
        max_retries=int(os.getenv('MAX_RETRIES', '3')),
        batch_size=int(os.getenv('BATCH_SIZE', '1000')),
        total_books=int(os.getenv('TOTAL_BOOKS', '100000')),
        excel_batch_size=int(os.getenv('EXCEL_BATCH_SIZE', '5000')),
        backend_api_base=os.getenv('BACKEND_API_BASE', 'http://localhost:8080/api'),
        admin_username=os.getenv('ADMIN_USERNAME', 'admin001'),
        admin_password=os.getenv('ADMIN_PASSWORD', 'Admin123456!'),
        max_concurrent_imports=int(os.getenv('MAX_CONCURRENT_IMPORTS', '3')),
        default_location=os.getenv('DEFAULT_LOCATION', '待分配'),
        default_status=os.getenv('DEFAULT_STATUS', 'available'),
        default_language=os.getenv('DEFAULT_LANGUAGE', 'zh'),
        default_availability=os.getenv('DEFAULT_AVAILABILITY', 'available'),
        default_circulation_policy=os.getenv('DEFAULT_CIRCULATION_POLICY', 'AUTO'),
        default_total_copies=int(os.getenv('DEFAULT_TOTAL_COPIES', '3')),
    )
