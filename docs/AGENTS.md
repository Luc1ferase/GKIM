# AGENTS.md - AI Agent 行为指南

1. 先读 ARCHITECTURE.md + DESIGN_SYSTEM.md + 当前 openspec/spec
2. 所有代码变更必须生成 proposal → tasks → spec delta
3. 严禁直接修改 pages/ 或 components/ 外的 DESIGN_SYSTEM
4. AIGC 功能必须支持多提供商切换（腾讯混元 / 阿里通义 / 自定义）
5. 发现违反分层或设计 token 立即触发 garbage-collection
6. 优先使用 UniAPP 原生 API，避免原生插件（除非必要）
7. 每个实现任务或子任务完成后，必须按 `docs/DELIVERY_WORKFLOW.md` 先执行验证、Review、质量打分和证据记录；未记录证据前不得把任务标记为完成
8. Review 分数低于 95 分时，任务保持未完成并返回修改建议；达到 95+ 后，必须先 commit 并 push 到当前工作分支，push 成功后才能开始下一个任务
9. Review 通过只代表当前任务可被接受并上传，不等同于自动 merge 到默认分支

默认使用 Claude Code / Cursor 执行，人类只做 Review & Approve。
