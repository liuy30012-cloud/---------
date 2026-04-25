"""将 DouBanSpider 数据转换并导入到系统"""
import os
import pandas as pd
import glob
from pathlib import Path
import sys

# 添加当前目录到路径
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import config

def convert_douban_excel_to_system_format(input_file, output_file, isbn_start_offset=0):
    """转换 DouBanSpider Excel 格式到系统格式

    Args:
        input_file: 输入文件路径
        output_file: 输出文件路径
        isbn_start_offset: ISBN起始偏移量，用于避免不同文件间ISBN重复
    """
    print(f"处理文件: {input_file}")

    # 读取原始数据
    df = pd.read_excel(input_file)

    # 原始列名（可能是乱码，但位置是固定的）
    # 列顺序: 序号, 书名, 评分, 评价人数, 作者, 出版信息
    df.columns = ['序号', '书名', '评分', '评价人数', '作者', '出版信息']

    # 创建系统需要的格式
    converted_data = []

    for idx, row in df.iterrows():
        try:
            # 提取书名
            title = str(row['书名']).strip() if pd.notna(row['书名']) else ''

            # 提取作者
            author = str(row['作者']).strip() if pd.notna(row['作者']) else ''
            # 清理作者信息（移除"作者/译者："等前缀）
            if '作者' in author or '译者' in author:
                author = author.split('/')[-1].strip()

            # 提取出版信息
            pub_info = str(row['出版信息']).strip() if pd.notna(row['出版信息']) else ''

            # 从出版信息中提取出版社、出版日期、价格
            publisher = ''
            pub_date = ''
            price = ''

            if pub_info:
                parts = pub_info.split('/')
                if len(parts) >= 1:
                    publisher = parts[0].strip()
                if len(parts) >= 2:
                    pub_date = parts[1].strip()
                if len(parts) >= 3:
                    price = parts[2].strip()

            # 生成 ISBN（使用序号 + 偏移量生成假的 ISBN-13）
            isbn = f"978{(isbn_start_offset + idx):010d}"

            # 提取年份
            year = ''
            if pub_date:
                import re
                match = re.search(r'(\d{4})', pub_date)
                if match:
                    year = match.group(1)

            # 创建系统格式的记录
            record = {
                'title': title,
                'author': author,
                'isbn': isbn,
                'location': config.DEFAULT_LOCATION,
                'coverUrl': '',  # DouBanSpider 数据没有封面
                'status': config.DEFAULT_STATUS,
                'year': year,
                'description': f"评分: {row['评分']}, 评价人数: {row['评价人数']}",
                'languageCode': config.DEFAULT_LANGUAGE,
                'availability': config.DEFAULT_AVAILABILITY,
                'category': os.path.basename(input_file).replace('book_list-', '').replace('.xlsx', ''),
                'circulationPolicy': config.DEFAULT_CIRCULATION_POLICY,
                'totalCopies': config.DEFAULT_TOTAL_COPIES
            }

            converted_data.append(record)

        except Exception as e:
            print(f"  警告: 处理第 {idx} 行时出错: {e}")
            continue

    # 创建 DataFrame
    result_df = pd.DataFrame(converted_data)

    # 确保列顺序
    columns = ['title', 'author', 'isbn', 'location', 'coverUrl', 'status',
              'year', 'description', 'languageCode', 'availability',
              'category', 'circulationPolicy', 'totalCopies']
    result_df = result_df[columns]

    # 保存为 Excel
    os.makedirs(os.path.dirname(output_file), exist_ok=True)

    # 确保 ISBN 保存为文本格式，coverUrl 空值转为 None
    result_df['isbn'] = result_df['isbn'].astype(str)
    result_df['coverUrl'] = result_df['coverUrl'].replace('', None)

    # 使用 openpyxl 写入，并设置 ISBN 列为文本格式
    from openpyxl import Workbook
    from openpyxl.utils.dataframe import dataframe_to_rows

    wb = Workbook()
    ws = wb.active

    # 写入表头
    for r_idx, row in enumerate(dataframe_to_rows(result_df, index=False, header=True), 1):
        for c_idx, value in enumerate(row, 1):
            cell = ws.cell(row=r_idx, column=c_idx, value=value)
            # 如果是 ISBN 列（第3列），设置为文本格式
            if c_idx == 3 and r_idx > 1:  # 跳过表头
                cell.number_format = '@'  # @ 表示文本格式

    wb.save(output_file)

    print(f"  转换完成: {len(result_df)} 本图书")
    return len(result_df)


def main():
    """主函数"""
    print("=" * 60)
    print("DouBanSpider 数据转换工具")
    print("=" * 60)
    print()

    # DouBanSpider 数据目录
    source_dir = r"E:\图书馆书籍定位系统\DouBanSpider-master"

    # 输出目录
    output_dir = config.PROCESSED_DATA_DIR

    # 查找所有 Excel 文件
    excel_files = glob.glob(os.path.join(source_dir, "book_list-*.xlsx"))

    if not excel_files:
        print(f"错误: 在 {source_dir} 中没有找到 book_list-*.xlsx 文件")
        return

    print(f"找到 {len(excel_files)} 个 Excel 文件")
    print()

    total_books = 0
    isbn_offset = 0  # ISBN偏移量，确保每个文件的ISBN不重复

    # 转换每个文件
    for idx, excel_file in enumerate(excel_files, 1):
        output_file = os.path.join(
            output_dir,
            f"books_{idx:03d}.xlsx"
        )

        count = convert_douban_excel_to_system_format(excel_file, output_file, isbn_offset)
        total_books += count
        isbn_offset += count  # 更新偏移量

    print()
    print("=" * 60)
    print(f"转换完成！共转换 {total_books} 本图书")
    print(f"输出目录: {output_dir}")
    print("=" * 60)
    print()
    print("下一步: 运行批量导入")
    print("  python batch_importer.py")


if __name__ == "__main__":
    main()
