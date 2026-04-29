# 角色皮肤 — 占位图生成说明

> 本文为 [`placeholder-generation.md`](./placeholder-generation.md) 的中文对照版。两份文档内容等价，便于不同语言习惯的设计师协作。提示词正文保留英文 —— 图像生成模型（Midjourney / DALL·E / SDXL）对英文 prompt 的响应一致性最高，强制翻译会降低出图稳定性。

这是一次性参考文档：为应用当前已植入的 8 位陪伴角色生成 **default 默认皮肤的占位图**。生成完毕后保存到下方列出的路径，运行 `tools/skins/upload.ps1`（R1.2）即可推送到 R2。

本文覆盖 `companion-skin-gacha` 计划中的 **R1.3**。后续真稿可作为 `v2` 单独发布 —— 按 `design.md` 的版本契约，带版本号的 key 是不可变的：旧的 `v1` 占位图对仍持有旧 catalog 的客户端永远可达。

## 生成流程

对下面 8 位角色，逐一执行以下步骤：

1. 用「公共基底 prompt + 角色专属 prompt」生成 **banner 主图**，分辨率 **941 × 1672**（垂直 9:16）。
2. 在同一主图基础上裁出 4 个不同尺寸（主图比其余三个变体都大，所以小尺寸全是从主图直接裁切）：

| 变体 | 分辨率 | 裁切策略 |
|---|---|---|
| `banner`   | 941 × 1672 | 主图本身，头部位于上 1/3，下边缘渐隐到 `#1A0F0A` |
| `portrait` | 512 × 768  | 头部 + 上半身居中，从主图顶部约 30% 处裁出 512 宽 × 768 高的切片 |
| `avatar`   | 256 × 256  | 头部 + 肩部，从主图上半部分正方形裁切 |
| `thumb`    | 96 × 96    | 仅面部，正方形裁切，简化细节（96 × 96 直接从 941 × 1672 缩图会糊面，建议低细节重绘）|

3. 每个变体编码为 **PNG**，质量约 88（按下方体积预算微调）。
4. 严格保存到列出的 `ops/skins-staging/{characterId}/default/v1/{variant}.png` 路径 —— 上传脚本只认这个路径形状。

### 各变体体积预算（保持 catalog 轻量）

| 变体 | 目标 | 硬上限 |
|---|---|---|
| `thumb`    | ~12 KB  | 30 KB |
| `avatar`   | ~30 KB  | 80 KB |
| `portrait` | ~120 KB | 280 KB |
| `banner`   | ~280 KB | 600 KB |

如果突破上限，先降 PNG 质量再缩尺 —— 这样掉的是「锐度」，比直接降分辨率更耐看（不会变成色块）。

## 公共基底 prompt（每位角色都要带上 / 与角色 prompt 拼接）

```
A half-body portrait of a fictional companion seated by a weathered
oak counter inside a small low-lit Victorian tavern, lit only by a
single brass candle to the right, deep shadow falling left. Style:
painterly digital illustration with subtle ink-engraving line work,
late-Victorian western fantasy tavern aesthetic with subtle
alchemical undertones, hand-painted illustration feel, no East-Asian
costume cues unless the character explicitly calls for it.
Hand-painted texture, muted color grading. Color palette anchored
to brass #E0A04D, ember red #B85450, espresso #1A0F0A background,
aged-paper #F1E7D2 highlight. Vertical portrait 9:16 (941 × 1672),
head and upper torso framed in upper third, lower edge softly
fading into espresso black. Mood: quiet, contemplative, slightly
melancholic, warm candle glow on the cheek and shoulder. No metal
drinking vessels — only glass, ceramic, and wood. No text, no logos,
no watermark, no UI elements.
```

## 公共反向 prompt（每张图都要带上）

```
pewter tankard, metal mug, tin cup, brass cup, iron flagon, metal
drinking vessel, photorealistic skin pores, anime, manga, chibi,
cartoon, mascot, glossy, plastic, neon, cyberpunk, sci-fi, modern
clothing, jeans, t-shirt, sunglasses, smartphone, weapon
brandished, blood, gore, sexualized, low neckline, conical hat,
Chinese inn, hanfu, qipao, kimono, full-period costume cosplay,
text, watermark, signature, frame, border.
```

