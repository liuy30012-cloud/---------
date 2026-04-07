# 图书馆书籍定位系统 - 软件架构与开发指南

> **文档版本**: v3.0 | **最后更新**: 2026-04-05  
> **产品标识**: 图书馆书籍定位系统  
> **技术栈**: Spring Boot 3.2.4 + Vue 3.4 + TypeScript 5.2 + Electron 41

---

## 1. 项目概述

**"图书馆书籍定位系统"** 是一个面向高校图书馆的全栈智慧管理平台，采用前后端分离架构，提供书籍精准定位、智能搜索、借阅管理、AI 辅导阅读及数据可视化分析等一站式服务。系统同时提供 Web 浏览器端和基于 Electron 的 Windows 桌面客户端。

### 已实现的核心功能

- ✅ 书籍搜索定位（书名/作者/ISBN，多维度筛选）
- ✅ 完整借阅管理（借/还/续借，状态机流转，逾期罚款）
- ✅ 预约系统（排队逻辑，到期自动释放）
- ✅ 数据分析大屏（ECharts 可视化，实时统计）
- ✅ 库存预警 + 智能采购建议
- ✅ 用户认证（JWT + BCrypt + 登录锁定）
- ✅ 多层安全防护（限流/反爬/行为分析/蜜罐/水印）
- ✅ AI 智能助手（多轮对话，流式输出）
- ✅ 沉浸式水墨登录界面（15+ 种微交互）
- ✅ 10,000 首古典诗词库
- ✅ 桌面宠物（鸡蛋仔，多状态动画）
- ✅ 十二地支时钟 + 多语言支持（中/英）
- ✅ Electron 桌面端 + NSIS 安装包
- ✅ DDoS 防御模块

---

## 2. 系统架构与技术栈

### 2.1 后端服务层

| 技术选型 | 版本/说明 |
|---------|----------|
| **核心框架** | Spring Boot 3.2.4 |
| **安全框架** | Spring Security + JWT (jjwt 0.12.3) |
| **持久化** | Spring Data JPA + MySQL 8.0 |
| **验证** | Jakarta Validation |
| **加密** | BCrypt |
| **调度** | ShedLock 分布式任务调度 |
| **监控** | Spring Actuator + Micrometer + Prometheus |
| **简化** | Lombok |
| **监听端口** | `8080` |

### 2.2 前端展现层

| 技术选型 | 版本/说明 |
|---------|----------|
| **核心框架** | Vue 3.4 (Composition API `<script setup>`) |
| **构建工具** | Vite 5 |
| **类型安全** | TypeScript 5.2 |
| **状态管理** | Pinia |
| **路由** | Vue Router (Hash History) |
| **HTTP** | Axios |
| **图表** | ECharts 6 |
| **国际化** | Vue I18n |
| **开发端口** | `5173` |

### 2.3 桌面端封装层

| 技术选型 | 版本/说明 |
|---------|----------|
| **运行时** | Electron 41 |
| **打包工具** | electron-builder |
| **输出目标** | Windows NSIS 安装包 + 绿色免安装版 |
| **输出目录** | `frontend/dist_electron/` |

---

## 3. 物理目录结构

