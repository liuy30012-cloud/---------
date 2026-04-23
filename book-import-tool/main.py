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
