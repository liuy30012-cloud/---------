# 图书管理 API

## 权限要求
所有接口仅限 ADMIN 角色访问。

## 接口列表

### 1. 创建图书
**POST** `/api/books`

**请求体：**
```json
{
  "title": "图书标题",
  "author": "作者",
  "isbn": "978-1234567890",
  "location": "A区->1层->计算机类->001",
  "coverUrl": "https://example.com/cover.jpg",
  "status": "良好",
  "year": "2024",
  "description": "图书描述",
  "languageCode": "zh",
  "availability": "可借阅",
  "category": "计算机",
  "circulationPolicy": "AUTO",
  "totalCopies": 5
}
```

**响应：**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "title": "图书标题",
    ...
  }
}
```

### 2. 更新图书
**PUT** `/api/books/{id}`

**请求体：** 与创建图书相同

**响应：** 返回更新后的图书信息

### 3. 删除图书
**DELETE** `/api/books/{id}`

**响应：**
```json
{
  "success": true,
  "data": null
}
```

### 4. 批量删除
**DELETE** `/api/books/batch`

**请求体：**
```json
{
  "bookIds": [1, 2, 3]
}
```

**响应：**
```json
{
  "success": true,
  "data": {
    "successCount": 2,
    "failedCount": 1,
    "failures": [
      {
        "bookId": 3,
        "reason": "图书有未归还的借阅记录"
      }
    ]
  }
}
```

### 5. 批量导入
**POST** `/api/books/import`

**请求：** multipart/form-data，文件参数名为 `file`

**支持格式：** .xlsx, .xls, .csv

**响应：**
```json
{
  "success": true,
  "data": {
    "successCount": 10,
    "failedCount": 2,
    "failures": [
      {
        "row": 5,
        "reason": "ISBN 已存在"
      }
    ]
  }
}
```

### 6. 下载导入模板
**GET** `/api/books/import/template`

**响应：** Excel 文件下载

## 错误码

- 400: 请求参数错误
- 403: 权限不足
- 404: 资源不存在
- 409: 冲突（如 ISBN 重复、有关联数据）
