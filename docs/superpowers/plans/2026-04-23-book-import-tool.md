# 图书批量导入工具实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现一个自动化工具，从豆瓣读书 API 抓取 100,000 本真实图书数据并批量导入到系统中

**Architecture:** Python 爬虫脚本从豆瓣 API 抓取数据 → 数据处理模块转换为 Excel 格式 → 批量导入模块调用后端 API 导入数据库。支持断点续传、错误重试、进度监控。

**Tech Stack:** Python 3.8+, requests, aiohttp, openpyxl, pandas, tqdm, fake-useragent

---

## 文件结构

```
book-import-tool/
├── config.py              # 配置文件
├── douban_crawler.py      # 豆瓣爬虫脚本
├── data_processor.py      # 数据处理和转换
├── batch_importer.py      # 批量导入脚本
├── main.py                # 主入口脚本
├── requirements.txt       # Python 依赖
├── data/                  # 数据目录（运行时创建）
│   ├── raw/              # 原始 JSON 数据
│   ├── processed/        # 处理后的 Excel 文件
│   ├── failed.json       # 失败记录
│   └── progress.json     # 进度记录
├── logs/                  # 日志文件（运行时创建）
│   ├── crawler.log
│   ├── processor.log
│   └── importer.log
└── README.md             # 使用说明
```

---

## Task 1: 项目初始化和依赖配置

**Files:**
- Create: `book-import-tool/requirements.txt`
- Create: `book-import-tool/config.py`
- Create: `book-import-tool/.gitignore`

- [ ] **Step 1: 创建项目目录**

```bash
mkdir -p book-import-tool
cd book-import-tool
```

- [ ] **Step 2: 创建 requirements.txt**

```txt
requests==2.31.0
aiohttp==3.9.1
fake-useragent==1.4.0
openpyxl==3.1.2
pandas==2.1.4
tqdm==4.66.1
python-dotenv==1.0.0
```

- [ ] **Step 3: 创建 config.py**

```python
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
```

- [ ] **Step 4: 创建 .gitignore**

```gitignore
# Python
__pycache__/
*.py[cod]
*$py.class
*.so
.Python
env/
venv/
ENV/

# Data and logs
data/
logs/
*.log

# IDE
.vscode/
.idea/
*.swp
*.swo

# OS
.DS_Store
Thumbs.db
```

- [ ] **Step 5: 安装依赖**

```bash
pip install -r requirements.txt
```

Expected: All packages installed successfully

- [ ] **Step 6: 提交初始化代码**

```bash
git add requirements.txt config.py .gitignore
git commit -m "feat: 初始化图书导入工具项目"
```

---

## Task 2: 实现豆瓣爬虫模块

**Files:**
- Create: `book-import-tool/douban_crawler.py`

- [ ] **Step 1: 创建爬虫基础结构**