```text
图书馆书籍定位系统/
├── backend/                        # 后端项目 (Spring Boot)
│   └── src/main/java/com/library/
│       ├── config/                 # SecurityConfig, PrometheusConfig, ShedLockConfig
│       ├── controller/             # 9 个 REST 控制器
│       │   ├── AuthController          认证 (登录/注册/刷新/登出)
│       │   ├── BookController          书籍搜索与详情
│       │   ├── BorrowController        借阅管理 (借/还/续)
│       │   ├── ReservationController   预约管理
│       │   ├── StatisticsController    数据分析统计
│       │   ├── NotificationController  消息通知
│       │   ├── SearchHistoryController 搜索历史
│       │   ├── CaptchaController       验证码服务
│       │   └── HoneypotController      蜜罐陷阱端点
│       ├── dto/                    # 20 个数据传输对象
│       ├── exception/              # 全局异常处理
│       ├── filter/                 # 安全过滤器链
│       │   ├── JwtAuthenticationFilter     JWT 令牌验证
│       │   ├── RateLimitFilter             七层纵深限流 (344行)
│       │   ├── AntiCrawlerFilter           反爬虫检测
│       │   └── ResponseWatermarkFilter     响应数据水印
│       ├── model/                  # 8 个 JPA 实体
│       ├── repository/             # 3 个 Spring Data JPA 仓库
│       ├── scheduler/              # 定时任务 (逾期检查、预约释放、ShedLock)
│       ├── service/                # 8 个业务服务
│       │   ├── UserService              用户管理 (280行)
│       │   ├── BorrowService            借阅状态机 (320行)
│       │   ├── StatisticsService        数据分析引擎 (523行)
│       │   ├── RequestPatternAnalyzer   五维行为模式分析 (260行)
│       │   └── ... (4 个其他服务)
│       └── util/                   # 工具类 (JWT, 加密等)
│
├── frontend/                       # 前端/桌面项目 (Vue 3 + Electron)
│   ├── electron/                   # Electron 主进程
│   └── src/
│       ├── api/                    # 4 个 API 接口层
│       │   ├── bookApi.ts              书籍接口
│       │   ├── borrowApi.ts            借阅接口
│       │   ├── statisticsApi.ts        统计接口
│       │   └── antiCrawler.ts          前端反爬策略
│       ├── components/             # UI 组件库 (5 个子目录, 15+ 组件)
│       │   ├── hero/                   首页英雄区
│       │   ├── home/                   首页组件 (BookGrid, SidebarFilters, SuperButton 等)
│       │   ├── login/                  登录页视觉效果 (墨水/山水/竹/雾)
│       │   ├── navigation/             导航栏 + 用户菜单
│       │   └── panels/                 通知面板 + 搜索历史面板
│       ├── composables/            # 可组合逻辑
│       │   └── useSuperButton.ts       超级按钮 12 种微交互 (467行)
│       ├── data/                   # 数据资源
│       │   └── poemLibrary.ts          10,000 首古典诗词库 (~1.2MB)
│       ├── dizhi/                  # 十二地支时钟模块
│       ├── locales/                # 国际化语料 (中/英)
│       ├── pet/                    # 桌面宠物交互体系
│       │   ├── DesktopPet.vue          宠物主组件
│       │   ├── composables/            可组合逻辑 (usePetInteraction 等)
│       │   └── data/                   宠物状态数据
│       ├── router/                 # Vue Router 路由配置
│       ├── stores/                 # Pinia 状态管理 (user.ts)
│       ├── views/                  # 8 个页面视图
│       │   ├── Login.vue               沉浸式水墨登录页 (1,286行)
│       │   ├── Dashboard.vue           数据分析仪表盘
│       │   ├── BookSearch.vue          书籍搜索
│       │   ├── BookDetail.vue          书籍详情与定位
│       │   ├── MyBorrows.vue           我的借阅
│       │   ├── MyReservations.vue      我的预约
│       │   ├── InventoryAlerts.vue     库存预警
│       │   └── PurchaseSuggestions.vue 采购建议
│       ├── App.vue                 # 主组件 + 首页视图 (2,004行)
│       └── main.ts                 # 应用入口
│
├── ddos-defense/                   # DDoS 防御独立模块
│   ├── configs/                    Nginx/iptables 配置
│   ├── scripts/                    自动化防御脚本
│   └── monitoring/                 监控告警
│
└── docs/                           # 项目文档集 (12 份文档)
```

---

## 4. 核心代码统计