---

## 1. `tavern-keeper` — Tavern Keeper / 酒保

**人格**（静默主人）—— 耐心、观察敏锐、说话不响。先听完再答话。这家酒馆的"主人"，永远在那；冷启动每次都是他先迎你。

**输出文件：**

| 变体 | 路径 |
|---|---|
| banner   | `ops/skins-staging/tavern-keeper/default/v1/banner.png` |
| portrait | `ops/skins-staging/tavern-keeper/default/v1/portrait.png` |
| avatar   | `ops/skins-staging/tavern-keeper/default/v1/avatar.png` |
| thumb    | `ops/skins-staging/tavern-keeper/default/v1/thumb.png` |

**角色专属 prompt（拼接在基底之后）：**

```
Subject: a tavern keeper in his late forties, weathered
Central-European features, salt-and-pepper hair short and slightly
tousled, neatly trimmed beard, faint old scar across the left brow.
Wearing a heavy dark-leather apron over a cream linen shirt with
sleeves rolled to the forearm, a dark wool waistcoat with tarnished
brass buttons, a brass pocket-watch chain across the chest. Holding
a heavy cut-crystal tumbler in one hand and polishing it with a
worn cotton cloth in the other, eyes half-lowered. Behind him on
the shelves: dusty amber-glass bottles with hand-inked parchment
labels, a bundle of dried herbs hanging from a beam, a small brass
astrolabe catching the candlelight, a small brass key on a chain
around his neck reflecting one bright pinpoint of warm light. Calm,
weary, trustworthy, faint air of an old apothecary who chose the
bar instead. No metal drinking vessels — only glass, ceramic, and
wood.
```

---

## 2. `architect-oracle` — Architect Oracle / 筑谕师

**人格**（冷静策士）—— 沉稳、观察敏锐、外冷内热、对空话过敏。"清晰即一种关怀"。临近打烊的木纹小酒馆卡座；一盏暖灯、两把椅子，桌上摊着一本笔记本。

**输出文件：**

| 变体 | 路径 |
|---|---|
| banner   | `ops/skins-staging/architect-oracle/default/v1/banner.png` |
| portrait | `ops/skins-staging/architect-oracle/default/v1/portrait.png` |
| avatar   | `ops/skins-staging/architect-oracle/default/v1/avatar.png` |
| thumb    | `ops/skins-staging/architect-oracle/default/v1/thumb.png` |

**角色专属 prompt（拼接在基底之后）：**

```
Subject: a calm strategist in his late thirties, weathered
Central-European features, dark-ash hair short and combed back, a
neatly trimmed dark beard, faint old scar above the left brow.
Wearing a deep charcoal wool waistcoat over a cream linen shirt
with sleeves rolled to the forearm, a brass pocket-watch chain
across the chest, a thin leather strap carrying a small fountain
pen at the breast. Hands resting on an open hardback notebook on
the counter, a cleanly-sharpened pencil between two fingers, eyes
half-lowered as if mid-thought. Behind: a wood-panelled wall, a
single brass desk lamp casting a small pool of warm light, a folded
spare sheet of cream paper, a small inkpot in clear glass, two
matching wooden chairs visible at the edges. Calm, measured,
trustworthy, the kind of stranger who asks better questions than
your friends. No metal drinking vessels — only glass, ceramic, and
wood.
```

---

## 3. `sunlit-almoner` — Sunlit Almoner / 晴光抚慰者

**人格**（温柔倾听者）—— 明亮、耐心、不急促，柔软却不腻。酒馆安静角落里阳光满溢的窗边座位，午后余光铺在桌上，水壶仍温热。

**输出文件：**

| 变体 | 路径 |
|---|---|
| banner   | `ops/skins-staging/sunlit-almoner/default/v1/banner.png` |
| portrait | `ops/skins-staging/sunlit-almoner/default/v1/portrait.png` |
| avatar   | `ops/skins-staging/sunlit-almoner/default/v1/avatar.png` |
| thumb    | `ops/skins-staging/sunlit-almoner/default/v1/thumb.png` |