```python
"""豆瓣图书爬虫模块"""
import json
import time
import random
import logging
import os
from typing import Dict, List, Optional
import requests
from fake_useragent import UserAgent
from tqdm import tqdm
import config

# 配置日志
os.makedirs(config.LOGS_DIR, exist_ok=True)
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler(f'{config.LOGS_DIR}/crawler.log'),
        logging.StreamHandler()
    ]
)
logger = logging.getLogger(__name__)


class DoubanCrawler:
    """豆瓣图书爬虫"""
    
    def __init__(self):
        self.ua = UserAgent()
        self.session = requests.Session()
        self.progress_file = config.PROGRESS_FILE
        self.failed_file = config.FAILED_FILE
        
    def _get_headers(self) -> Dict[str, str]:
        """生成请求头"""
        return {
            'User-Agent': self.ua.random,
            'Accept': 'application/json',
            'Referer': 'https://book.douban.com/'
        }
    
    def _random_sleep(self):
        """随机延迟"""
        time.sleep(random.uniform(
            config.REQUEST_INTERVAL_MIN,
            config.REQUEST_INTERVAL_MAX
        ))

    def _fetch_book(self, book_id: str) -> Optional[Dict]:
        """获取单本图书信息"""
        url = f"{config.DOUBAN_API_BASE}/{book_id}"
        
        for attempt in range(config.MAX_RETRIES):
            try:
                response = self.session.get(
                    url,
                    headers=self._get_headers(),
                    timeout=10
                )
                
                if response.status_code == 200:
                    return response.json()
                elif response.status_code == 429:
                    logger.warning(f"Rate limited, waiting 60s...")
                    time.sleep(60)
                else:
                    logger.error(f"Failed to fetch {book_id}: {response.status_code}")
                    return None
                    
            except Exception as e:
                logger.error(f"Error fetching {book_id}: {e}")
                if attempt < config.MAX_RETRIES - 1:
                    time.sleep(2 ** attempt)
                    
        return None
    
    def search_books(self, query: str, start: int = 0, count: int = 20) -> List[Dict]:
        """搜索图书"""
        url = f"{config.DOUBAN_API_BASE}/search"
        params = {'q': query, 'start': start, 'count': count}
        
        try:
            response = self.session.get(
                url,
                params=params,
                headers=self._get_headers(),
                timeout=10
            )
            
            if response.status_code == 200:
                data = response.json()
                return data.get('books', [])
            else:
                logger.error(f"Search failed: {response.status_code}")
                return []
                
        except Exception as e:
            logger.error(f"Search error: {e}")
            return []


if __name__ == "__main__":
    crawler = DoubanCrawler()
    logger.info("Douban crawler initialized")
```

- [ ] **Step 2: 测试爬虫基础功能**

```bash
cd book-import-tool
python -c "from douban_crawler import DoubanCrawler; c = DoubanCrawler(); print('Crawler initialized successfully')"
```

Expected: "Crawler initialized successfully"

- [ ] **Step 3: 添加进度管理和批量抓取功能**

在 `douban_crawler.py` 中的 `DoubanCrawler` 类添加以下方法：

```python
    def _load_progress(self) -> Dict:
        """加载进度"""
        if os.path.exists(self.progress_file):
            with open(self.progress_file, 'r', encoding='utf-8') as f:
                return json.load(f)
        return {'crawled_count': 0, 'book_ids': []}
    
    def _save_progress(self, progress: Dict):
        """保存进度"""
        os.makedirs(os.path.dirname(self.progress_file), exist_ok=True)
        with open(self.progress_file, 'w', encoding='utf-8') as f:
            json.dump(progress, f, ensure_ascii=False, indent=2)
    
    def _save_batch(self, books: List[Dict], batch_num: int):
        """保存批次数据"""
        os.makedirs(config.RAW_DATA_DIR, exist_ok=True)
        filename = f"{config.RAW_DATA_DIR}/batch_{batch_num:03d}.json"
        with open(filename, 'w', encoding='utf-8') as f:
            json.dump(books, f, ensure_ascii=False, indent=2)
        logger.info(f"Saved batch {batch_num} to {filename}")
    
    def crawl_books(self, total_books: int = config.TOTAL_BOOKS):
        """批量抓取图书"""
        progress = self._load_progress()
        crawled_count = progress['crawled_count']
        
        logger.info(f"Starting crawl from {crawled_count}/{total_books}")
        
        # 搜索关键词列表（用于获取不同类型的图书）
        keywords = ['小说', '文学', '历史', '哲学', '科学', '艺术', '经济', '管理', 
                   '心理', '社会', '计算机', '编程', '数学', '物理', '化学', '生物']
        
        batch_num = crawled_count // config.BATCH_SIZE + 1
        current_batch = []
        
        with tqdm(total=total_books, initial=crawled_count, desc="Crawling") as pbar:
            for keyword in keywords:
                if crawled_count >= total_books:
                    break
                    
                start = 0
                while crawled_count < total_books:
                    books = self.search_books(keyword, start, 20)
                    if not books:
                        break
                    
                    for book in books:
                        if crawled_count >= total_books:
                            break
                        
                        book_id = book.get('id')
                        if book_id and book_id not in progress['book_ids']:
                            book_detail = self._fetch_book(book_id)
                            if book_detail:
                                current_batch.append(book_detail)
                                progress['book_ids'].append(book_id)
                                crawled_count += 1
                                pbar.update(1)
                                
                                # 保存批次
                                if len(current_batch) >= config.BATCH_SIZE:
                                    self._save_batch(current_batch, batch_num)
                                    progress['crawled_count'] = crawled_count
                                    self._save_progress(progress)
                                    current_batch = []
                                    batch_num += 1
                            
                            self._random_sleep()
                    
                    start += 20
                    self._random_sleep()
        
        # 保存剩余数据
        if current_batch:
            self._save_batch(current_batch, batch_num)
            progress['crawled_count'] = crawled_count
            self._save_progress(progress)
        
        logger.info(f"Crawling completed: {crawled_count} books")
```

