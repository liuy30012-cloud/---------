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

# 确保日志目录存在
os.makedirs(config.LOGS_DIR, exist_ok=True)

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
                'description': self._clean_text(book.get('summary', ''))[:500],
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