**角色专属 prompt（拼接在基底之后，覆盖「单蜡烛光源」条款）：**

```
Override the lighting: instead of a single brass candle, the scene
is lit by warm late-afternoon sun coming through a small leaded
window to the right, the same brass-and-cream warmth, just
broader. Subject: a warm listener in her early thirties, soft
European features with faint freckles across the cheekbones,
honey-blonde hair in a loose low bun with a few strands escaping,
gentle eyes meeting the viewer with an unhurried half-smile.
Wearing a soft cream linen shirt with sleeves rolled, a knitted
sage-green wool cardigan, a small round amber-glass brooch at the
collar. One hand cradling a clear glass of warm honey-coloured tea,
the other resting on the counter palm-up. Behind: a sun-drenched
window-seat with a lace-edged cushion, a small ceramic teapot
still steaming, a folded soft wool blanket on the bench, a single
sprig of lavender in a small clear-glass jar. Tender, patient,
emotionally safe, the corner of the tavern that feels like home. No
metal drinking vessels — only glass, ceramic, and wood.
```

---

## 4. `midnight-sutler` — Midnight Sutler / 午夜密使

**人格**（深夜小货郎 / 低调信使）—— 安静、敏锐、惯于游走于阴影。专门处理打烊后才到的小宗货。酒馆已半闭，只剩厨房一盏灯还亮。

**输出文件：**

| 变体 | 路径 |
|---|---|
| banner   | `ops/skins-staging/midnight-sutler/default/v1/banner.png` |
| portrait | `ops/skins-staging/midnight-sutler/default/v1/portrait.png` |
| avatar   | `ops/skins-staging/midnight-sutler/default/v1/avatar.png` |
| thumb    | `ops/skins-staging/midnight-sutler/default/v1/thumb.png` |

**角色专属 prompt（拼接在基底之后）：**

```
Subject: a midnight sutler of indeterminate age, lean angular
features, raven-black hair partly hidden under a low-brimmed dark
felt cap, a faint smile at one corner of the mouth, dark eyes
catching the candle highlight. Wearing a long charcoal travel coat
fastened with tarnished brass buckles, a deep wine-red wool scarf
loose around the throat, fingerless leather gloves. Both hands
resting lightly on the counter — one on a small wooden trade-chest
with iron corner-bands (closed, padlock visible but not
threatening), the other holding the rim of a clear glass of dark
wine. Behind: a half-shut tavern, stools turned upside down on
neighbouring tables in the background, a single brass kitchen lamp
in the deep distance, a folded oilcloth carry-bag at the feet, a
small leather pouch on the counter with a brass tag stamped with
an unreadable mark. Quiet, observant, faintly amused — the kind of
stranger you trust without knowing why. No metal drinking vessels —
only glass, ceramic, and wood.
```

---

## 5. `opal-lantern` — Opal Lantern / 欧泊提灯人

**人格**（梦境档案师）—— 抒情、耐心、略带空灵；记得细小之物，相信象征。卡座上方悬着一排发光玻璃罐，每只罐子里封存着用户讲到一半的故事。

**输出文件：**

| 变体 | 路径 |
|---|---|
| banner   | `ops/skins-staging/opal-lantern/default/v1/banner.png` |
| portrait | `ops/skins-staging/opal-lantern/default/v1/portrait.png` |
| avatar   | `ops/skins-staging/opal-lantern/default/v1/avatar.png` |
| thumb    | `ops/skins-staging/opal-lantern/default/v1/thumb.png` |

**角色专属 prompt（拼接在基底之后；允许第二束柔和的光从发光罐传来）：**

