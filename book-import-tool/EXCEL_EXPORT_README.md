# Excel 批量导出工具使用说明

## 功能说明

将 `data/raw/` 目录下的所有 `batch_*.json` 文件中的图书数据导出为 Excel 文件，每个 Excel 文件包含 5000 本图书。

## 使用方法

### 1. 安装依赖

```bash
pip install -r requirements.txt
```

### 2. 准备数据

确保 `data/raw/` 目录下有 `batch_*.json` 格式的批次文件。

### 3. 运行导出

```bash
python excel_exporter.py
```

### 4. 查看结果

导出的 Excel 文件保存在 `data/processed/` 目录下，文件名格式为 `books_001.xlsx`, `books_002.xlsx` 等。

## 输出格式

每个 Excel 文件包含以下列：

| 列名 | 说明 | 数据来源 |
|------|------|----------|
| ISBN | 图书 ISBN 号 | isbn13 或 isbn10 |
| 书名 | 图书标题 | title |
| 作者 | 作者列表（逗号分隔） | author |
| 出版社 | 出版社名称 | publisher |
| 出版日期 | 出版日期 | pubdate |
| 价格 | 图书价格 | price |
| 页数 | 页数 | pages |
| 装帧 | 装帧方式 | binding |
| 分类 | 图书分类（取第一个标签） | tags[0].name |
| 简介 | 图书简介 | summary |
| 封面URL | 封面图片链接 | images.large |
| 豆瓣评分 | 豆瓣评分 | rating.average |
| 位置 | 图书位置 | 默认值: "待分配" |
| 状态 | 图书状态 | 默认值: "available" |
| 可用性 | 可用性状态 | 默认值: "available" |
| 流通策略 | 流通策略 | 默认值: "AUTO" |
| 总副本数 | 总副本数量 | 默认值: 3 |
| 语言 | 语言代码 | 默认值: "zh" |

## 配置说明

在 `config.py` 中可以修改以下配置：

- `EXCEL_BATCH_SIZE`: 每个 Excel 文件包含的图书数量（默认 5000）
- `DEFAULT_LOCATION`: 默认位置（默认 "待分配"）
- `DEFAULT_STATUS`: 默认状态（默认 "available"）
- `DEFAULT_LANGUAGE`: 默认语言（默认 "zh"）
- `DEFAULT_AVAILABILITY`: 默认可用性（默认 "available"）
- `DEFAULT_CIRCULATION_POLICY`: 默认流通策略（默认 "AUTO"）
- `DEFAULT_TOTAL_COPIES`: 默认总副本数（默认 3）

## 错误处理

如果某些图书数据处理失败，失败记录会保存到 `data/failed.json` 文件中，包含失败的图书数据和错误信息。

## 示例

```bash
# 运行导出
$ python excel_exporter.py
开始导出 Excel 文件...
找到 2 个批次文件
已加载 batch_001.json: 2 本图书
已加载 batch_002.json: 10 本图书
总共加载 12 本图书
正在导出 data/processed\books_001.xlsx (12 本图书)...
已完成 1/1
导出完成！
```

## 注意事项

1. 确保 `data/raw/` 目录存在且包含有效的批次文件
2. 导出前会自动创建 `data/processed/` 目录
3. 如果目标文件已存在，会被覆盖
4. Excel 文件编号从 001 开始，格式为 `books_001.xlsx`, `books_002.xlsx` 等