- [ ] **Step 4: 测试批量抓取功能（小规模测试）**

```bash
cd book-import-tool
python -c "from douban_crawler import DoubanCrawler; c = DoubanCrawler(); c.crawl_books(10)"
```

Expected: 成功抓取 10 本图书，生成 data/raw/batch_001.json 和 data/progress.json

- [ ] **Step 5: 验证数据文件**

```bash
cat data/progress.json
ls -lh data/raw/
```

Expected: progress.json 显示 crawled_count: 10，raw 目录包含 batch_001.json

- [ ] **Step 6: 提交爬虫代码**

```bash
git add douban_crawler.py
git commit -m "feat: 实现豆瓣图书爬虫模块"
```

---

## Task 3: 实现数据处理模块

**Files:**
- Create: `book-import-tool/data_processor.py`

- [ ] **Step 1: 创建数据处理基础结构**

```python
"""数据处理和转换模块"""
import json
import os
import re
import logging
from typing import Dict, List, Optional
import pandas as pd
from openpyxl import Workbook
from openpyxl.styles import Font, Alignment
import config

# 配置日志
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler(f'{config.LOGS_DIR}/processor.log'),
        logging.StreamHandler()
    ]
)
logger = logging.getLogger(__name__)


class DataProcessor:
    """数据处理器"""
    
    def __init__(self):
        self.processed_count = 0
        self.failed_books = []
    
    def _clean_text(self, text: str) -> str:
        """清理文本"""
        if not text:
            return ""
        # 移除 HTML 标签
        text = re.sub(r'<[^>]+>', '', text)
        # 移除多余空白
        text = re.sub(r'\s+', ' ', text)
        return text.strip()

    def _validate_isbn(self, isbn: str) -> bool:
        """验证 ISBN 格式"""
        if not isbn:
            return False
        # 移除连字符和空格
        isbn = isbn.replace('-', '').replace(' ', '')
        # ISBN-10 或 ISBN-13
        return len(isbn) in [10, 13] and isbn.isdigit()
    
    def _extract_year(self, pubdate: str) -> str:
        """提取出版年份"""
        if not pubdate:
            return ""
        # 提取年份（4位数字）
        match = re.search(r'(\d{4})', pubdate)
        return match.group(1) if match else ""
    
    def _convert_book(self, book: Dict) -> Optional[Dict]:
        """转换单本图书数据"""
        try:
            # 必填字段验证
            title = book.get('title', '').strip()
            author = book.get('author', [])
            isbn = book.get('isbn13', '') or book.get('isbn10', '')
            
            if not title or not author or not isbn:
                logger.warning(f"Missing required fields: {book.get('id')}")
                return None
            
            if not self._validate_isbn(isbn):
                logger.warning(f"Invalid ISBN: {isbn}")
                return None
            
            # 转换作者（数组转字符串）
            author_str = ', '.join(author) if isinstance(author, list) else str(author)
            
            # 提取分类（取第一个标签）
            tags = book.get('tags', [])
            category = tags[0].get('name', '') if tags else ''
            
            # 获取封面图片
            images = book.get('images', {})
            cover_url = images.get('large', '') or images.get('medium', '') or images.get('small', '')
            
            return {
                'title': self._clean_text(title),
                'author': self._clean_text(author_str),
                'isbn': isbn.replace('-', '').replace(' ', ''),
                'location': config.DEFAULT_LOCATION,
                'coverUrl': cover_url,
                'status': config.DEFAULT_STATUS,
                'year': self._extract_year(book.get('pubdate', '')),
                'description': self._clean_text(book.get('summary', ''))[:500],  # 限制长度
                'languageCode': config.DEFAULT_LANGUAGE,
                'availability': config.DEFAULT_AVAILABILITY,
                'category': self._clean_text(category),
                'circulationPolicy': config.DEFAULT_CIRCULATION_POLICY,
                'totalCopies': config.DEFAULT_TOTAL_COPIES
            }
            
        except Exception as e:
            logger.error(f"Error converting book: {e}")
            return None

    def process_batch(self, batch_file: str) -> List[Dict]:
        """处理单个批次文件"""
        logger.info(f"Processing {batch_file}")
        
        with open(batch_file, 'r', encoding='utf-8') as f:
            books = json.load(f)
        
        processed_books = []
        for book in books:
            converted = self._convert_book(book)
            if converted:
                processed_books.append(converted)
            else:
                self.failed_books.append({
                    'id': book.get('id'),
                    'title': book.get('title'),
                    'reason': 'Conversion failed'
                })
        
        return processed_books
    
    def create_excel(self, books: List[Dict], output_file: str):
        """创建 Excel 文件"""
        df = pd.DataFrame(books)
        
        # 确保列顺序
        columns = ['title', 'author', 'isbn', 'location', 'coverUrl', 'status', 
                  'year', 'description', 'languageCode', 'availability', 
                  'category', 'circulationPolicy', 'totalCopies']
        df = df[columns]
        
        # 保存为 Excel
        os.makedirs(os.path.dirname(output_file), exist_ok=True)
        df.to_excel(output_file, index=False, engine='openpyxl')
        logger.info(f"Created Excel file: {output_file}")
    
    def process_all_batches(self):
        """处理所有批次文件"""
        batch_files = sorted([
            os.path.join(config.RAW_DATA_DIR, f)
            for f in os.listdir(config.RAW_DATA_DIR)
            if f.startswith('batch_') and f.endswith('.json')
        ])
        
        all_books = []
        excel_num = 1
        
        for batch_file in batch_files:
            processed = self.process_batch(batch_file)
            all_books.extend(processed)
            
            # 每 EXCEL_BATCH_SIZE 本生成一个 Excel
            while len(all_books) >= config.EXCEL_BATCH_SIZE:
                excel_books = all_books[:config.EXCEL_BATCH_SIZE]
                output_file = f"{config.PROCESSED_DATA_DIR}/books_{excel_num:03d}.xlsx"
                self.create_excel(excel_books, output_file)
                all_books = all_books[config.EXCEL_BATCH_SIZE:]
                excel_num += 1
        
        # 处理剩余图书
        if all_books:
            output_file = f"{config.PROCESSED_DATA_DIR}/books_{excel_num:03d}.xlsx"
            self.create_excel(all_books, output_file)
        
        # 保存失败记录
        if self.failed_books:
            with open(config.FAILED_FILE, 'w', encoding='utf-8') as f:
                json.dump(self.failed_books, f, ensure_ascii=False, indent=2)
        
        logger.info(f"Processing completed. Failed: {len(self.failed_books)}")


if __name__ == "__main__":
    processor = DataProcessor()
    processor.process_all_batches()
```

