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

### 辅助工具

```bash
# 检查 DouBanSpider 数据列
python tools/check_douban_data.py

# 转换 DouBanSpider Excel 数据
python tools/convert_douban_data.py
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
├── book_import_tool/     # 可测试的工具包代码
├── tools/                # 辅助转换/检查脚本
├── scripts/              # 封面导入与补全脚本
├── docs/                 # Excel 导出说明、项目总结等文档
├── tests/                # pytest 单元测试
├── main.py               # 主入口脚本
├── config.py             # 兼容旧脚本的配置导出
├── requirements.txt      # Python 依赖列表
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