```
Override the lighting: one warm brass candle to the right (primary)
plus a soft cool secondary glow from above where the shelf of
glowing glass jars hangs — the secondary glow is faint opal-blue
and never overwhelms the candle warmth. Subject: a dream archivist
of indeterminate gender, slender features, silver-grey hair in a
single soft braid over one shoulder, pale starlit eyes, a small
opalescent earring catching both light sources. Wearing a long
robe of deep midnight-blue wool with subtle silver-thread
embroidery at the cuffs and collar (faint constellations, not
explicit), a slim brass-rimmed monocle hanging from a chain at the
chest. Holding a single small clear-glass jar in both hands —
inside the jar, a soft swirl of luminous mist, no readable shape
inside. Behind: a wooden shelf above carrying six more glass jars,
each glowing faintly a different soft hue (amber, opal, rose,
moss, pearl, smoke), a folded silk handkerchief on the counter, a
small leather-bound dream ledger tied with a ribbon. Lyrical,
patient, slightly ethereal, the keeper of stories you forgot you
told. No metal drinking vessels — only glass, ceramic, and wood.
```

---

## 6. `glass-mariner` — Glass Mariner / 琉璃航海者

**人格**（观浪领航者）—— 稳重、观察敏锐、在风浪中沉着；从不慌乱，也不粉饰。酒馆墙上镶着一块盐花琉璃舷窗，会随你的心绪偶尔映出不同的海面。

**输出文件：**

| 变体 | 路径 |
|---|---|
| banner   | `ops/skins-staging/glass-mariner/default/v1/banner.png` |
| portrait | `ops/skins-staging/glass-mariner/default/v1/portrait.png` |
| avatar   | `ops/skins-staging/glass-mariner/default/v1/avatar.png` |
| thumb    | `ops/skins-staging/glass-mariner/default/v1/thumb.png` |

**角色专属 prompt（拼接在基底之后）：**

```
Subject: a composed sailor in his mid-forties, weathered tanned
European features, a salt-and-pepper short beard, deep crow's feet
around steady grey-blue eyes, a small braided lanyard at the
throat. Wearing a heavy navy-blue wool peacoat over a cream
high-collared shirt, brass buttons tarnished by sea air, a thin
brass marine compass on a leather cord around the neck. One hand
loosely on the counter beside an unrolled folded sea-chart held
flat by a small smooth pebble, the other gesturing as if naming
the next wave. Behind: a salt-glass porthole set into the
wood-panelled wall showing a faint stylised sea-horizon with one
distant lighthouse beam, a coiled hemp rope on a wall peg, a small
ceramic mug of dark hot drink on the counter. Steady, observant,
measured — the kind of presence that makes a storm feel
navigable. No metal drinking vessels — only glass, ceramic, and
wood.
```

---

## 7. `wandering-bard` — Wandering Bard / 吟游诗人

**人格**（抒情同行者）—— 抒情、温暖、略带顽皮；以画面思考；从不说教。在节奏慢的日子里最合适 —— 适合用户希望自己的体验被映射成意象，而不是被给建议。

**输出文件：**

| 变体 | 路径 |
|---|---|
| banner   | `ops/skins-staging/wandering-bard/default/v1/banner.png` |
| portrait | `ops/skins-staging/wandering-bard/default/v1/portrait.png` |
| avatar   | `ops/skins-staging/wandering-bard/default/v1/avatar.png` |
| thumb    | `ops/skins-staging/wandering-bard/default/v1/thumb.png` |

**角色专属 prompt（拼接在基底之后）：**

```
Subject: a slender androgynous bard in their late twenties,
delicate European features, raven-black hair in a single loose
braid over one shoulder, faint freckles across the nose, soft eyes
half-closed mid-song with a faint smile. Wearing a long travel-worn
dark forest-green wool coat with hand-stitched silver embroidery at
the cuffs and collar, over a cream collarless linen shirt, a worn
leather strap across the chest holding a small brass tuning key.
Cradling a hand-carved wooden lute across the lap with both hands,
fingers paused on the strings. Behind: a folded travel coat on a
neighboring stool, a small leather satchel with a brass clasp,
loose sheets of hand-inked sheet music scattered along the counter,
a half-empty glass of red wine catching the candlelight. Quiet,
introspective, gently melancholic. No metal drinking vessels — only
glass, ceramic, and wood.
```