- [ ] **Step 2: 测试数据处理功能**

```bash
cd book-import-tool
python data_processor.py
```

Expected: 成功处理 data/raw/ 中的所有批次文件，生成 Excel 文件到 data/processed/

- [ ] **Step 3: 验证生成的 Excel 文件**

```bash
ls -lh data/processed/
python -c "import pandas as pd; df = pd.read_excel('data/processed/books_001.xlsx'); print(f'Rows: {len(df)}, Columns: {len(df.columns)}'); print(df.head())"
```

Expected: 显示 Excel 文件包含正确的行数和列数，前几行数据格式正确

- [ ] **Step 4: 提交数据处理代码**

```bash
git add data_processor.py
git commit -m "feat: 实现数据处理和转换模块"
```

---

## Task 4: 实现批量导入模块

**Files:**
- Create: `book-import-tool/batch_importer.py`

- [ ] **Step 1: 创建导入模块基础结构**

```python
"""批量导入模块"""
import os
import time
import logging
from typing import Dict, List, Optional
import requests
from concurrent.futures import ThreadPoolExecutor, as_completed
from tqdm import tqdm
import config

# 配置日志
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler(f'{config.LOGS_DIR}/importer.log'),
        logging.StreamHandler()
    ]
)
logger = logging.getLogger(__name__)


class BatchImporter:
    """批量导入器"""
    
    def __init__(self):
        self.token = None
        self.session = requests.Session()
    
    def _login(self) -> bool:
        """登录获取 JWT token"""
        url = f"{config.BACKEND_API_BASE}/auth/login"
        payload = {
            'username': config.ADMIN_USERNAME,
            'password': config.ADMIN_PASSWORD
        }
        
        try:
            response = self.session.post(url, json=payload, timeout=10)
            if response.status_code == 200:
                data = response.json()
                self.token = data.get('data', {}).get('token')
                if self.token:
                    logger.info("Login successful")
                    return True
            
            logger.error(f"Login failed: {response.status_code}")
            return False
            
        except Exception as e:
            logger.error(f"Login error: {e}")
            return False

    def _import_file(self, excel_file: str) -> Dict:
        """导入单个 Excel 文件"""
        url = f"{config.BACKEND_API_BASE}/books/import"
        headers = {'Authorization': f'Bearer {self.token}'}
        
        try:
            with open(excel_file, 'rb') as f:
                files = {'file': (os.path.basename(excel_file), f, 
                        'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet')}
                response = self.session.post(
                    url,
                    headers=headers,
                    files=files,
                    timeout=300  # 5分钟超时
                )
            
            if response.status_code == 200:
                data = response.json()
                result = data.get('data', {})
                logger.info(f"Imported {excel_file}: {result.get('successCount')} success, "
                          f"{result.get('failureCount')} failed")
                return result
            else:
                logger.error(f"Import failed {excel_file}: {response.status_code}")
                return {'successCount': 0, 'failureCount': 0, 'error': response.text}
                
        except Exception as e:
            logger.error(f"Import error {excel_file}: {e}")
            return {'successCount': 0, 'failureCount': 0, 'error': str(e)}
    
    def import_all_files(self):
        """导入所有 Excel 文件"""
        if not self._login():
            logger.error("Cannot proceed without authentication")
            return
        
        excel_files = sorted([
            os.path.join(config.PROCESSED_DATA_DIR, f)
            for f in os.listdir(config.PROCESSED_DATA_DIR)
            if f.startswith('books_') and f.endswith('.xlsx')
        ])
        
        if not excel_files:
            logger.warning("No Excel files found to import")
            return
        
        total_success = 0
        total_failed = 0
        
        logger.info(f"Starting import of {len(excel_files)} files")
        
        with ThreadPoolExecutor(max_workers=config.MAX_CONCURRENT_IMPORTS) as executor:
            futures = {executor.submit(self._import_file, f): f for f in excel_files}
            
            with tqdm(total=len(excel_files), desc="Importing") as pbar:
                for future in as_completed(futures):
                    result = future.result()
                    total_success += result.get('successCount', 0)
                    total_failed += result.get('failureCount', 0)
                    pbar.update(1)
        
        logger.info(f"Import completed: {total_success} success, {total_failed} failed")
        print(f"\n导入完成:")
        print(f"  成功: {total_success} 本")
        print(f"  失败: {total_failed} 本")


if __name__ == "__main__":
    importer = BatchImporter()
    importer.import_all_files()
```

