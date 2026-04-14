## Context

GKIM 当前已经有可构建的 Android 原生客户端，但仓库中还不存在任何 `.github/workflows` 自动化流程。`android/app/build.gradle.kts` 目前固定使用 `versionCode = 1`、`versionName = "0.1.0"`，并且还没有 release signing 配置，因此本地虽然可以手工构建 APK，GitHub 侧却没有一个可追溯、可复现的打 tag 发布路径。

这个改动会横跨 GitHub Actions、Android Gradle 配置和运维文档三部分，因此适合先把交付链路设计清楚，再进入实现。对这个仓库来说，目标不是直接接 Play Store，而是先让维护者在 GitHub 上打版本 tag 后，稳定得到一个可下载的 Android 版本 APK。

## Goals / Non-Goals

**Goals:**
- 在推送受支持的版本 tag 后，自动触发 GitHub Actions 构建 Android release APK。
- 让生成的 APK 文件名、GitHub Release 资产名和 tag 版本保持一致，方便回溯和下载。
- 通过 GitHub secrets 管理 release keystore 和密码，不把签名材料提交进仓库。
- 在发布成功后同时保留 workflow artifact 和 GitHub Release asset 两个下载入口。
- 补充仓库文档，说明 tag 规则、所需 secrets 和运维使用方式。

**Non-Goals:**
- 本次不接入 Google Play 发布、AAB 上传或 Fastlane。
- 本次不处理 iOS、H5 或后端镜像发布。
- 本次不改动应用运行时功能，只覆盖 Android 构建与发布自动化。
- 本次不引入复杂的多渠道、多风味（flavor）版本矩阵。

## Decisions

### 1. Use a dedicated GitHub Actions workflow triggered by `push` tags matching `v*`
采用单独的 GitHub Actions workflow，例如 `.github/workflows/android-tag-release.yml`，只在推送 `v*` 版本 tag 时运行。

Why this decision:
- 与用户“打 tag 后自动编译 APK”的心智模型完全一致。
- 不会把普通分支 push、PR 验证和正式发布混在一起。
- 后续如果要增加 `workflow_dispatch` 手动重跑，也能在同一个 workflow 上扩展。

Alternatives considered:
- 在现有通用 CI workflow 中追加 tag 分支：仓库目前没有现成 GitHub workflow，直接新建独立 release workflow 更清晰。
- 用 GitHub Release published 事件触发：比直接监听 tag 多一步人工创建 release，不符合当前诉求。

### 2. Derive release metadata from the Git tag instead of hardcoding version fields
工作流会解析 `vMAJOR.MINOR.PATCH` 形式的 tag，并把解析后的版本信息传给 Gradle。`versionName` 使用 tag 去掉前缀后的值，例如 `1.4.3`；`versionCode` 采用数值化规则，例如 `MAJOR * 1000000 + MINOR * 1000 + PATCH`。

Why this decision:
- 让 APK 元数据与 tag 对齐，避免每次发布前手工改 `build.gradle.kts`。
- 单个仓库提交可以在不同 tag 下生成不同版本产物，更符合 release automation 预期。
- 版本规则明确后，CI 可以在 tag 非法时快速失败。

Alternatives considered:
- 保持 Gradle 中的固定版本号，仅重命名输出文件：文件能对上 tag，但 APK 内部版本信息不一致，不利于安装与排查。
- 直接使用 GitHub run number 作为 versionCode：简单，但同一个 tag 重跑时含义不直观，也不利于人工回溯。

### 3. Reconstruct Android signing inputs from GitHub secrets at runtime
release keystore 通过 base64 secrets 存储，workflow 运行时恢复到临时文件，并把 alias、store password、key password 以环境变量或 Gradle properties 的方式传入 Android 构建。

Why this decision:
- 满足“自动发布 APK”同时避免提交 keystore 或密码。
- 与 GitHub 仓库级 secret 管理兼容，维护成本低。
- 便于在 CI 中对缺失配置进行显式失败，而不是悄悄产出不可分发的 unsigned APK。

