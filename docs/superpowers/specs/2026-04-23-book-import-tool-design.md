# 图书批量导入工具设计文档

## 项目概述

设计并实现一个自动化工具，从豆瓣读书 API 抓取 100,000 本真实图书数据，并批量导入到图书馆书籍定位系统中。

## 需求背景

当前系统馆藏为空（0 条记录），需要快速填充大量真实图书数据用于：
- 系统功能测试和演示
- 搜索功能性能测试
- 用户体验优化
- 数据分析功能验证

## 设计目标

1. **数据规模**：导入 100,000 本真实图书数据
2. **数据来源**：豆瓣读书 API
3. **数据质量**：确保数据完整性和准确性
4. **可靠性**：支持断点续传和错误恢复
5. **可维护性**：代码结构清晰，易于扩展

## 整体架构

### 数据流

```
豆瓣读书 API → Python 爬虫脚本 → CSV/Excel 文件 → 后端批量导入接口 → MySQL 数据库
```

### 核心组件

#### 1. 数据抓取模块 (douban_crawler.py)

**功能**：
- 从豆瓣 API 获取图书信息
- 处理反爬虫限制
- 数据清洗和格式转换
- 支持断点续传

**反爬虫策略**：
- 请求间隔：每次请求间隔 2-5 秒（随机）
- User-Agent 轮换：使用 fake-useragent 库模拟真实浏览器
- 请求头伪装：添加 Referer、Accept 等完整请求头
- IP 代理池（可选）：如果被封可以使用代理
- 错误重试：遇到 429/503 错误时指数退避重试

**数据字段映射**：
| 豆瓣字段 | 系统字段 | 说明 |
|---------|---------|------|
| title | title | 书名 |
| author | author | 作者（数组转字符串） |
| isbn13 | isbn | ISBN-13 编号 |
| pubdate | year | 出版年份 |
| summary | description | 简介 |
| images.large | coverUrl | 封面图片 URL |
| tags | category | 分类（取第一个标签） |
| publisher | - | 出版社（可选） |
| pages | - | 页数（可选） |

#### 2. 数据处理模块 (data_processor.py)

**功能**：
- 将抓取的 JSON 数据转换为系统需要的格式
- 生成符合导入模板的 Excel/CSV 文件
- 数据验证和清洗

**数据质量保证**：
- **必填字段验证**：title、author、isbn 必须存在
- **ISBN 格式校验**：确保 ISBN-10/ISBN-13 格式正确
- **去重处理**：基于 ISBN 去重，避免重复导入
- **数据清洗**：
  - 移除 HTML 标签
  - 统一日期格式（提取年份）
  - 处理特殊字符（转义或移除）
  - 作者数组转字符串（用逗号分隔）
  - 标签数组转分类（取第一个或最热门的）

**Excel 文件格式**：
```
| title | author | isbn | location | coverUrl | status | year | description | languageCode | availability | category | circulationPolicy | totalCopies |
```

#### 3. 批量导入模块 (batch_importer.py)

**功能**：
- 使用系统现有的 `/api/books/import` 接口
- 分批导入（每批 5,000 本）
- 错误处理和重试机制
- 导入进度跟踪

**导入策略**：
- **分批导入**：每个 Excel 文件包含 5,000 本图书
- **并发控制**：同时最多 3 个导入任务
- **失败处理**：记录失败的 ISBN，单独重试
- **认证处理**：使用管理员账号获取 JWT token

## 技术实现

### 性能优化

1. **分批抓取**：每批 1,000 本，避免内存溢出
2. **异步请求**：使用 asyncio + aiohttp 提升抓取速度（在限流允许范围内）
3. **本地缓存**：已抓取的数据保存到本地 JSON 文件，避免重复请求
4. **进度持久化**：每批完成后保存进度到 progress.json，支持断点续传

### 错误处理

#### 错误分类

1. **网络错误**：超时、连接失败
   - 处理：自动重试 3 次，每次间隔递增（1s, 2s, 4s）
   
2. **API 限流**：429 状态码
   - 处理：等待 60 秒后重试，如果持续失败则暂停 10 分钟
   
3. **数据格式错误**：字段缺失、格式不正确
   - 处理：记录日志，跳过该条，继续处理下一条
   
4. **导入失败**：后端返回错误
   - 处理：记录失败记录到 failed.json，生成失败报告

#### 日志记录

- **抓取日志** (logs/crawler.log)：
  - 记录每次请求的 URL、状态码、耗时
  - 记录成功/失败的图书数量
  
- **数据日志** (logs/processor.log)：
  - 记录数据验证结果
  - 记录清洗和转换过程
  
- **导入日志** (logs/importer.log)：
  - 记录每批导入的结果、耗时
  - 记录错误信息和失败原因

### 进度监控

**实时进度条**：
- 使用 tqdm 库显示进度条
- 显示当前抓取/导入进度

**统计信息**：
```
已抓取：45,230 / 100,000 (45.23%)
成功导入：40,000 本
失败：230 本
预计剩余时间：2 小时 15 分钟
```

## 文件结构