| 文件 | 行数 | 说明 |
|------|------|------|
| `poemLibrary.ts` | 10,011 | 诗词数据库（动态导入，不影响首屏） |
| `App.vue` | 2,004 | 首页 + 全局路由壳 |
| `Login.vue` | 1,286 | 沉浸式水墨登录（15+ 微交互） |
| `BookDetail.vue` | 737 | 书籍详情与定位 |
| `PurchaseSuggestions.vue` | 560 | 采购建议页面 |
| `StatisticsService.java` | 523 | 数据分析引擎（最大后端服务） |
| `BookSearch.vue` | 491 | 书籍搜索页 |
| `useSuperButton.ts` | 467 | 超级按钮 12 种微交互 |
| `InventoryAlerts.vue` | 429 | 库存预警页面 |
| `MyBorrows.vue` | 375 | 借阅管理页面 |
| `RateLimitFilter.java` | 344 | 七层纵深限流防御 |
| `BorrowService.java` | 320 | 借阅状态机（6 状态） |
| **后端源文件** | **65 个** | 10 个包 · 65 个 Java 类 |
| **前端源文件** | **51 个** | Vue/TS 文件 |

---

## 5. 运行与构建指南

### 5.1 前置要求

| 工具 | 最低版本 | 说明 |
|------|----------|------|
| Java | 17+ | 后端运行环境 |
| Node.js | 16+ | 前端构建环境 |
| Maven | 3.6+ | 后端依赖管理（或使用 IDE 内置） |
| MySQL | 8.0+ | 数据库 |

### 5.2 开发环境启动

**1. 启动 MySQL 数据库**
```sql
CREATE DATABASE IF NOT EXISTS library CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

**2. 配置后端环境变量**
```bash
cd backend
cp .env.example .env
# 编辑 .env，设置 DB_PASSWORD, JWT_SECRET 等
```

**3. 启动后端**
```bash
cd backend
mvn spring-boot:run          # 或使用 IDE 运行 LibraryApplication.java
# → http://localhost:8080
```

**4. 启动前端**
```bash
cd frontend
npm install                  # 首次运行
npm run dev                  # → http://localhost:5173
```

**5. (可选) 启动 Electron 桌面端**
```bash
cd frontend
npm run electron:dev
```

### 5.3 生产构建

```bash
# Web 版本
cd frontend && npm run build          # → dist/

# Windows 桌面安装包
cd frontend && npm run build:win      # → dist_electron/
```

### 5.4 测试账号

| 字段 | 值 |
|------|-----|
| 学号 | `2021001` |
| 密码 | `123456` |

---

## 6. API 接口速查

| 模块 | 方法 | 路径 | 需认证 |
|------|------|------|--------|
| 注册 | POST | `/api/auth/register` | ❌ |
| 登录 | POST | `/api/auth/login` | ❌ |
| 刷新 | POST | `/api/auth/refresh` | ❌ |
| 用户信息 | GET | `/api/auth/me` | ✅ |
| 修改密码 | POST | `/api/auth/change-password` | ✅ |
| 搜索书籍 | GET | `/api/books/search` | ❌ |
| 书籍详情 | GET | `/api/books/{id}` | ❌ |
| 借阅书籍 | POST | `/api/borrows` | ✅ |
| 归还书籍 | POST | `/api/borrows/{id}/return` | ✅ |
| 续借 | POST | `/api/borrows/{id}/renew` | ✅ |
| 预约书籍 | POST | `/api/reservations` | ✅ |
| 统计概览 | GET | `/api/statistics/overview` | ✅ |
| 库存预警 | GET | `/api/statistics/inventory-alerts` | ✅ |
| 采购建议 | GET | `/api/statistics/purchase-suggestions` | ✅ |

---

## 7. 代码规范与约定

### 7.1 前端规范
- **组件风格**: `<script setup lang="ts">` 组合式 API
- **CSS**: 全局 CSS 变量 + 组件内 `<style scoped>`
- **命名**: 组件文件 PascalCase，CSS 类名 kebab-case
- **国际化**: 所有用户可见文本通过 `$t('key')` 引用
- **模块化**: 复杂逻辑提取为 `composables/useXxx.ts`

### 7.2 后端规范
- **包结构**: `com.library.{config, controller, dto, exception, filter, model, repository, scheduler, service, util}`
- **REST 风格**: URL 以 `/api/` 为前缀，使用标准 HTTP 方法
- **响应格式**: 统一使用 `ApiResponse<T>` 泛型封装
- **安全**: 敏感配置使用环境变量（`${DB_PASSWORD}`, `${JWT_SECRET}`）

---

**最后更新**: 2026-04-05  
**版本**: v3.0
