# UniAIGC-IM Architecture (Harness Engineering)

## 核心原则（所有 Agent 必须 100% 遵守）
- 单代码库跨平台：使用 UniAPP (Vue3 + TypeScript + Setup Script) 同时支持 iOS 和 Android
- 严格分层架构（禁止反向依赖）：
  - pages/          → 只负责页面布局和路由
  - components/     → 可复用 UI 组件（必须使用 DESIGN_SYSTEM）
  - composables/    → 业务逻辑钩子（AIGC、IM、创意工坊等）
  - stores/         → Pinia 全局状态（chatStore、userStore、aigcStore）
  - api/            → 所有网络请求（IM WebSocket + AIGC 第三方 API）
  - utils/          → 纯工具函数（MD/MDX 解析、日期格式等）
- 禁止在 pages/ 或 components/ 中直接写 API 调用或复杂逻辑
- 所有 AIGC 相关功能必须走 composables/useAIGC.ts 并支持多提供商切换
- 实时消息使用 WebSocket（或 UniAPP 兼容的 IM SDK），必须实现本地 Room + Pinia 缓存
- 条件编译：平台差异仅使用 #ifdef APP-PLUS / #ifdef APP-PLUS-NVUE 等 UniAPP 原生方式

## 技术栈强制（不可变更）
- 框架：UniAPP x（Vue3 + TS + Vite）
- 状态管理：Pinia + Pinia Persistedstate
- 样式：UnoCSS（原子化）+ 自定义 Design Tokens
- MD/MDX 解析：markdown-it + 自定义 CSS 渲染器（支持开发者空间）
- AIGC：支持腾讯混元、阿里通义 + 自定义 OpenAI-compatible 接口
- IM 实时：WebSocket + UniCloud（或自建后端），支持离线消息
- 测试：Vitest + @vue/test-utils（至少覆盖 composables 和 AIGC 核心）

## 目录结构（必须严格遵守）

src/
├── pages/              # 首页(消息)、联系人、空间、聊天室、设置等
├── components/         # 全局组件（ChatBubble、AIGCButton 等）
├── composables/        # useChat、useAIGC、useCreativeWorkshop
├── stores/
├── api/
├── styles/
├── utils/
└── types/