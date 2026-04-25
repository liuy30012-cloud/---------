"""Backwards-compatible settings exports for legacy scripts."""

from book_import_tool.config import settings

DOUBAN_API_BASE = settings.douban_api_base
REQUEST_INTERVAL_MIN = settings.request_interval_min
REQUEST_INTERVAL_MAX = settings.request_interval_max
MAX_RETRIES = settings.max_retries
BATCH_SIZE = settings.batch_size
TOTAL_BOOKS = settings.total_books
EXCEL_BATCH_SIZE = settings.excel_batch_size
BACKEND_API_BASE = settings.backend_api_base
ADMIN_USERNAME = settings.admin_username
ADMIN_PASSWORD = settings.admin_password
MAX_CONCURRENT_IMPORTS = settings.max_concurrent_imports
DEFAULT_LOCATION = settings.default_location
DEFAULT_STATUS = settings.default_status
DEFAULT_LANGUAGE = settings.default_language
DEFAULT_AVAILABILITY = settings.default_availability
DEFAULT_CIRCULATION_POLICY = settings.default_circulation_policy
DEFAULT_TOTAL_COPIES = settings.default_total_copies
BASE_DIR = str(settings.base_dir)
DATA_DIR = str(settings.data_dir)
RAW_DATA_DIR = str(settings.raw_data_dir)
PROCESSED_DATA_DIR = str(settings.processed_data_dir)
LOGS_DIR = str(settings.logs_dir)
PROGRESS_FILE = str(settings.progress_file)
FAILED_FILE = str(settings.failed_file)
