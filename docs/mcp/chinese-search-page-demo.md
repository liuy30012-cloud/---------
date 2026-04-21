# 中文搜书的 MCP 页面演示

## 前置条件

1. 启动 demo 依赖：

```bash
docker compose -f backend/docker-compose.demo.yml up -d
```

2. 启动后端：

```bash
cd backend
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=demo"
```

3. 启动前端：

```bash
cd frontend
npm run dev
```

4. 如需补建索引：

```bash
curl -X POST http://localhost:8080/api/admin/elasticsearch/sync-all
```

## chrome-devtools MCP 演示流程

1. 打开页面：
   `http://localhost:5173/#/books/search`
2. 在搜索框输入 `设计模式` 并提交。
3. 验证页面上出现中文书目结果，且搜索辅助区域可见。
4. 打开网络请求，检查 `/api/smart-search/search?query=设计模式...` 的 JSON 响应：
   - `searchEngine` 为 `ELASTICSEARCH`
   - `books` 至少返回 1 条
5. 再依次搜索：
   - `计算机`
   - `深入理解计算机系统`
6. 停掉 Elasticsearch 后重复一次搜索，验证：
   - 页面仍有结果或至少无崩溃
   - 响应中的 `searchEngine` 切换为 `MYSQL_FALLBACK`

## 截图点位

- 搜索框输入中文关键词后的页面
- 结果列表中命中的中文书目卡片
- 搜索辅助区域中的解释、建议词
- 开发者工具 Network 中 `/api/smart-search/search` 的响应体，重点截出 `searchEngine`
