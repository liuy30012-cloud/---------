"""配置文件"""

# 豆瓣 API 配置
DOUBAN_API_BASE = "https://api.douban.com/v2/book"
REQUEST_INTERVAL_MIN = 2  # 最小请求间隔（秒）
REQUEST_INTERVAL_MAX = 5  # 最大请求间隔（秒）
MAX_RETRIES = 3           # 最大重试次数

# 数据抓取配置
BATCH_SIZE = 1000         # 每批抓取数量
TOTAL_BOOKS = 100000      # 总目标数量

# 数据处理配置
EXCEL_BATCH_SIZE = 5000   # 每个 Excel 文件包含的图书数量

# 后端 API 配置
BACKEND_API_BASE = "http://localhost:8080/api"
ADMIN_USERNAME = "admin"
ADMIN_PASSWORD = "admin123"

# 导入配置
MAX_CONCURRENT_IMPORTS = 3  # 最大并发导入数

# 默认值配置
DEFAULT_LOCATION = "待分配"
DEFAULT_STATUS = "available"
DEFAULT_LANGUAGE = "zh"
DEFAULT_AVAILABILITY = "available"
DEFAULT_CIRCULATION_POLICY = "AUTO"
DEFAULT_TOTAL_COPIES = 3

# 目录配置
DATA_DIR = "data"
RAW_DATA_DIR = "data/raw"
PROCESSED_DATA_DIR = "data/processed"
LOGS_DIR = "logs"
PROGRESS_FILE = "data/progress.json"
FAILED_FILE = "data/failed.json"