```
book-import-tool/
├── douban_crawler.py      # 豆瓣爬虫脚本
├── data_processor.py      # 数据处理和转换
├── batch_importer.py      # 批量导入脚本
├── config.py              # 配置文件
├── requirements.txt       # Python 依赖
├── main.py                # 主入口脚本
├── data/                  # 数据目录
│   ├── raw/              # 原始 JSON 数据
│   │   ├── batch_001.json
│   │   ├── batch_002.json
│   │   └── ...
│   ├── processed/        # 处理后的 Excel 文件
│   │   ├── books_001.xlsx
│   │   ├── books_002.xlsx
│   │   └── ...
│   ├── failed.json       # 失败记录
│   └── progress.json     # 进度记录
├── logs/                  # 日志文件
│   ├── crawler.log
│   ├── processor.log
│   └── importer.log
└── README.md             # 使用说明
```

## 配置文件 (config.py)

```python
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
```

## 使用流程

### 1. 环境准备

```bash
# 安装 Python 依赖
pip install -r requirements.txt

# 确保后端服务已启动
cd backend
mvn spring-boot:run
```

### 2. 执行抓取和导入

```bash
# 方式一：一键执行（推荐）
python main.py

# 方式二：分步执行
python douban_crawler.py      # 步骤 1：抓取数据
python data_processor.py      # 步骤 2：处理数据
python batch_importer.py      # 步骤 3：批量导入
```

### 3. 断点续传

如果中途中断，再次运行 `python main.py` 会自动从上次的进度继续。

### 4. 查看进度

```bash
# 查看实时日志
tail -f logs/crawler.log

# 查看进度文件
cat data/progress.json
```

## 预期时间估算

**假设条件**：
- 豆瓣 API 请求间隔：平均 3 秒/本
- 抓取 100,000 本：100,000 × 3 秒 = 300,000 秒 ≈ 83 小时 ≈ 3.5 天
- 批量导入：每批 5,000 本约 2 分钟，共 20 批 = 40 分钟

**总计**：约 4 天完成全部导入

**优化方案**：
- 如果使用代理池和更激进的并发策略，可以缩短到 1-2 天
- 如果豆瓣限流严格，可能需要 5-7 天

## 风险与应对

### 风险 1：豆瓣 API 封禁

**应对**：
- 降低请求频率（增加间隔到 5-10 秒）
- 使用代理池轮换 IP
- 分多个账号或 API Key 请求

### 风险 2：数据质量问题

**应对**：
- 严格的数据验证和清洗
- 人工抽查部分数据
- 提供数据修正工具

### 风险 3：导入失败

**应对**：
- 完善的错误处理和重试机制
- 记录失败数据，支持单独重试
- 分批导入，降低单次失败影响

### 风险 4：系统性能问题

**应对**：
- 分批导入，避免一次性导入过多数据
- 监控数据库性能，必要时优化索引
- 导入完成后运行 Elasticsearch 同步

## 后续优化

1. **数据补充**：
   - 补充图书位置信息（location）
   - 补充更详细的分类信息
   
2. **数据更新**：
   - 定期更新图书信息（如评分、评论数）
   - 补充新出版的图书
   
3. **多数据源**：
   - 支持从 Google Books API 获取数据
   - 支持从 Open Library 获取数据
   
4. **可视化界面**：
   - 开发 Web 界面监控抓取和导入进度
   - 提供数据统计和分析功能

## 成功标准

1. **数据量**：成功导入至少 95,000 本图书（95% 成功率）
2. **数据质量**：必填字段完整率 > 99%
3. **性能**：导入完成后系统响应时间 < 500ms
4. **稳定性**：支持断点续传，中断后可恢复

## 附录

### A. 豆瓣 API 示例

**搜索图书**：
```
GET https://api.douban.com/v2/book/search?q=python&count=20
```

**获取图书详情**：
```
GET https://api.douban.com/v2/book/:id
```

**响应示例**：
```json
{
  "id": "1003078",
  "title": "小王子",
  "author": ["圣埃克苏佩里"],
  "publisher": "人民文学出版社",
  "pubdate": "2003-8",
  "isbn13": "9787020042494",
  "pages": "100",
  "summary": "小王子是一个超凡脱俗的仙童...",
  "images": {
    "small": "https://img3.doubanio.com/view/subject/s/public/s1237549.jpg",
    "large": "https://img3.doubanio.com/view/subject/l/public/s1237549.jpg"
  },
  "tags": [
    {"count": 12345, "name": "童话"},
    {"count": 8901, "name": "法国文学"}
  ]
}
```

### B. 系统导入接口

**接口**：`POST /api/books/import`

**请求**：
- Content-Type: multipart/form-data
- Authorization: Bearer {JWT_TOKEN}
- Body: file (Excel/CSV 文件)

**响应**：
```json
{
  "code": 200,
  "message": "导入成功",
  "data": {
    "totalRows": 5000,
    "successCount": 4950,
    "failureCount": 50,
    "duplicateCount": 0,
    "errors": [
      {
        "row": 123,
        "isbn": "9787020042494",
        "reason": "ISBN 已存在"
      }
    ]
  }
}
```

### C. Python 依赖 (requirements.txt)

```
requests==2.31.0
aiohttp==3.9.1
fake-useragent==1.4.0
openpyxl==3.1.2
pandas==2.1.4
tqdm==4.66.1
python-dotenv==1.0.0
```
