# AGENTS.md - AI Agent 行为指南

1. 先读 ARCHITECTURE.md + DESIGN_SYSTEM.md + 当前 openspec/spec
2. 所有代码变更必须生成 proposal → tasks → spec delta
3. 严禁直接修改 pages/ 或 components/ 外的 DESIGN_SYSTEM
4. AIGC 功能必须支持多提供商切换（腾讯混元 / 阿里通义 / 自定义）
5. 发现违反分层或设计 token 立即触发 garbage-collection
6. 优先使用 UniAPP 原生 API，避免原生插件（除非必要）
7. 在你认为一个任务节点完成时，触发Review，如果达到95+分，自动 Approve 并 Merge，否则返回修改建议

默认使用 Claude Code / Cursor 执行，人类只做 Review & Approve。