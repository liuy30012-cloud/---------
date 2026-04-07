# 测试、安全与认证架构说明

## 1. 后端测试入口

- 快速套件：`cd backend && .\mvnw test`
  - 默认覆盖控制器、服务、JPA 集成与安全回归测试。
  - 不包含依赖 Docker/Testcontainers 的并发数据库压力测试。
- 并发数据库套件：`cd backend && .\mvnw -Pconcurrency-db-tests test`
  - 只运行 `concurrency-db` 标记的测试。
  - 当前通过 `@Testcontainers(disabledWithoutDocker = true)` 和前置 `Assumptions` 保证在无 Docker 环境下干净跳过，不会挂死或晚失败。

## 2. 安全过滤链收敛

- `SecurityConfig` 只保留认证、验证码、健康检查和明确允许公开读取的图书接口。
- 敏感路径如 `/api/admin/**`、`/api/internal/**`、`/api/v2/**`、`/graphql` 与 `/api/swagger.json` 已回收到拒绝访问或认证保护。
- 代理信任来源改成配置驱动：
  - 默认仅信任 loopback。
  - 额外私网代理必须显式通过 `security.trusted-proxies` 配置。
- 限流与封禁链路分层：
  - 首次或普通超限返回现有 `429` JSON 结构，并要求验证码。
  - 重复 burst/global 违规才进入封禁。
- 安全状态存储默认分环境切换：
  - 生产优先 Redis，保证多节点共享 rate counter、ban、nonce。
  - 本地和测试继续使用内存实现，便于隔离和重置。

## 3. 认证持久化流转

- `UserService` 现在是外观层，内部拆成三块职责：
  - `UserAccountService`：用户持久化、资料查询、密码更新。
  - `UserAuthenticationService`：注册、登录、刷新令牌、登出等认证流程。
  - `UserLoginSecurityService`：登录失败计数、锁定和登录日志。
- 用户与登录日志改为 JPA 持久化：
  - `User`
  - `LoginLog`
  - `UserRepository`
  - `LoginLogRepository`
- Demo 账号不再写死在 service 构造函数中，而是在启动期由 `DemoUserInitializer` 初始化。

## 4. 前端登录页收敛

- `Login.vue` 变成薄容器，只负责：
  - 组合 `useLoginForm`、`useLoginParticles`、`useInkCanvas`
  - 连接页面级鼠标事件
  - 将状态下发给视觉层与表单卡片组件
- `LoginVisualLayer.vue` 承载背景场景、书页粒子、环境装饰。
- `LoginAuthCard.vue` 承载品牌区、登录注册表单和卡片交互。
- `poemLibrary` 改成动态导入，登录页首屏不再同步吃下整份诗词数据。
