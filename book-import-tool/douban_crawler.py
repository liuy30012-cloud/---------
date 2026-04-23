"""豆瓣图书数据爬取模块"""

import requests
import time
import random
import json
import logging
import os
from pathlib import Path
from typing import Optional, Dict, List
from tqdm import tqdm
from fake_useragent import UserAgent
import config

# 配置日志
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler(f'{config.LOGS_DIR}/crawler.log', encoding='utf-8'),
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
        """随机延迟，避免请求过快"""
        time.sleep(random.uniform(config.REQUEST_INTERVAL_MIN, config.REQUEST_INTERVAL_MAX))

    def _fetch_book(self, book_id: str) -> Optional[Dict]:
        """获取单本图书详情

        Args:
            book_id: 豆瓣图书 ID

        Returns:
            图书详情 JSON 数据，失败返回 None
        """
        url = f"{config.DOUBAN_API_BASE}/{book_id}"

        for attempt in range(config.MAX_RETRIES):
            try:
                self._random_sleep()
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
                elif response.status_code == 404:
                    logger.warning(f"图书 {book_id} 不存在")
                    return None
                else:
                    logger.warning(f"获取图书 {book_id} 失败，状态码: {response.status_code}")

            except requests.RequestException as e:
                logger.error(f"请求图书 {book_id} 出错 (尝试 {attempt + 1}/{config.MAX_RETRIES}): {e}")

            if attempt < config.MAX_RETRIES - 1:
                time.sleep(2 ** attempt)  # 指数退避

        return None

    def search_books(self, query: str, start: int = 0, count: int = 20) -> Optional[Dict]:
        """搜索图书

        Args:
            query: 搜索关键词
            start: 起始位置
            count: 返回数量

        Returns:
            搜索结果 JSON 数据，失败返回 None
        """
        url = f"{config.DOUBAN_API_BASE}/search"
        params = {
            'q': query,
            'start': start,
            'count': count
        }

        for attempt in range(config.MAX_RETRIES):
            try:
                self._random_sleep()
                response = self.session.get(url, params=params, timeout=10)

                if response.status_code == 200:
                    return response.json()
                else:
                    logger.warning(f"搜索 '{query}' 失败，状态码: {response.status_code}")

            except requests.RequestException as e:
                logger.error(f"搜索 '{query}' 出错 (尝试 {attempt + 1}/{config.MAX_RETRIES}): {e}")

            if attempt < config.MAX_RETRIES - 1:
                time.sleep(2 ** attempt)

        return None

    def _save_batch(self, books: List[Dict], batch_num: int):
        """保存一批图书数据

        Args:
            books: 图书数据列表
            batch_num: 批次号
        """
        filename = f"{config.RAW_DATA_DIR}/batch_{batch_num:03d}.json"
        with open(filename, 'w', encoding='utf-8') as f:
            json.dump(books, f, ensure_ascii=False, indent=2)
        logger.info(f"保存批次 {batch_num}，共 {len(books)} 本图书到 {filename}")

    def _load_progress(self) -> Dict:
        """加载爬取进度"""
        if os.path.exists(self.progress_file):
            with open(self.progress_file, 'r', encoding='utf-8') as f:
                return json.load(f)
        return {'crawled_count': 0, 'book_ids': []}

    def _save_progress(self, progress: Dict):
        """保存爬取进度"""
        os.makedirs(os.path.dirname(self.progress_file), exist_ok=True)
        with open(self.progress_file, 'w', encoding='utf-8') as f:
            json.dump(progress, f, ensure_ascii=False, indent=2)

    def crawl_books(self, total_books: int = config.TOTAL_BOOKS):
        """爬取图书数据"""
        # 搜索关键词列表
        keywords = [
            '小说', '文学', '历史', '哲学', '科学', '艺术', '经济', '管理',
            '心理', '社会', '计算机', '编程', '数学', '物理', '化学', '生物'
        ]

        # 加载进度
        progress = self._load_progress()
        crawled_count = progress['crawled_count']
        book_ids = set(progress['book_ids'])

        logger.info(f"开始爬取，当前进度: {crawled_count}/{total_books}")

        # 当前批次数据
        current_batch = crawled_count // config.BATCH_SIZE
        books_in_current_batch = []

        try:
            with tqdm(total=total_books, initial=crawled_count, desc="爬取进度") as pbar:
                while crawled_count < total_books:
                    # 遍历关键词
                    for keyword in keywords:
                        if crawled_count >= total_books:
                            break

                        logger.info(f"搜索关键词: {keyword}")

                        # 每个关键词搜索多页
                        for page in range(10):  # 每个关键词最多搜索 10 页
                            if crawled_count >= total_books:
                                break

                            start = page * 20
                            result = self.search_books(keyword, start=start, count=20)

                            if not result or 'books' not in result:
                                break

                            books = result['books']
                            if not books:
                                break

                            # 处理搜索结果
                            for book in books:
                                if crawled_count >= total_books:
                                    break

                                # 去重
                                book_id = book.get('id')
                                if book_id in book_ids:
                                    continue

                                book_ids.add(book_id)
                                books_in_current_batch.append(book)
                                crawled_count += 1
                                pbar.update(1)

                                # 达到批次大小，保存
                                if len(books_in_current_batch) >= config.BATCH_SIZE:
                                    self._save_batch(books_in_current_batch, current_batch)
                                    current_batch += 1
                                    books_in_current_batch = []

                                # 保存进度
                                progress = {
                                    'crawled_count': crawled_count,
                                    'book_ids': list(book_ids)
                                }
                                self._save_progress(progress)

            # 保存最后一批
            if books_in_current_batch:
                self._save_batch(books_in_current_batch, current_batch)

            logger.info(f"爬取完成！共爬取 {crawled_count} 本图书")

        except KeyboardInterrupt:
            logger.info("用户中断爬取")
            # 保存当前进度
            progress = {
                'crawled_count': crawled_count,
                'book_ids': list(book_ids)
            }
            self._save_progress(progress)
            logger.info(f"进度已保存，当前已爬取 {crawled_count} 本图书")

        except Exception as e:
            logger.error(f"爬取过程中出错: {e}", exc_info=True)
            # 保存当前进度
            progress = {
                'crawled_count': crawled_count,
                'book_ids': list(book_ids)
            }
            self._save_progress(progress)
            raise


if __name__ == "__main__":
    crawler = DoubanCrawler()
    logger.info("Douban crawler initialized")
