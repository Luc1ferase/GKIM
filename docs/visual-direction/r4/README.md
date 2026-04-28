# R4 final screenshots — tavern-visual-direction-redesign

These five PNGs are the after-state of the full slice (R1.1–R4.3 all
landed). Compare against the same captures under
`docs/visual-direction/r1/` to read the R1 → R4 progression.

| File | Surface | What changed since R1 |
|---|---|---|
| `tavern-home.png`      | Tavern home (default landing) | R2.1 lands here on cold start (was messages); R2.2 reduces nav to 2 tabs (酒馆 / 消息) with a 2 dp brass underline on the active tab (R3.4); R3.1 demotes 设置 / 导入卡 to rectangles and promotes 抽卡 to the sole brass pill; R3.2 swaps the AO letter monogram for a silhouette inside a rounded-square; R2.2's "全部陪伴" / "All companions" header sits above the preset / owned / custom sub-sections; R4.2 applies grain + TopEnd candle glow to the outer column. |
| `messages-list.png`    | Messages tab | R2.3 rewrites the empty-state copy to "The bar is empty. Pull up a stool and pick a card to start a conversation." / "还没有人来过你的酒馆…"; R3.3 reframes the new-conversation FAB as a 48 dp rectangular tap target with brass-tinted Add icon (was a pill); R3.2 swaps conversation-row letter monograms for silhouettes. |
| `character-detail.png` | 筑谕师 detail | Inherits the new palette + Newsreader / Inter typography; pill audit unchanged here (the 激活 / 导出 PNG / 导出 JSON triple stays since each is a primary-emotional action on this surface). |
| `settings.png`         | Settings menu | R1.2 added the "Open-source fonts / 开源字体致谢" credits caption at the bottom (Newsreader OFL + Inter OFL). |
| `gacha-result.png`     | Tavern post-draw banner | R4.3 manifest pins "本次抽卡" / "重复抽到" / "Keep as bonus" CTA accents to AetherColors.Tertiary (ember red `#B85450`) and AetherColors.Primary (brass `#E0A04D`); R1.1 swapped both tokens to those tones, so the prior lavender / pink saturation is gone. |

## What R1 vs R4 makes obvious

- **Palette tone**: R1 already lived on the new Tavern Light variant, so
  the surface and text colors on these captures match. The R4 layer
  difference is the chrome-pattern + behavior shift, not the palette.
- **Information architecture**: R1 captures land on `messages` first
  (the IM-residue default) and present three bottom-nav tabs; R4
  captures land on `tavern` and present two.
- **Pill discipline**: R1 tavern-home shows the `[设置] [抽卡] [导入卡]`
  pill triple and an `[AO]` letter monogram on the preset card;
  R4 promotes `抽卡` to the sole brass pill and hides the monogram
  behind the silhouette fallback.
- **FAB**: R1 messages-list shows the pill-style `+` trigger; R4 shows
  the rectangular 48 dp affordance with a brass icon.
- **Bottom-nav active state**: R1 uses Material's pill-behind-icon;
  R4 uses the 2 dp brass underline.
- **Empty-state copy**: R1 messages-list reads "登录后开始和联系人聊天 …";
  R4 reads "还没有人来过你的酒馆 …".
- **Ambient layer**: R4 tavern-home pixel-samples ~4 RGB darker than R1
  on the same surface anchor — the 8 % grain + 5 % candle glow nudging
  the surface luminance subtly, exactly as designed (the bar metaphor
  reads as "weighted material" rather than as a visible texture).

## Reproducing

Same script as R1, against the post-R4 build:

```powershell
pwsh tools/visual-direction/capture_chrome.ps1 -OutDir docs/visual-direction/r4
```

The script's surface-pixel tolerance is widened to ±12 per channel in
this slice (vs ±2 in R1's draft) to absorb the R4.2 ambient layer.
The gacha-result capture taps `抽一张` on the live tavern home before
screencap; if the layout ever shifts, the tap coordinate in the
script needs a refresh (the rest of the navigation is stable against
the R2-IA contract).

## Device

Same as R1: `sdk_gphone64_x86_64` (Pixel 6 profile, API 34, AVD
`codex_api34`), 1080 × 2400 portrait.