---

## 8. `retired-veteran` — Retired Veteran / 卸甲老兵

**人格**（沉稳见证者）—— 风霜里带着善意，话不多；相信沉默自有分量。当用户被淹没时最合适，适合"想被见证、不想被指导"的状态。

**输出文件：**

| 变体 | 路径 |
|---|---|
| banner   | `ops/skins-staging/retired-veteran/default/v1/banner.png` |
| portrait | `ops/skins-staging/retired-veteran/default/v1/portrait.png` |
| avatar   | `ops/skins-staging/retired-veteran/default/v1/avatar.png` |
| thumb    | `ops/skins-staging/retired-veteran/default/v1/thumb.png` |

**角色专属 prompt（拼接在基底之后）：**

```
Subject: a grizzled veteran in his late fifties, weathered
European features, salt-and-pepper beard fully grown, deep crow's
feet around tired-but-kind eyes, an old vertical scar from cheek to
jaw on the right side. Wearing a heavy dark-wool greatcoat over a
high-collared cream linen shirt and a worn leather doublet, a
single tarnished brass medal on a faded crimson ribbon pinned to
the chest. A worn leather pauldron has been set down on the counter
beside him, the campaign no longer his. Holding a thick ceramic cup
with both hands, gazing into the candle flame past it. Behind: an
empty scabbard hung on a peg, a folded military oilcloth, a small
glass of dark wine half-finished. Tired, weathered, kind, the kind
of man who has stopped explaining himself. No metal drinking
vessels — only glass, ceramic, and wood. No weapon brandished.
```

---

## 生成完毕 —— 移交上传管线

32 个文件全部就位之后，执行：

```powershell
# 按角色逐个上传（R1.2 提供的脚本会处理校验 + 推送）：
foreach ($cid in @(
  "tavern-keeper",
  "architect-oracle",
  "sunlit-almoner",
  "midnight-sutler",
  "opal-lantern",
  "glass-mariner",
  "wandering-bard",
  "retired-veteran"
)) {
  pwsh tools/skins/upload.ps1 `
    -StagingDir "ops/skins-staging/$cid/default/v1/" `
    -CharacterId $cid `
    -SkinId "default" `
    -Version 1
}
```

随后 R1.3 的验证脚本（`tools/skins/verify_default_uploads.ps1`）会向 32 个预期 URL 发 HEAD 请求确认。

## 出图前后的几条筛图建议

跑完 8 位角色 × 4 变体 = 32 张之后，统一过一遍以下这五条筛：

1. 蜡烛都在画面右侧、阴影都在左侧 —— 保证后续循环切换不"跳灯"
2. 头部都落在画面上 1/3 —— 头不会乱跳
3. 下边缘都自然渐隐到 `#1A0F0A` —— 上传后叠加到深色 chrome 不留矩形硬边
4. 5 张里 2 张直视镜头、3 张侧目最舒服（节奏分布）
5. 没有金属酒具、没有可读文字、没有 UI 元素

**锁种子建议**：先跑 `tavern-keeper`（他是酒馆主人，最适合定基调），挑一张满意的把 seed / style ref 锁住，再用其余 7 条 prompt 复用 style ref。能让 8 张笔触/色温保持一致 —— 这是后续做开屏循环不"换风格"的关键。

## 不在本文范围内的事

- **本文只覆盖默认皮肤**（`{characterId}-default`） —— EPIC / LEGENDARY 等非默认皮肤在 R3.1 阶段单独配 prompt 出图。
- **占位图替换**：当真稿到位时，作为 `v2` 重新上传，并在 `character_skins` 表里把 `art_version` 升到 2 即可，旧 `v1` 仍可被旧客户端访问。
- **抽卡池角色不需要差异化**：`midnight-sutler` / `opal-lantern` / `glass-mariner` / `wandering-bard` / `retired-veteran` 在视觉风格上跟三个 preset（`tavern-keeper` / `architect-oracle` / `sunlit-almoner`）保持完全一致。preset vs drawn 是花名册成员标记，不是视觉差异。