- [ ] **Step 2: 测试登录功能**

```bash
cd book-import-tool
python -c "from batch_importer import BatchImporter; i = BatchImporter(); success = i._login(); print(f'Login: {success}')"
```

Expected: "Login: True"

- [ ] **Step 3: 测试导入功能（确保后端服务运行）**

```bash
# 先确保后端服务运行
cd ../backend
mvn spring-boot:run &

# 等待后端启动后测试导入
cd ../book-import-tool
python batch_importer.py
```

Expected: 成功导入所有 Excel 文件，显示成功和失败数量

- [ ] **Step 4: 提交导入模块代码**

```bash
git add batch_importer.py
git commit -m "feat: 实现批量导入模块"
```

---

## Task 5: 创建主入口脚本

**Files:**
- Create: `book-import-tool/main.py`

- [ ] **Step 1: 创建主入口脚本**

```python
"""主入口脚本"""
import sys
import logging
from douban_crawler import DoubanCrawler
from data_processor import DataProcessor
from batch_importer import BatchImporter
import config

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)


def main():
    """主函数"""
    print("=" * 60)
    print("图书批量导入工具")
    print("=" * 60)
    print()
    
    # 步骤 1: 抓取数据
    print("步骤 1/3: 从豆瓣抓取图书数据")
    print(f"目标数量: {config.TOTAL_BOOKS} 本")
    print("-" * 60)
    
    try:
        crawler = DoubanCrawler()
        crawler.crawl_books(config.TOTAL_BOOKS)
        print("✓ 数据抓取完成")
    except KeyboardInterrupt:
        print("\n用户中断，进度已保存，可以稍后继续")
        sys.exit(0)
    except Exception as e:
        logger.error(f"抓取失败: {e}")
        sys.exit(1)
    
    print()
    
    # 步骤 2: 处理数据
    print("步骤 2/3: 处理和转换数据")
    print("-" * 60)
    
    try:
        processor = DataProcessor()
        processor.process_all_batches()
        print("✓ 数据处理完成")
    except Exception as e:
        logger.error(f"处理失败: {e}")
        sys.exit(1)
    
    print()
    
    # 步骤 3: 导入数据
    print("步骤 3/3: 批量导入到系统")
    print("-" * 60)
    
    try:
        importer = BatchImporter()
        importer.import_all_files()
        print("✓ 数据导入完成")
    except Exception as e:
        logger.error(f"导入失败: {e}")
        sys.exit(1)
    
    print()
    print("=" * 60)
    print("全部完成！")
    print("=" * 60)


if __name__ == "__main__":
    main()
```

