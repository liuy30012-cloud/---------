"""批量导入模块"""

import requests
import logging
import os
from pathlib import Path
from typing import Optional, Dict
from concurrent.futures import ThreadPoolExecutor, as_completed
from tqdm import tqdm
import config

# 配置日志
os.makedirs(config.LOGS_DIR, exist_ok=True)
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler(f'{config.LOGS_DIR}/importer.log', encoding='utf-8'),
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
        """登录后端 API，获取 JWT token"""
        try:
            url = f"{config.BACKEND_API_BASE}/auth/login"
            payload = {
                "username": config.ADMIN_USERNAME,
                "password": config.ADMIN_PASSWORD
            }

            logger.info(f"正在登录: {url}")
            response = self.session.post(url, json=payload, timeout=10)
            response.raise_for_status()

            data = response.json()
            if data.get('code') == 200 and data.get('data', {}).get('token'):
                self.token = data['data']['token']
                logger.info("登录成功")
                return True
            else:
                logger.error(f"登录失败: {data}")
                return False

        except Exception as e:
            logger.error(f"登录异常: {e}")
            return False

    def _import_file(self, excel_file: Path) -> Dict:
        """导入单个 Excel 文件"""
        try:
            if not self.token:
                return {
                    'file': excel_file.name,
                    'success': False,
                    'error': 'No token available'
                }

            url = f"{config.BACKEND_API_BASE}/books/import"
            headers = {
                'Authorization': f'Bearer {self.token}'
            }

            with open(excel_file, 'rb') as f:
                files = {'file': (excel_file.name, f, 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet')}
                response = self.session.post(url, headers=headers, files=files, timeout=300)
                response.raise_for_status()

                data = response.json()
                if data.get('code') == 200:
                    result_data = data.get('data', {})
                    success_count = result_data.get('successCount', 0)
                    failure_count = result_data.get('failureCount', 0)

                    logger.info(f"文件 {excel_file.name} 导入完成: 成功 {success_count}, 失败 {failure_count}")
                    return {
                        'file': excel_file.name,
                        'success': True,
                        'successCount': success_count,
                        'failureCount': failure_count
                    }
                else:
                    logger.error(f"文件 {excel_file.name} 导入失败: {data}")
                    return {
                        'file': excel_file.name,
                        'success': False,
                        'error': data.get('message', 'Unknown error')
                    }

        except Exception as e:
            logger.error(f"文件 {excel_file.name} 导入异常: {e}")
            return {
                'file': excel_file.name,
                'success': False,
                'error': str(e)
            }

    def import_all_files(self):
        """导入所有 Excel 文件"""
        # 登录
        if not self._login():
            logger.error("登录失败，无法继续导入")
            return

        # 获取所有 Excel 文件
        processed_dir = Path(config.PROCESSED_DATA_DIR)
        if not processed_dir.exists():
            logger.error(f"目录不存在: {processed_dir}")
            return

        excel_files = list(processed_dir.glob("*.xlsx"))
        if not excel_files:
            logger.warning(f"未找到 Excel 文件: {processed_dir}")
            return

        logger.info(f"找到 {len(excel_files)} 个 Excel 文件")

        # 并发导入
        total_success = 0
        total_failure = 0
        failed_files = []

        with ThreadPoolExecutor(max_workers=config.MAX_CONCURRENT_IMPORTS) as executor:
            futures = {executor.submit(self._import_file, f): f for f in excel_files}

            with tqdm(total=len(excel_files), desc="导入进度") as pbar:
                for future in as_completed(futures):
                    result = future.result()
                    pbar.update(1)

                    if result['success']:
                        total_success += result.get('successCount', 0)
                        total_failure += result.get('failureCount', 0)
                    else:
                        failed_files.append(result)

        # 输出统计
        logger.info("=" * 50)
        logger.info(f"导入完成!")
        logger.info(f"总成功: {total_success}")
        logger.info(f"总失败: {total_failure}")
        if failed_files:
            logger.warning(f"失败文件数: {len(failed_files)}")
            for failed in failed_files:
                logger.warning(f"  - {failed['file']}: {failed.get('error', 'Unknown')}")
        logger.info("=" * 50)


if __name__ == "__main__":
    importer = BatchImporter()
    importer.import_all_files()
