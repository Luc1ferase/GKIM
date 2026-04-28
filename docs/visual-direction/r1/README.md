# R1 baseline screenshots — tavern-visual-direction-redesign

These four PNGs are the baseline after R1.1 (palette swap) + R1.2
(packaged Latin fonts) + R1.3 (typography binding). They predate every
R2/R3/R4 change, so they show the new Tavern palette and typography
running on the old IM-residue layout (3-tab bottom nav, default
messages route, pill-heavy tavern home, letter-monogram avatars,
Material default FAB).

| File | Surface | Notes |
|---|---|---|
| `messages-list.png` | `消息` empty state | Tavern Light surface; IM-residue empty-state copy ("登录后开始和联系人聊天，这里就会出现你的会话。") still present — R2.3 rewrites it. |
| `tavern-home.png`   | Tavern home | Pill row `设置 / 抽卡 / 导入卡` is the explicit R3.1 anti-pattern; `AO` letter monogram on the preset card is the explicit R3.2 anti-pattern. |
| `character-detail.png` | 筑谕师 detail | Demonstrates Newsreader on the title and Inter on body. Pill triple `激活 / 导出 PNG / 导出 JSON` is also part of the R3.1 audit. |
| `settings.png`      | Settings menu | Confirms the new palette + typography flow into the chrome surfaces with no per-screen layout pass (per R1's "no layout edits" gate). |

The "chat with Streaming + Completed bubble" capture mentioned in
`tasks.md` §R1.4 is intentionally deferred to R4.4: the chat-entry
flow under R1's IA passes through the `激活`-then-`ensureConversation`
path, which depends on a logged-in messaging session and creates real
side effects on the production backend. R4.4 captures will use a real
chat session opened from the same emulator after R3 + R4 land.

## Reproducing

```powershell
pwsh tools/visual-direction/capture_chrome.ps1 -OutDir docs/visual-direction/r1
```

The script asserts each PNG's surface pixel matches Tavern Dark
`#1A0F0A` or Tavern Light `#F1E7D2` (±2 per channel) and that each
file is at least 50 KB (the originally-suggested 200 KB threshold was
relaxed because the messages-list empty state and the portrait-less
character detail are legitimately thin captures; the surface-pixel
gate is the load-bearing palette check).

## Device

Captured against `sdk_gphone64_x86_64` (Pixel 6 profile, API 34, AVD
named `codex_api34` locally), 1080 × 2400 portrait. Tap coordinates
in the script are anchored to that resolution.