- [ ] **Step 2: 测试主脚本**

```bash
cd book-import-tool
python main.py
```

Expected: 依次执行抓取、处理、导入三个步骤，显示进度和结果

- [ ] **Step 3: 提交主脚本**

```bash
git add main.py
git commit -m "feat: 添加主入口脚本"
```

---

## Task 6: 创建 README 文档

**Files:**
- Create: `book-import-tool/README.md`

- [ ] **Step 1: 创建 README 文档**

```markdown
# 图书批量导入工具

从豆瓣读书 API 抓取图书数据并批量导入到图书馆系统。

## 功能特性

- 从豆瓣读书 API 抓取真实图书数据
- 支持断点续传，中断后可继续
- 自动数据清洗和格式转换
- 批量导入到后端系统
- 完整的日志记录和错误处理

## 环境要求

- Python 3.8+
- 后端服务运行在 http://localhost:8080

## 安装

```bash
pip install -r requirements.txt
```

## 使用方法

### 一键执行（推荐）

```bash
python main.py
```

### 分步执行

```bash
# 步骤 1: 抓取数据
python douban_crawler.py

# 步骤 2: 处理数据
python data_processor.py

# 步骤 3: 导入数据
python batch_importer.py
```

## 配置

编辑 `config.py` 修改配置：

- `TOTAL_BOOKS`: 目标抓取数量（默认 100,000）
- `REQUEST_INTERVAL_MIN/MAX`: 请求间隔（秒）
- `BACKEND_API_BASE`: 后端 API 地址
- `ADMIN_USERNAME/PASSWORD`: 管理员账号

## 目录结构

```
book-import-tool/
├── data/
│   ├── raw/              # 原始 JSON 数据
│   ├── processed/        # 处理后的 Excel 文件
│   ├── progress.json     # 进度记录
│   └── failed.json       # 失败记录
└── logs/                 # 日志文件
```

## 断点续传

如果中途中断，再次运行会自动从上次的进度继续。

## 注意事项

1. 豆瓣 API 有限流，建议设置合理的请求间隔
2. 确保后端服务已启动
3. 首次运行需要管理员账号权限
4. 大量数据导入可能需要较长时间

## 故障排除

### 登录失败

检查 `config.py` 中的管理员账号和密码是否正确。

### 导入失败

确保后端服务正常运行：

```bash
cd backend
mvn spring-boot:run
```

### 豆瓣 API 限流

增加 `config.py` 中的 `REQUEST_INTERVAL_MIN` 和 `REQUEST_INTERVAL_MAX`。

## 许可证

MIT
```