Alternatives considered:
- 把 keystore 放进仓库加密目录：增加泄漏风险，也不符合仓库安全边界。
- 允许缺 secret 时发布 unsigned APK：容易把“构建成功”和“可发布版本”混淆，不适合作为正式 tag 发布默认行为。

### 4. Publish the APK to both workflow artifacts and the GitHub Release bound to the tag
成功构建后，workflow 会先上传 APK 为 Actions artifact，再把同一文件附加到 tag 对应的 GitHub Release。

Why this decision:
- workflow artifact 便于 CI 排障和内部快速下载。
- GitHub Release 资产是更稳定的外部下载入口，适合测试和版本留档。
- 两个入口一起保留，可以避免单一发布面导致的排查不便。

Alternatives considered:
- 只上传 artifact：不利于版本化留档，下载入口分散在 workflow run 页面。
- 只上传 GitHub Release：一旦发布前后排查需要中间产物，workflow 页面信息不够完整。

### 5. Keep the first release workflow focused on deterministic build + lightweight verification
在 tag workflow 中先执行适合 GitHub-hosted runner 的轻量验证，例如 `:app:testDebugUnitTest`，随后执行 `:app:assembleRelease`，不把依赖模拟器的仪器测试纳入首版发布链路。

Why this decision:
- 用户当前需求核心是“打 tag 自动产出 APK”，首版链路应优先保证稳定可运行。
- GitHub-hosted runner 上启用 Android 模拟器成本高、耗时长、波动大。
- 单元测试加 release 组装足以覆盖大部分构建与基础逻辑回归，后续可再扩充更重的发布门禁。

Alternatives considered:
- 发布前完全不做验证：构建虽然更快，但更容易把明显损坏的代码打包进 tag 产物。
- 强制接入完整 instrumentation：交付成本高，且超出这次“先建立 tag 发布链路”的目标。

## Risks / Trade-offs

- [Tag version parsing is too strict] → 明确文档只支持 `vMAJOR.MINOR.PATCH`，并让 workflow 在 tag 不合法时给出清晰错误。
- [Signing secrets are misconfigured] → 在真正 assemble/publish 前增加 secrets 完整性检查，尽早失败并打印缺失项名称。
- [GitHub runner Android environment drifts] → 固定 JDK 17、compile SDK 34 和所需 build-tools 版本，避免依赖 runner 默认环境。
- [Release asset naming and APK metadata diverge] → 统一由同一段 tag 解析逻辑同时驱动 Gradle 参数和最终输出文件名。
- [Workflow only covers APK, not full store distribution] → 在设计和文档中明确这是 GitHub release automation 的第一阶段，而不是完整移动发布平台。

## Migration Plan

1. 在 Android Gradle 层新增可由 CI 注入的 `versionName`、`versionCode` 和 signing 配置入口。
2. 新增 GitHub Actions release workflow，完成 checkout、JDK/Android SDK 初始化、Gradle 缓存、轻量验证、assembleRelease 和产物上传。
3. 补充 GitHub Release 发布步骤，把 APK 绑定到对应 tag。
4. 在 README 或 Android 文档中记录 tag 规范与 repository secrets 配置方法。
5. 用一个测试 tag 完成端到端验证，确认 GitHub 上能看到 workflow artifact 与 release asset。

Rollback strategy:
- 如果 release workflow 导致错误发布，可先删除该 workflow 文件并停止继续打 release tag。
- 如果是 signing 或版本注入配置出错，可保留本地手工构建路径，同时修复 CI 配置后再重新打 tag 或重跑 workflow。

## Open Questions

- 未来如果仓库需要同时发布 Android 与其他平台，是否要把 tag 规则从 `v*` 细分为 `android/v*` 这样的命名空间？本次先按单平台 Android 发布处理。
