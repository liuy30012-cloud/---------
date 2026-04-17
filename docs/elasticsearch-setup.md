# Elasticsearch 部署指南

## 环境要求

- Elasticsearch 8.x
- IK 中文分词器插件
- 内存：建议 4GB+

## 安装步骤

### 1. 安装 Elasticsearch

使用 Docker：
```bash
docker run -d \
  --name elasticsearch \
  -p 9200:9200 \
  -p 9300:9300 \
  -e "discovery.type=single-node" \
  -e "ES_JAVA_OPTS=-Xms2g -Xmx2g" \
  docker.elastic.co/elasticsearch/elasticsearch:8.11.0
```

### 2. 安装 IK 分词器

```bash
docker exec -it elasticsearch \
  elasticsearch-plugin install \
  https://github.com/medcl/elasticsearch-analysis-ik/releases/download/v8.11.0/elasticsearch-analysis-ik-8.11.0.zip

docker restart elasticsearch
```

### 3. 配置应用

在 `.env` 文件中配置：
```
ES_URIS=http://localhost:9200
ES_USERNAME=elastic
ES_PASSWORD=your_password
ES_ENABLED=true
```

### 4. 执行全量同步

启动应用后：
```bash
curl -X POST http://localhost:8080/api/admin/elasticsearch/sync-all
```

## 验证

```bash
# 检查索引
curl -X GET "http://localhost:9200/books/_count"

# 测试搜索
curl -X GET "http://localhost:9200/books/_search?q=java"
```