- [ ] **Step 2: 提交 README**

```bash
git add README.md
git commit -m "docs: 添加 README 文档"
```

---

## Task 7: 最终测试和验证

**Files:**
- None (testing only)

- [ ] **Step 1: 清理测试数据**

```bash
cd book-import-tool
rm -rf data/ logs/
```

- [ ] **Step 2: 完整流程测试（小规模）**

修改 `config.py` 临时设置小目标：

```python
TOTAL_BOOKS = 50  # 临时改为 50 本用于测试
```

运行完整流程：

```bash
python main.py
```

Expected: 成功完成抓取、处理、导入全流程

- [ ] **Step 3: 验证数据库**

```bash
cd ../backend
# 连接数据库查询
mysql -u root -p library_db -e "SELECT COUNT(*) FROM books;"
```

Expected: 显示约 50 条记录

- [ ] **Step 4: 验证前端显示**

打开浏览器访问 http://localhost:5173，查看馆藏页面。

Expected: 显示导入的图书数据

- [ ] **Step 5: 恢复配置**

将 `config.py` 中的 `TOTAL_BOOKS` 改回 100000

```python
TOTAL_BOOKS = 100000
```

- [ ] **Step 6: 最终提交**

```bash
git add config.py
git commit -m "test: 完成完整流程测试"
git push origin master
```

---

## 自我审查清单

### 规格覆盖检查

- [x] 数据抓取：从豆瓣 API 抓取图书数据
- [x] 反爬虫策略：请求间隔、User-Agent 轮换、错误重试
- [x] 数据处理：字段映射、数据清洗、格式转换
- [x] 数据验证：必填字段、ISBN 格式、去重
- [x] Excel 生成：分批生成 Excel 文件
- [x] 批量导入：调用后端 API 导入数据
- [x] 错误处理：网络错误、API 限流、数据格式错误、导入失败
- [x] 进度管理：断点续传、进度持久化
- [x] 日志记录：抓取日志、处理日志、导入日志
- [x] 并发控制：限制并发导入数

### 占位符检查

- [x] 无 TBD、TODO 或未完成部分
- [x] 所有代码步骤包含完整实现
- [x] 所有测试步骤包含具体命令和预期输出

### 类型一致性检查

- [x] 文件路径在所有任务中一致
- [x] 函数名和方法名在所有任务中一致
- [x] 配置项名称在所有任务中一致
- [x] 数据结构在所有任务中一致

---

## 执行选项

计划已完成并保存到 `docs/superpowers/plans/2026-04-23-book-import-tool.md`。

**两种执行方式：**

**1. Subagent-Driven（推荐）** - 我为每个任务派发一个新的子代理，任务间进行审查，快速迭代

**2. Inline Execution** - 在当前会话中使用 executing-plans 执行任务，批量执行并设置检查点

你选择哪种方式？

