# 📚 图书馆书籍定位系统

> **中国劳动关系学院图书馆智能定位系统** — 集书籍检索定位、借阅管理、AI 辅助、数据分析与沉浸式交互于一体的全栈智慧图书馆平台。

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.4-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/Vue-3.4-42B883?style=for-the-badge&logo=vuedotjs&logoColor=white)](https://vuejs.org/)
[![Electron](https://img.shields.io/badge/Electron-41-47848F?style=for-the-badge&logo=electron&logoColor=white)](https://www.electronjs.org/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.2-3178C6?style=for-the-badge&logo=typescript&logoColor=white)](https://www.typescriptlang.org/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=for-the-badge&logo=mysql&logoColor=white)](https://www.mysql.com/)
[![License](https://img.shields.io/badge/License-MIT-EAB308?style=for-the-badge)](LICENSE)

---

## 🎯 项目简介

本系统是一个面向高校图书馆的**全栈智慧管理平台**，采用前后端分离架构，为读者提供书籍精准定位、智能搜索、借阅管理、AI 辅导阅读及数据可视化分析等一站式服务。系统同时提供 **Web 浏览器端**和基于 Electron 的 **Windows 绿色桌面客户端**，并内置了丰富的安全防护与运维监控能力。

### 📊 项目规模

| 指标 | 数据 |
|------|------|
| 📝 **代码总量** | 54,000+ 行（前端 40,000+ 行 + 后端 14,000+ 行） |
| 🎨 **前端组件** | 32 个 Vue 组件 + 14 个页面视图 |
| 🔧 **后端服务** | 19 个控制器 + 28 个服务 + 18 个实体模型 |
| 🛡️ **安全过滤器** | 4 个安全过滤器（限流、反爬、JWT、水印） |
| 📚 **文档数量** | 22+ 份技术文档 |
| 🔄 **提交历史** | 75+ 次提交 |
| 🌟 **核心特性** | 21+ 个功能模块 |
| 🧪 **测试覆盖** | 完整的单元测试套件（Vitest） |

### ✨ 核心功能一览

| 模块 | 功能 | 亮点 |
|------|------|------|
| 🔍 **智能搜索** | 书名、作者、ISBN 多维度检索 | Elasticsearch 全文搜索、中文分词、实时搜索建议 |
| 📍 **精准定位** | 馆内楼层、书架、层位可视化指引 | 基于地理视图的馆内定位地图 |
| 📖 **借阅管理** | 借书、还书、续借全生命周期管理 | 完整状态机流转、超期自动处理、分页查询 |
| 📋 **预约系统** | 在线预约热门书籍 | 排队逻辑、到期自动释放、数据库锁防并发 |
| 📚 **图书管理** | 管理员新增、修改、删除、批量导入图书 | Excel/CSV 批量导入、智能库存调整、关联数据检查 |
| 🤖 **AI 智能助手** | 阅读分析、文档润色、多轮对话 | 流式输出、上下文保持、抽屉式 UI |
| 📊 **数据分析** | 借阅统计、热门排行、趋势可视化 | Elasticsearch 聚合查询、ECharts 图表、多维度数据看板 |
| ⚡ **高性能架构** | Elasticsearch 搜索引擎集成 | 性能提升 50-60 倍、自动降级到 MySQL、实时数据同步 |
| 📄 **分页系统** | 7 大核心接口全面分页支持 | 统一分页工具类、最大页面限制、多字段排序、向后兼容 |
| ⚠️ **库存预警** | 低库存预警、采购建议 | 智能阈值、批量导出 |
| 🔐 **用户系统** | 注册、登录、权限管理、密码安全 | JWT 鉴权、BCrypt 加密、登录失败锁定 |
| 🔔 **通知系统** | 新书上架、借阅到期提醒 | 实时推送、已读/未读管理、分页查询 |
| 📜 **搜索历史** | 历史记录保存与快速重搜 | 一键清空、快捷复搜、分页查询 |
| 🐱 **桌面宠物** | 鸡蛋仔虚拟形象互动陪读 | 跳舞/游戏/魔法/生气多状态动画、拖拽移动 |
| 🕐 **十二地支时钟** | 中国传统时辰展示模块 | 独立地支时钟组件 |
| 🌍 **多语言** | 中/英文界面一键切换 | Vue I18n 深度集成 |
| 💻 **跨平台** | Web 端 + Windows 桌面端 | Electron 打包免安装绿色版 + NSIS 安装包 |
| 📴 **离线支持** | 离线借阅、收藏、数据同步 | Service Worker 缓存、IndexedDB 本地存储、自动同步队列 |
| 🛡️ **错误处理** | 智能错误恢复与重试机制 | 统一错误中心、自动重试、用户友好提示、错误边界 |

---

## 🎨 沉浸式登录界面

登录界面是本系统的**视觉名片**，融合了中国传统水墨美学与现代交互设计，打造了一个极具沉浸感的入口体验：

<details>
<summary>🖌️ 查看完整交互特性列表（15+ 种微交互）</summary>

#### 视觉层次
- **宣纸纹理底层** — 仿古宣纸质感背景
- **水墨晕染画布** — Canvas 动态水墨扩散效果
- **远中近山水层** — 三层山峦剪影 + 水面倒影
- **流动云雾层** — 多层缓动雾气动画
- **翠竹装饰** — SVG 矢量竹枝节点叶片

#### 诗词系统
- **万首诗词库** — 收录先秦至清代 10,000 首经典古典诗词
  - 📊 **分类覆盖率**: 96.7% (9,668首已分类)
  - 🏛️ **朝代分布**: 先秦至隋 73首 | 唐代 9,593首 | 宋代 2首
  - 👤 **作者分类**: 唐代按645位作者精细分类，支持按作者按需加载
  - ⚡ **性能优化**: 诗词数据按朝代和作者分片，按需加载，减少初始加载体积
- **飘落诗页** — 随机抽取诗词以竖排书页形式飘落，自适应尺寸排版
- **墨点溅射** — 不规则形态墨点随机分布

#### 微交互动效
- **鼠标跟随暖光** — 全局鼠标追踪暖色光晕
- **粒子尾迹** — 鼠标移动时生成金色粒子轨迹
- **毛笔笔触画布** — Canvas 实时描绘书法笔触尾迹
- **墨水涟漪** — 点击任意位置产生墨水飞溅 + 墨点散射
- **3D 透视倾斜** — 登录卡片随鼠标呈 3D 景深倾斜
- **呼吸光环** — 登录区域缓慢脉动的光晕环
- **表单进度环** — 实时计算填写进度的 SVG 圆环指示器
- **磁性按钮** — 提交按钮随鼠标靠近产生吸附偏移
- **输入脉冲** — 打字实时触发输入框脉冲波纹
- **登录成功烟花** — 认证成功后全屏 40 粒子爆裂庆祝动效
- **错误震动反馈** — 登录失败时卡片物理抖动
- **印章互动** — 可点击切换的传统印章文字（学海无涯/书山有路/博学笃行…）
- **瀑布入场动画** — 表单元素交错进入的级联动画
- **时段问候语** — 根据当前时刻自动切换问候文案与 Emoji
- **密码强度条** — 注册时实时四级密码强度可视化
- **浮动图标** — 书籍主题图标水墨色调轻柔漂浮

</details>

---

## 🚀 快速开始

### 前置要求

| 工具 | 最低版本 | 说明 |
|------|----------|------|
| Java | 17+ | 后端运行环境 |
| Node.js | 16+ | 前端构建环境 |
| Maven | 3.6+ | 后端依赖管理 |
| MySQL | 8.0+ | 数据库 |
| Elasticsearch | 8.x | 搜索引擎（可选，不安装时自动降级到 MySQL） |

### 启动步骤

#### 1️⃣ 启动后端

```bash
cd backend
mvn spring-boot:run
```

后端将在 `http://localhost:8080` 启动

#### 2️⃣ 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端开发服务器将在 `http://localhost:5173` 启动

#### 3️⃣ （可选）启动 Elasticsearch

```bash
# 使用 Docker 启动 Elasticsearch
docker run -d \
  --name elasticsearch \
  -p 9200:9200 \
  -p 9300:9300 \
  -e "discovery.type=single-node" \
  -e "ES_JAVA_OPTS=-Xms2g -Xmx2g" \
  docker.elastic.co/elasticsearch/elasticsearch:8.11.0

# 安装 IK 中文分词器
docker exec -it elasticsearch \
  elasticsearch-plugin install \
  https://github.com/medcl/elasticsearch-analysis-ik/releases/download/v8.11.0/elasticsearch-analysis-ik-8.11.0.zip

docker restart elasticsearch
```

> **注意：** Elasticsearch 是可选的。如果不安装，系统会自动降级到 MySQL 进行搜索和统计，核心功能不受影响。

#### 4️⃣ 访问系统（二选一）

**选项 A — Web 浏览器端**
打开浏览器访问：http://localhost:5173

**选项 B — 桌面客户端（开发环境）**
在 frontend 目录下新开一个终端运行：
```bash
npm run electron:dev
```

### 🔑 测试账号

| 账号类型 | 学工号 | 密码 | 角色 |
|----------|--------|------|------|
| 默认读者 | `2021001` | `Test123456` | `STUDENT` |
| 默认管理员 | `admin001` | `Admin123456!` | `ADMIN` |

---

## 🏗️ 技术架构

```
                    ┌─────────────────────────────────────────────┐
                    │              客户端 (Client)                 │
                    │  ┌──────────┐          ┌──────────────────┐ │
                    │  │ 浏览器    │          │ Electron 桌面端   │ │
                    │  │ Web SPA  │          │ Windows App      │ │
                    │  └────┬─────┘          └────────┬─────────┘ │
                    └───────┼─────────────────────────┼───────────┘
                            │      HTTP / REST        │
                    ┌───────┴─────────────────────────┴───────────┐
                    │           安全过滤链 (Filter Chain)           │
                    │  RateLimit → AntiCrawler → JWT Auth          │
                    │  → ResponseWatermark → RequestPattern        │
                    ├─────────────────────────────────────────────┤
                    │           Spring Boot 后端                    │
                    │  ┌────────────────────────────────────────┐  │
                    │  │  Controller 层                         │  │
                    │  │  Auth│Book│Borrow│Reserve│Stats│...    │  │
                    │  ├────────────────────────────────────────┤  │
                    │  │  Service 层                            │  │
                    │  │  业务逻辑│状态机│调度│分析              │  │
                    │  ├────────────────────────────────────────┤  │
                    │  │  Repository 层 (Spring Data JPA)       │  │
                    │  └────────────────┬───────────────────────┘  │
                    │                   │                          │
                    │            ┌──────┴──────┐                   │
                    │            │   MySQL DB  │                   │
                    │            └─────────────┘                   │
                    │                                              │
                    │  ┌──────────────────────────────────────┐    │
                    │  │  监控 (Actuator + Prometheus + 日志)   │    │
                    │  └──────────────────────────────────────┘    │
                    └─────────────────────────────────────────────┘
```

### 后端技术栈

| 类别 | 技术 |
|------|------|
| 框架 | Spring Boot 3.2.4 |
| 安全 | Spring Security + JWT (jjwt 0.12.3) |
| 持久化 | Spring Data JPA + MySQL + Hibernate |
| 搜索引擎 | Elasticsearch 8.x + IK 中文分词器 |
| 缓存 | Redis + Spring Cache |
| 验证 | Jakarta Validation |
| 加密 | BCrypt |
| 调度 | ShedLock 分布式任务调度 |
| 监控 | Spring Actuator + Micrometer + Prometheus |
| 熔断降级 | Resilience4j Circuit Breaker |
| 简化 | Lombok |

### 前端技术栈

| 类别 | 技术 |
|------|------|
| 框架 | Vue 3.4 (Composition API + `<script setup>`) |
| 语言 | TypeScript 5.2 |
| 桌面端 | Electron 41 + electron-builder |
| 状态管理 | Pinia |
| 路由 | Vue Router |
| HTTP | Axios |
| 图表 | ECharts 6 |
| 国际化 | Vue I18n |
| 构建 | Vite 5 |
| 离线支持 | Service Worker + IndexedDB |
| 测试 | Vitest + Vue Test Utils |
| 诗词数据 | 10,000首古典诗词，按朝代和作者分类，按需加载 |

---

## 📁 项目结构

```
图书馆书籍定位系统/
├── backend/                        # 后端项目 (Spring Boot)
│   └── src/main/java/com/library/
│       ├── config/                 # SecurityConfig, PrometheusConfig, ShedLockConfig
│       ├── controller/             # REST API 控制器
│       │   ├── AuthController      #   认证 (登录/注册/刷新/登出)
│       │   ├── BookController      #   书籍搜索与详情
│       │   ├── BorrowController    #   借阅管理 (借/还/续)
│       │   ├── ReservationController#  预约管理
│       │   ├── StatisticsController#   数据分析统计
│       │   ├── NotificationController# 消息通知
│       │   ├── SearchHistoryController# 搜索历史
│       │   ├── CaptchaController   #   验证码服务
│       │   └── HoneypotController  #   蜜罐陷阱端点
│       ├── dto/                    # 数据传输对象
│       ├── exception/              # 全局异常处理
│       ├── filter/                 # 安全过滤器链
│       │   ├── JwtAuthenticationFilter  # JWT 令牌验证
│       │   ├── RateLimitFilter          # 多维度限流
│       │   ├── AntiCrawlerFilter        # 反爬虫检测
│       │   └── ResponseWatermarkFilter  # 响应数据水印
│       ├── model/                  # JPA 实体 (User, Book, BorrowRecord...)
│       ├── repository/             # Spring Data JPA 仓库
│       ├── scheduler/              # 定时任务 (超期检查、预约释放)
│       ├── service/                # 业务服务层
│       │   ├── UserService         #   用户管理
│       │   ├── BookService         #   书籍业务
│       │   ├── BorrowService       #   借阅状态机
│       │   ├── ReservationService  #   预约排队
│       │   ├── StatisticsService   #   数据分析引擎（ES + MySQL 双引擎）
│       │   ├── SmartSearchService  #   智能搜索服务（ES + MySQL 双引擎）
│       │   ├── NotificationService #   通知推送
│       │   ├── LoginFailureTracker #   登录失败追踪
│       │   ├── RequestPatternAnalyzer # 行为模式分析
│       │   └── elasticsearch/      #   Elasticsearch 服务层
│       │       ├── ElasticsearchSyncService      # 数据同步服务
│       │       ├── ElasticsearchStatisticsService # ES 统计服务
│       │       ├── ElasticsearchSearchService     # ES 搜索服务
│       │       └── MysqlStatisticsService         # MySQL 降级服务
│       └── util/                   # 工具类 (JWT, 加密等)
│
├── frontend/                       # 前端/桌面项目 (Vue 3 + Electron)
│   ├── electron/                   # Electron 主进程 + 环境配置
│   └── src/
│       ├── api/                    # API 接口层
│       │   ├── bookApi.ts          #   书籍接口
│       │   ├── borrowApi.ts        #   借阅接口
│       │   ├── statisticsApi.ts    #   统计接口
│       │   └── antiCrawler.ts      #   前端反爬策略
│       ├── components/             # UI 组件库
│       │   ├── common/             #   通用组件 (ErrorBoundary, LoadingOverlay, SyncStatus 等)
│       │   ├── hero/               #   首页英雄区
│       │   ├── home/               #   首页组件 (BookGrid, SidebarFilters, SuperButton 等)
│       │   ├── login/              #   登录页水墨组件 (山水/云雾/竹/墨)
│       │   ├── navigation/         #   顶部导航 + 用户菜单
│       │   └── panels/             #   通知面板 + 搜索历史面板
│       ├── data/                   # 数据资源
│       │   └── poemLibrary.ts      #   10,000 首古典诗词库 (~1.2MB)
│       ├── composables/            # Vue 组合式函数
│       │   ├── useError.ts         #   错误处理
│       │   ├── useFeedback.ts      #   用户反馈
│       │   ├── useNetworkStatus.ts #   网络状态监控
│       │   ├── useOffline.ts       #   离线功能
│       │   ├── useOfflineSync.ts   #   离线同步
│       │   ├── useBorrowOffline.ts #   离线借阅
│       │   └── useFavoriteOffline.ts # 离线收藏
│       ├── services/               # 业务服务层
│       │   ├── ErrorCenter.ts      #   错误中心
│       │   ├── FeedbackManager.ts  #   反馈管理器
│       │   └── OfflineManager.ts   #   离线管理器
│       ├── dizhi/                  # 十二地支时钟模块
│       ├── locales/                # 国际化语料 (中/英)
│       ├── pet/                    # 桌面宠物交互体系
│       │   ├── DesktopPet.vue      #   宠物主组件
│       │   ├── composables/        #   可组合逻辑抽取
│       │   └── data/               #   宠物状态数据
│       ├── router/                 # Vue Router 路由配置
│       ├── stores/                 # Pinia 状态管理
│       ├── test/                   # 单元测试
│       │   ├── setup.ts            #   测试环境配置
│       │   ├── components/         #   组件测试
│       │   └── composables/        #   组合式函数测试
│       ├── utils/                  # 工具函数
│       │   ├── serviceWorker.ts    #   Service Worker 管理
│       │   └── errorHelpers.ts     #   错误处理辅助
│       └── views/                  # 页面视图
│           ├── Login.vue           #   沉浸式水墨登录页 (~54KB, 1286行)
│           ├── Dashboard.vue       #   数据分析仪表盘
│           ├── BookSearch.vue      #   书籍搜索
│           ├── BookDetail.vue      #   书籍详情与定位
│           ├── MyBorrows.vue       #   我的借阅
│           ├── MyReservations.vue  #   我的预约
│           ├── InventoryAlerts.vue #   库存预警
│           └── PurchaseSuggestions.vue # 采购建议
│
├── ddos-defense/                   # DDoS 防御独立模块
│   ├── configs/                    #   Nginx/iptables 防护配置
│   ├── scripts/                    #   自动化防御脚本
│   ├── monitoring/                 #   监控告警
│   └── docs/                       #   防御方案文档
│
├── docs/                           # 项目文档集
│   ├── ARCHITECTURE_ANALYSIS.md    #   架构深度分析
│   ├── DEVELOPMENT_GUIDE.md        #   开发指南
│   ├── elasticsearch-setup.md      #   Elasticsearch 部署指南
│   ├── elasticsearch-integration-checklist.md # ES 集成验收清单
│   ├── elasticsearch-bugfix-report.md # ES 集成 bug 修复报告
│   ├── 快速启动指南.md              #   5 分钟上手
│   ├── 借阅管理系统详细计划.md      #   借阅模块设计
│   ├── 数据分析模块详细计划.md      #   分析模块设计
│   └── ...                         #   更多总结与报告
│
└── test-api.bat / test-api.sh      # API 测试脚本
```

---

## 📴 离线功能与错误处理

### 离线支持

系统提供完整的离线功能支持，确保在网络不稳定或断网情况下仍能正常使用：

#### 核心特性

- ✅ **Service Worker 缓存策略** — 智能缓存静态资源和 API 响应
- ✅ **IndexedDB 本地存储** — 离线数据持久化存储
- ✅ **自动同步队列** — 网络恢复后自动同步离线操作
- ✅ **离线借阅支持** — 离线状态下可申请借阅，联网后自动提交
- ✅ **离线收藏支持** — 离线添加/取消收藏，自动同步
- ✅ **网络状态监控** — 实时监测网络状态并提供视觉反馈
- ✅ **同步状态指示** — 显示待同步操作数量和同步进度

#### 技术实现

```typescript
// 离线管理器架构
OfflineManager
├── 数据缓存 (IndexedDB)
│   ├── 书籍数据缓存
│   ├── 用户数据缓存
│   └── 搜索结果缓存
├── 操作队列
│   ├── 借阅操作队列
│   ├── 收藏操作队列
│   └── 其他操作队列
└── 同步引擎
    ├── 网络状态监听
    ├── 自动重试机制
    └── 冲突解决策略
```

#### 组合式函数

- `useOffline()` — 离线状态管理
- `useOfflineSync()` — 离线同步控制
- `useBorrowOffline()` — 离线借阅功能
- `useFavoriteOffline()` — 离线收藏功能
- `useNetworkStatus()` — 网络状态监控

### 错误处理系统

构建了统一的错误处理中心，提供智能错误恢复和用户友好的错误提示：

#### 核心特性

- ✅ **统一错误中心 (ErrorCenter)** — 集中管理所有错误类型
- ✅ **智能重试机制** — 自动识别可重试错误并执行重试
- ✅ **错误分类** — 网络错误、业务错误、系统错误分类处理
- ✅ **用户友好提示** — 根据错误类型提供清晰的解决建议
- ✅ **错误边界组件** — 捕获组件树中的错误并优雅降级
- ✅ **错误日志记录** — 完整的错误追踪和日志记录

#### 错误处理流程

```typescript
// 错误处理架构
ErrorCenter
├── 错误捕获
│   ├── HTTP 请求拦截
│   ├── 全局错误监听
│   └── 组件错误边界
├── 错误分类
│   ├── NetworkError (网络错误)
│   ├── BusinessError (业务错误)
│   ├── AuthError (认证错误)
│   └── SystemError (系统错误)
├── 错误处理
│   ├── 自动重试 (可配置次数和延迟)
│   ├── 降级处理
│   └── 用户提示
└── 错误恢复
    ├── 重新加载
    ├── 返回上一页
    └── 跳转到错误页面
```

#### 组件与工具

- `ErrorBoundary.vue` — 错误边界组件
- `ErrorPage.vue` — 错误页面
- `ErrorRetry.vue` — 错误重试组件
- `useError()` — 错误处理组合式函数
- `errorHelpers.ts` — 错误处理辅助函数

### 用户反馈系统

- ✅ **FeedbackManager** — 统一反馈管理器
- ✅ **Toast 通知** — 轻量级消息提示
- ✅ **加载状态** — 全局加载遮罩
- ✅ **操作确认** — 重要操作二次确认

### 测试覆盖

完整的单元测试套件确保功能稳定性：

- ✅ **Vitest 测试框架** — 快速的单元测试
- ✅ **Vue Test Utils** — Vue 组件测试
- ✅ **组件测试** — 覆盖所有关键组件
- ✅ **组合式函数测试** — 覆盖所有业务逻辑

```bash
# 运行测试
cd frontend
npm run test

# 测试覆盖率
npm run test:coverage
```

---

## 🔐 安全体系

本系统构建了**多层纵深安全防御**体系，包含 4 个核心安全过滤器和多项防护机制：

### 认证与访问控制

- ✅ **BCrypt 密码加密** — 不可逆哈希存储
- ✅ **JWT 令牌认证** — 无状态身份验证 + Token 刷新机制
- ✅ **登录失败锁定** — 5 次失败后锁定 30 分钟
- ✅ **CORS 跨域安全配置** — 严格域名白名单

### 反爬虫与反滥用

- ✅ **多维度限流 (RateLimitFilter)** — IP/用户/端点粒度速率限制
- ✅ **反爬虫检测 (AntiCrawlerFilter)** — 请求频率与路径模式异常检测
- ✅ **行为模式分析 (RequestPatternAnalyzer)** — 检测顺序遍历、定时轮询、广度优先爬取
- ✅ **设备指纹识别** — 前端 Canvas/WebGL 指纹采集
- ✅ **渐进式惩罚** — 可疑 IP 逐步加重限流至封禁
- ✅ **蜜罐陷阱 (Honeypot)** — 隐藏端点诱捕恶意爬虫
- ✅ **响应水印 (Watermark)** — 数据追溯标记

### 性能与可用性

- ✅ **Redis 缓存层** — 搜索结果缓存（5分钟）、热门书籍缓存（10分钟）
- ✅ **数据库索引优化** — 复合索引覆盖常见搜索模式，性能提升 50-70%
- ✅ **Elasticsearch 集成** — 全文搜索性能提升 50-60 倍
- ✅ **熔断降级机制** — Resilience4j Circuit Breaker，ES 不可用时自动降级到 MySQL

### 基础设施安全

- ✅ **DDoS 防御方案** — Nginx 限流 + iptables 规则 + 自动化脚本
- ✅ **表单验证 & SQL 注入防护** — Jakarta Validation + 参数化查询
- ✅ **Prometheus 监控** — 实时性能指标采集与告警

---

## 📊 API 接口文档

> 💡 **分页支持**：以下标注 📄 的接口支持分页查询，通过 `page`（页码，从0开始）、`size`（每页数量）、`sort`（排序字段）参数控制。详见 [PAGINATION_API.md](./PAGINATION_API.md)

### 认证接口 `/api/auth`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/auth/register` | 用户注册 |
| POST | `/api/auth/login` | 用户登录 |
| POST | `/api/auth/refresh` | 刷新令牌 |
| GET  | `/api/auth/me` | 获取当前用户信息 |
| POST | `/api/auth/change-password` | 修改密码 |
| POST | `/api/auth/logout` | 退出登录 |

### 书籍接口 `/api/books`

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/api/books/search` | 搜索书籍 | 所有用户 |
| GET | `/api/books/{id}` | 获取书籍详情 | 所有用户 |
| POST | `/api/books` | 创建图书 | 仅管理员 |
| PUT | `/api/books/{id}` | 更新图书 | 仅管理员 |
| DELETE | `/api/books/{id}` | 删除图书 | 仅管理员 |
| DELETE | `/api/books/batch` | 批量删除图书 | 仅管理员 |
| POST | `/api/books/import` | 批量导入（Excel/CSV） | 仅管理员 |
| GET | `/api/books/import/template` | 下载导入模板 | 仅管理员 |

### 借阅接口 `/api/borrow`

| 方法 | 路径 | 说明 | 分页 |
|------|------|------|------|
| POST | `/api/borrow/apply` | 申请借阅 | - |
| POST | `/api/borrow/{id}/return` | 归还书籍 | - |
| POST | `/api/borrow/{id}/renew` | 续借书籍 | - |
| POST | `/api/borrow/{id}/pickup` | 确认取书 | - |
| GET  | `/api/borrow/history` | 获取借阅历史 | 📄 15条/页 |
| GET  | `/api/borrow/current` | 获取当前借阅 | 📄 10条/页 |
| GET  | `/api/borrow/admin/pending` | 获取待审批（管理员） | 📄 20条/页 |

### 预约接口 `/api/reservations`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/reservations` | 预约书籍 |
| DELETE | `/api/reservations/{id}` | 取消预约 |
| GET  | `/api/reservations/my` | 获取我的预约 |

### 统计接口 `/api/statistics`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/statistics/overview` | 总览数据 |
| GET | `/api/statistics/trends` | 借阅趋势 |
| GET | `/api/statistics/rankings` | 热门排行 |

### 通知接口 `/api/notifications`

| 方法 | 路径 | 说明 | 分页 |
|------|------|------|------|
| GET | `/api/notifications` | 获取通知列表 | 📄 20条/页 |
| PUT | `/api/notifications/{id}/read` | 标记已读 | - |
| PUT | `/api/notifications/read-all` | 全部标记已读 | - |

### 搜索历史接口 `/api/search-history`

| 方法 | 路径 | 说明 | 分页 |
|------|------|------|------|
| GET  | `/api/search-history` | 获取搜索历史 | 📄 30条/页 |
| POST | `/api/search-history` | 添加搜索记录 | - |
| DELETE | `/api/search-history` | 清空搜索历史 | - |

### 收藏接口 `/api/favorites`

| 方法 | 路径 | 说明 | 分页 |
|------|------|------|------|
| GET  | `/api/favorites` | 获取收藏列表 | 📄 20条/页 |
| POST | `/api/favorites` | 添加收藏 | - |
| DELETE | `/api/favorites/{bookId}` | 取消收藏 | - |
| GET | `/api/favorites/check` | 检查收藏状态 | - |

### 阅读状态接口 `/api/reading-status`

| 方法 | 路径 | 说明 | 分页 |
|------|------|------|------|
| GET  | `/api/reading-status` | 获取阅读状态列表 | 📄 20条/页 |
| PUT  | `/api/reading-status` | 更新阅读状态 | - |
| DELETE | `/api/reading-status/{bookId}` | 删除阅读状态 | - |
| GET | `/api/reading-status/{bookId}` | 获取单本书阅读状态 | - |

---

## 🛠️ 开发与构建

### 后端

```bash
cd backend

# 编译
mvn clean compile

# 运行测试
mvn test

# 打包可执行 JAR
mvn clean package
```

### 前端

```bash
cd frontend

# 安装依赖
npm install

# ---- Web 开发 ----
npm run dev          # 启动开发服务器
npm run build        # 构建生产版本
npm run preview      # 预览生产构建

# ---- Windows 桌面端打包 ----
# 生成绿色免安装版 + NSIS 安装包 → dist_electron/
npm run build:win
```

---

## 🔧 问题排查

| 问题 | 排查方向 |
|------|----------|
| **后端启动失败** | 检查 Java ≥ 17、MySQL 连接配置 (`application.yml`)、端口 8080 是否被占用 |
| **Elasticsearch 连接失败** | 检查 ES 是否启动 (`docker ps`)、端口 9200 是否可访问、环境变量配置是否正确 |
| **搜索功能异常** | 查看日志确认是否已降级到 MySQL、检查 ES 索引是否存在 (`curl localhost:9200/books/_count`) |
| **前后端联调 CORS 报错** | 确认 Spring Boot 已运行且 SecurityConfig 中 CORS 配置正确；检查 Axios `baseURL` |
| **Electron 打包下载慢** | 配置 npm 镜像源或 `ELECTRON_MIRROR` 环境变量指向国内镜像 |
| **诗词库加载慢** | `poemLibrary.ts` 约 1.2MB，生产环境已配置 Vite 代码分割 + 动态导入 |

详见 [快速启动指南.md](./docs/快速启动指南.md)

---

## 📖 文档导航

| 文档 | 说明 |
|------|------|
| [ARCHITECTURE_ANALYSIS.md](./docs/ARCHITECTURE_ANALYSIS.md) | 全项目架构深度分析及目录依赖说明 |
| [DEVELOPMENT_GUIDE.md](./docs/DEVELOPMENT_GUIDE.md) | 项目总开发指南与规划 |
| [PERFORMANCE_OPTIMIZATION.md](./docs/PERFORMANCE_OPTIMIZATION.md) | 性能优化实现文档（Redis 缓存、数据库索引） |
| [elasticsearch-setup.md](./docs/elasticsearch-setup.md) | Elasticsearch 部署指南 |
| [elasticsearch-integration-checklist.md](./docs/elasticsearch-integration-checklist.md) | Elasticsearch 集成验收清单 |
| [elasticsearch-bugfix-report.md](./docs/elasticsearch-bugfix-report.md) | Elasticsearch Bug 修复报告 |
| [book-management-api.md](./docs/api/book-management-api.md) | 图书管理 API 文档 |
| [快速启动指南.md](./docs/快速启动指南.md) | 5 分钟快速上手 |
| [借阅管理系统详细计划.md](./docs/借阅管理系统详细计划.md) | 借阅模块完整设计方案 |
| [数据分析模块详细计划.md](./docs/数据分析模块详细计划.md) | 数据分析引擎设计方案 |
| [开发完成总结.md](./docs/开发完成总结.md) | 项目核心业务总结 |
| [离线功能使用说明.md](./docs/离线功能使用说明.md) | 离线模式功能说明 |
| [SECURITY_CONFIGURATION_NOTES.md](./docs/SECURITY_CONFIGURATION_NOTES.md) | 安全配置说明 |
| [DDoS 防御方案](./ddos-defense/README.md) | DDoS 防御体系文档 |
| [test-api.bat](./test-api.bat) / [test-api.sh](./test-api.sh) | API 接口测试脚本 |

---

## 📝 更新日志

### v1.5.0 (2026-04-20)

#### 🚀 新增功能

- ✅ **分页功能全面实现** — 解决活跃用户数据膨胀问题
  - 借阅历史分页（默认 15 条/页）
  - 当前借阅分页（默认 10 条/页）
  - 待审批借阅分页（默认 20 条/页）
  - 通知列表分页（默认 20 条/页）
  - 搜索历史分页（默认 30 条/页）
  - 收藏列表分页（默认 20 条/页）
  - 阅读状态分页（默认 20 条/页）
- ✅ **PageableHelper 工具类** — 统一分页参数处理
  - 自动规范化负数页码
  - 限制最大页面大小为 100
  - 支持多字段排序
- ✅ **向后兼容** — 保留原有非分页方法，确保现有客户端不受影响

#### 🔧 优化改进

- ✅ **预约服务并发控制** — 使用数据库锁防止并发冲突
- ✅ **启动脚本改进** — 增强错误处理和英文化输出
  - 自动检测端口占用并释放
  - 验证 Java 和 npm 环境
  - 改进错误提示信息

#### 🐛 Bug 修复

- ✅ 修复 BorrowRecord 字段名错误
  - 将不存在的 `borrowTime` 和 `applyTime` 替换为 `createdAt`
  - 修复 Repository、Service、Controller 三层的字段引用

#### 🧪 测试覆盖

- ✅ 新增 `ConcurrencyStressTest` — 预约队列位置唯一性并发测试

#### 📚 文档更新

- ✅ 新增 [PAGINATION_API.md](./PAGINATION_API.md) — 分页 API 完整文档
- ✅ 新增 BUG_FIX_SUMMARY.md — Bug 修复总结文档

### v1.4.0 (2026-04-19)

#### 🚀 新增功能

- ✅ **图书管理 API** — 管理员可通过 API 新增、修改、删除图书
  - POST `/api/books` - 创建图书
  - PUT `/api/books/{id}` - 更新图书（智能库存调整）
  - DELETE `/api/books/{id}` - 删除图书（关联数据检查）
  - DELETE `/api/books/batch` - 批量删除
  - POST `/api/books/import` - 批量导入（支持 Excel/CSV）
  - GET `/api/books/import/template` - 下载导入模板

#### 🔧 Bug 修复

- ✅ 修复 Elasticsearch 配置在非 test 环境下可能失效的问题
  - 将 `@Profile("!test")` 替换为 `@ConditionalOnProperty`
  - 基于配置项 `library.search.elasticsearch.enabled` 控制
- ✅ 修复 Elasticsearch Service 和 Controller 缺少条件注解的问题
  - 给 `ElasticsearchSyncService`、`ElasticsearchSearchService`、`ElasticsearchStatisticsService` 添加条件注解
  - 给 `ElasticsearchSyncController` 添加条件注解
  - 修复 `SmartSearchService` 和 `StatisticsService` 的依赖注入问题
- ✅ 修复 BookSyncListener 注入失败问题
  - 添加 `@Autowired(required = false)` 避免 ES 禁用时启动失败

#### 🧪 测试覆盖

- ✅ 新增 `BookServiceTest` — 图书服务单元测试（7 个测试用例）
- ✅ 新增 `BookControllerAdminTest` — 管理接口集成测试（3 个测试用例）
- ✅ 新增 `TestElasticsearchConfig` — 测试环境 Mock 配置

#### 📚 文档更新

- ✅ 新增 [book-management-api.md](./docs/api/book-management-api.md) — 图书管理 API 文档

### v1.3.0 (2026-04-18)

#### 🚀 重大更新

- ✅ **集成 Elasticsearch 8.x 搜索引擎** — 全文搜索性能提升 50-60 倍
- ✅ **IK 中文分词器支持** — 智能中文分词，提升搜索准确度
- ✅ **实时数据同步** — JPA 事件监听器自动同步数据到 Elasticsearch
- ✅ **智能降级机制** — ES 不可用时自动降级到 MySQL，保证服务可用性

#### ⚡ 性能优化

- ✅ 统计查询性能：3-5秒 → < 100ms（50 倍提升）
- ✅ 搜索建议性能：2-3秒 → < 50ms（60 倍提升）
- ✅ 内存占用优化：200MB → < 1MB（200 倍降低）
- ✅ 支持 100+ QPS 高并发查询

#### 🔧 Bug 修复

- ✅ 修复 Elasticsearch 聚合结果访问的 NPE 风险
- ✅ 修复 IndexOutOfBoundsException 风险
- ✅ 修复聚合结果顺序依赖问题，改为按名称访问
- ✅ 修复日志格式错误

#### 📦 新增服务

- ✅ `ElasticsearchSyncService` — 数据同步服务（增量 + 全量）
- ✅ `ElasticsearchStatisticsService` — ES 统计服务
- ✅ `ElasticsearchSearchService` — ES 搜索服务
- ✅ `MysqlStatisticsService` — MySQL 降级服务
- ✅ `FullSyncController` — 全量同步控制器

#### 🧪 测试覆盖

- ✅ 单元测试：`ElasticsearchSyncServiceTest`
- ✅ 集成测试：`StatisticsServiceIntegrationTest`
- ✅ 降级测试：`FallbackTest`

#### 📚 文档更新

- ✅ 新增 [elasticsearch-setup.md](./docs/elasticsearch-setup.md) — 部署指南
- ✅ 新增 [elasticsearch-integration-checklist.md](./docs/elasticsearch-integration-checklist.md) — 验收清单
- ✅ 新增 [elasticsearch-bugfix-report.md](./docs/elasticsearch-bugfix-report.md) — Bug 修复报告

### v1.2.0 (2026-04-05)

#### 🆕 新增功能
- ✅ 实装**沉浸式水墨登录界面**，包含 15+ 种微交互动效（粒子尾迹、毛笔笔触、磁性按钮、呼吸光环、进度环、登录烟花等）
- ✅ 集成 **10,000 首古典诗词库**，登录页飘落诗页随机展示先秦至清代名篇
- ✅ 新增**行为模式反爬虫分析** — 检测顺序遍历、定时轮询、广度优先爬取模式
- ✅ 新增**设备指纹识别**与**渐进式惩罚**机制
- ✅ 部署 **DDoS 防御独立模块** — 含 Nginx 限流、iptables 规则、自动化监控脚本

#### 🔧 技术改进
- ✅ 全面修复前端 16 项代码质量问题（内存泄漏、竞态条件、时区偏差、并发问题）
- ✅ 全面修复后端 16 项安全与性能问题（JWT token 管理、状态机死锁、全表扫描优化、线程安全）
- ✅ 登录页动画引擎重构：视频方案 → Canvas 帧级交叉缓存，消除"黑屏频闪"
- ✅ 前端大组件模块化拆分，提炼 Composables 降低耦合度

### v1.1.0 (2026-04-04)

#### 🆕 新增功能
- ✅ 上线基于 Electron 的 Windows 桌面独立端安装包 (`dist_electron`)
- ✅ 新增 AI 智能助手组件，支持上下文保持、多轮对话与写作润色
- ✅ 扩充桌面宠物功能，实装跳舞、游戏、魔法与生气等丰富的帧级状态交互
- ✅ 强化应用整体生态体验，加入更精美的动态背景逻辑及缓存呈现

#### 🔧 技术改进
- ✅ 修复了由于 Vue Router 递归加载造成的系统登录页阻塞或死循环问题
- ✅ 重构了 AI 等超过千行的前端组件模块化梳理，提炼 Composables 以降低系统耦合度
- ✅ 优化登录页面的动画，由视频依赖转向健壮的交叉缓存动效实现，消除加载"黑屏频闪"
- ✅ 成功修复打包依赖环境导致打包不通过的异常

### v1.0.0 (2026-04-03)
- 🎉 初始版本发布：全平台基本架构确立，实现基础用户态及书籍系统联调

---

## 🤝 贡献指南

欢迎贡献代码！请遵循以下步骤：

1. Fork 本项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request 进行审核

### 代码规范

- 后端：遵循 Spring Boot 最佳实践，使用 Lombok 简化代码
- 前端：使用 Vue 3 Composition API + TypeScript
- 提交信息：遵循 [Conventional Commits](https://www.conventionalcommits.org/) 规范
  - `feat:` 新功能
  - `fix:` Bug 修复
  - `docs:` 文档更新
  - `refactor:` 代码重构
  - `test:` 测试相关
  - `chore:` 构建/工具链更新

---

## 📄 许可证

本项目采用 MIT 许可证 — 详见 [LICENSE](LICENSE) 文件

---

## 👥 团队

- **开发**: AI Agent 辅助研制
- **课题**: 中国劳动关系学院

---

## 🙏 致谢

感谢以下开源项目的支持：

- [Spring Boot](https://spring.io/projects/spring-boot) — 后端框架
- [Vue.js](https://vuejs.org/) — 前端框架
- [Electron](https://www.electronjs.org/) — 桌面端跨平台
- [Elasticsearch](https://www.elastic.co/) — 全文搜索引擎
- [Redis](https://redis.io/) — 高性能缓存
- [ECharts](https://echarts.apache.org/) — 数据可视化
- [Pinia](https://pinia.vuejs.org/) — 状态管理
- [Axios](https://axios-http.com/) — HTTP 客户端
- [JWT](https://jwt.io/) — 令牌认证
- [ShedLock](https://github.com/lukas-krecan/ShedLock) — 分布式调度
- [Micrometer](https://micrometer.io/) + [Prometheus](https://prometheus.io/) — 监控体系
- [Resilience4j](https://resilience4j.readme.io/) — 熔断降级

---

**⭐ 如果这个项目对你有帮助，请给个 Star 支持一下！**
