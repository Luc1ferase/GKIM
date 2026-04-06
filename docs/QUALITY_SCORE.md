# Quality Score - 必须达到 95+ 分才能 Merge

## 代码质量标准
- TypeScript：strict 模式，无 any
- ESLint + Prettier + @unocss/eslint-config 强制通过
- 组件必须有 <script setup lang="ts"> + defineProps 完整类型
- 所有 AIGC 调用必须有错误兜底 + loading 状态
- 实时消息必须有本地缓存（Pinia + uni.setStorage）
- 性能：列表使用 v-memo / uni-list，AIGC 请求限流

## 测试要求
- 每个 composable 必须有 Vitest 测试
- 关键流程（发消息、调用文生图、创意工坊保存）必须有端到端测试
- 所有页面必须支持 uni-app 预览

## 文档要求
- 每次变更必须同步 openspec/spec
- 复杂组件必须有 <!-- DESIGN: --> 注释指向 DESIGN_SYSTEM.md