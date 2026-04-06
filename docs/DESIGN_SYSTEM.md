# Design System - UniAIGC-IM（Aether Mono 主风格）

## 权威来源（必须 100% 遵守）
所有 UI 实现**必须严格遵循**以下 Stitch 设计文件（相对路径）：

**核心设计系统（最高优先级）：**
- `docs/stitch-design/aether_mono/DESIGN.md` ← **最终权威**（Cyber-Minimalist Mono）

**补充参考（视觉 + 代码灵感）：**
- `docs/stitch-design/cyber_minimalist_mono/DESIGN.md`
- `docs/stitch-design/indigo_architect/DESIGN.md`
- 每个功能页面的 **screen.png** 和 **code.html**：
  - messages_tab/（聊天列表）
  - contacts_tab/（联系人）
  - aigc_chat/ + aigc_chat_light_mode/（聊天界面）
  - aigc_workshop/（创意工坊）
  - ai_settings/ + foldable_ai_settings/（设置页）
  - discovery_feed/（空间 Feed）

生成任何页面或组件前，**必须先阅读**对应文件夹的 screen.png（视觉对齐）和 code.html（UnoCSS 灵感）。

## UniAPP + UnoCSS 实现映射

### 1. 颜色 Token（来自 aether_mono/DESIGN.md）
在 `unocss.config.ts` 中必须定义：

```ts
theme: {
  colors: {
    surface: '#091328',                    // Dark 主背景
    'surface-light': '#F8FAFC',
    'surface-container-low': '#1a2338',
    'surface-container-high': '#2a3550',
    primary: '#C3C0FF',                    // Indigo 主色
    'primary-container': '#4F46E5',
    'on-surface': '#ffffff',
    'outline-variant': '#464555',
  }
}
```

### 2. 排版 & 核心视觉规则（直接摘自 aether_mono/DESIGN.md）

字体：Space Grotesk（标题） + Inter（正文）
No-Line Rule：禁止任何 1px solid border，仅用 surface 颜色层次分隔
Glassmorphism：浮动元素使用 backdrop-blur-2xl bg-surface/60
Primary CTA：135° 渐变 from-primary to-primary-container
底部导航：固定底部三栏（消息 / 联系人 / 空间），选中态使用 primary 色 + 轻微 scale

### 3. 页面级参考规则

消息列表 → 严格参考 docs/stitch-design/messages_tab/screen.png + code.html
聊天界面 → 参考 docs/stitch-design/aigc_chat/screen.png（支持 MD 气泡 + AIGC 按钮）
联系人 → 参考 docs/stitch-design/contacts_tab/screen.png
创意工坊 → 参考 docs/stitch-design/aigc_workshop/screen.png（Foldable 手风琴）
设置页 → 参考 docs/stitch-design/ai_settings/screen.png
空间 Feed → 参考 docs/stitch-design/discovery_feed/screen.png

所有组件必须保持 “Architectural Glitch” 风格：大量负空间、不对称布局、300ms ease-out 动画、极客级简洁。
任何新组件生成前，必须先阅读 aether_mono/DESIGN.md + 对应页面的 screen.png / code.html。
text---

**现在操作流程**：
1. 把整个 stitch 文件夹复制到 `docs/stitch-design/`（2 分钟）
2. 把上面**全新**的 DESIGN_SYSTEM.md 覆盖保存
3. 在 Claude Code / Cursor 里直接执行：
/openspec:proposal
根据 docs/DESIGN_SYSTEM.md（已更新为完整 Stitch 结构）和 docs/stitch-design/aether_mono/DESIGN.md，
初始化 UniAPP 项目脚手架（Vue3 + TS + Pinia + UnoCSS），
包含底部导航三栏（消息/联系人/空间），并确保所有样式严格对齐 aether_mono 设计 + 各页面 screen.png