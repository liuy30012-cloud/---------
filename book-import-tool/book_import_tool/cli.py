from __future__ import annotations

from .config import settings


def run_pipeline(crawler_cls, processor_cls, importer_cls) -> None:
    crawler = crawler_cls()
    crawler.crawl_books(settings.total_books)

    processor = processor_cls()
    processor.process_all_batches()

    importer = importer_cls()
    importer.import_all_files()


def main() -> None:
    from douban_crawler import DoubanCrawler
    from data_processor import DataProcessor
    from batch_importer import BatchImporter

    run_pipeline(DoubanCrawler, DataProcessor, BatchImporter)
