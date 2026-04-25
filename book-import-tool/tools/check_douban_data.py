"""检查 DouBanSpider 数据格式"""
import pandas as pd
import os

# 读取一个示例文件
excel_file = r"E:\图书馆书籍定位系统\DouBanSpider-master\book_list-名著.xlsx"

print(f"Reading: {excel_file}")
df = pd.read_excel(excel_file)

print(f"\n列名: {list(df.columns)}")
print(f"行数: {len(df)}")
print(f"\n前 3 行数据:")
print(df.head(3))

print(f"\n数据类型:")
print(df.dtypes)
