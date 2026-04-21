# Demo Dataset

## Run the demo stack

1. Start MySQL and Elasticsearch:

```bash
docker compose -f backend/docker-compose.demo.yml up -d
```

2. Copy `backend/.env.demo.example` to `backend/.env` or export the same variables.

3. Run the backend with the `demo` profile:

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=demo
```

When the `books` table is empty, the backend will seed the built-in Chinese showcase file at `classpath:/demo/chinese-showcase-books.csv` first, then import `classpath:/demo/openlibrary-demo-books.csv.gz` when that bulk file exists, and finally run one full Elasticsearch sync.

## Generate the Open Library dataset

Use the generator script to build the compressed CSV resource:

```bash
python backend/scripts/generate_openlibrary_demo_dataset.py \
  --output backend/src/main/resources/demo/openlibrary-demo-books.csv.gz \
  --target-count 100000
```

The script reads official Open Library dumps, resolves author names from the author dump, and writes a 13-column CSV that matches the admin import template.

## Chinese demo verification

After the backend starts with the `demo` profile and the frontend is running, verify these queries on `http://localhost:5173/#/books/search`:

- `设计模式`
- `深入理解计算机系统`
- `计算机`

If the Elasticsearch index is empty, trigger a full sync:

```bash
curl -X POST http://localhost:8080/api/admin/elasticsearch/sync-all
```
