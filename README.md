# GKIM — AIGC-First 即时通信应用

全栈移动端 IM 应用，包含 Android 原生客户端、Rust 实时后端和 AIGC 创作能力，全程由 AI 编码代理驱动开发。

## 技术栈

| 层级 | 技术 | 定位 |
|---|---|---|
| **Android 客户端** | Kotlin · Jetpack Compose · Material 3 · Navigation Compose · Room · DataStore · EncryptedSharedPreferences · Retrofit 2 · OkHttp 4 · WebSocket | 原生移动端 |
| **Rust 后端** | Rust · Axum · Tokio · PostgreSQL (SQLx) · Argon2 · Docker | HTTP + WebSocket 即时通信服务 |

客户端选择 Android 原生以保证 IM 场景对交互流畅度、状态管理和实时连接的要求；后端选择 Rust 以适配高并发、长连接和状态一致性的需求。

## 核心功能

- **账号体系** — 注册 / 登录，Argon2 密码哈希，Token 会话持久化与启动恢复
- **社交关系** — 用户搜索、好友请求发送 / 接受 / 拒绝，仅互为联系人后才能建立消息链路
- **实时消息** — WebSocket 网关推送，送达回执、已读回执、好友请求事件通知
- **离线恢复** — 消息历史拉取、未读计数重建、会话状态恢复
- **AIGC 创作** — 多提供商适配架构（腾讯混元 / 阿里通义 / 自定义 OpenAI 兼容端点），支持 text-to-image、image-to-image、video-to-video 三种模式
- **创作工坊** — Prompt 模板库，支持分类筛选和一键注入聊天生成流程

## AI 与本项目

### 文生视频

应用欢迎页的全屏视频动画由**豆包 Seed 2.0 Pro** 文生视频能力生成，作为开屏背景循环播放。这是项目中文生视频技术的直接落地应用。

在 AIGC 架构层面，框架已通过 Provider Adapter 注册表模式预留了 video-to-video 生成入口，聊天界面的 `+` 菜单包含视频生成操作入口，后续可接入更多视频生成服务。

### Vibe Coding

本项目全程使用 **Claude Code** 和 **Cursor** 作为主力编码代理。人类开发者负责方向决策、Review 和 Approve，AI agent 承担代码实现、测试编写和文档生成。

具体工作流：
- AI agent 读取架构文档和设计规范后自主编码
- 每个任务完成后必须通过人类 Review，质量分数达到 95+ 才可提交
- 所有代码变更必须经过 `proposal → tasks → spec delta` 流程

详见 [`docs/AGENTS.md`](docs/AGENTS.md)。

### OpenSpec 规格驱动开发

项目引入自定义的 **OpenSpec** 规格驱动交付框架，将需求、实现、验证和归档串成闭环：

```
proposal → design → tasks → 实现 → 验证 → 打分 → 证据记录 → push → archive
```

- 所有变更从 `openspec/changes/` 中的提案开始，每个变更包含 proposal、design、tasks 和可选的 spec delta
- 每个实现任务必须经过验证、Review、质量打分（最低 95 分）和证据记录后才能标记完成
- 已归档 17 个变更，4 个变更进行中
- 主规范位于 `openspec/specs/core/im-app/spec.md`

详见 [`openspec/config.yaml`](openspec/config.yaml) 和 [`docs/DELIVERY_WORKFLOW.md`](docs/DELIVERY_WORKFLOW.md)。

## 项目结构

```
GKIM/
├── android/          # Android 原生客户端（主交付目标）
│   └── app/src/main/java/com/gkim/im/android/
│       ├── core/     #   设计系统、模型、渲染、安全
│       ├── data/     #   Room 本地库、Retrofit 远程、Repository
│       └── feature/  #   Welcome、Login、Chat、Contacts、Space、Settings 等
├── backend/          # Rust IM 后端
│   ├── src/          #   Axum 路由、Auth、Social、IM Service、WebSocket Hub
│   ├── migrations/   #   PostgreSQL Schema 迁移
│   └── tests/        #   集成测试
├── openspec/         # 规格驱动交付框架
│   ├── changes/      #   活跃 + 归档变更
│   └── specs/        #   核心规范、质量门禁、后端规范
├── docs/             # 架构、设计系统、交付工作流、Agent 指南
└── infra/            # 部署和数据库文档
```

## 快速开始

**Rust 后端**：详见 [`backend/README.md`](backend/README.md)

**Android 客户端**：详见 [`android/README.md`](android/README.md)

## GitHub Android Release

仓库现在预留了 GitHub tag 驱动的 Android APK 发布链路，约定如下：

- 发布 tag 格式：`vMAJOR.MINOR.PATCH`
- GitHub Release APK 资产名：`gkim-android-vMAJOR.MINOR.PATCH.apk`
- 触发入口：向 GitHub 推送符合格式的版本 tag

仓库需要先在 GitHub Secrets 中配置以下 Android release signing 输入：

- `ANDROID_RELEASE_KEYSTORE_BASE64`
- `ANDROID_RELEASE_STORE_PASSWORD`
- `ANDROID_RELEASE_KEY_ALIAS`
- `ANDROID_RELEASE_KEY_PASSWORD`

CI 构建会把 tag 版本注入 Android Gradle 的以下输入，而不是要求维护者手工改版本号：

- `GKIM_RELEASE_VERSION_NAME`
- `GKIM_RELEASE_VERSION_CODE`
- `GKIM_RELEASE_STORE_FILE`
- `GKIM_RELEASE_STORE_PASSWORD`
- `GKIM_RELEASE_KEY_ALIAS`
- `GKIM_RELEASE_KEY_PASSWORD`

详细的 Android 构建和本地验证说明见 [`android/README.md`](android/README.md)。

## 测试

| 层级 | 工具 | 覆盖范围 |
|---|---|---|
| Rust 后端 | cargo test | HTTP API、WebSocket 网关、Schema 迁移集成测试 |
| Android 单元测试 | JUnit + Coroutines Test | Repository、ViewModel、IM Client、Markdown 解析 |
| Android 仪器测试 | Espresso + Compose UI Test | 导航、欢迎视频布局、IM 后端端到端验证 |
