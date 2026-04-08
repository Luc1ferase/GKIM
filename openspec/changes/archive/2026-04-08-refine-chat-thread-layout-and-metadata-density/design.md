## Context

The Android chat detail screen already has a compact top bar, a primary text composer, and direction-specific message bubbles, but the thread still feels spatially loose compared with mature messenger apps. The composer can appear to settle lower as content grows, incoming metadata placement is inconsistent and wasteful, and timestamps still consume too much room relative to message content.

This change should move the chat layout closer to the rhythm of Telegram's default theme without cloning Telegram exactly. The goal is to borrow the density principles: a composer that stays visually anchored, metadata that is compact and predictable, and message bubbles that prioritize conversation content over chrome.

## Goals / Non-Goals

**Goals:**
- Keep the chat composer visually anchored to the bottom edge of the chat screen.
- Align incoming metadata so the avatar and timestamp participate in the same compact top rhythm, with the incoming timestamp rendered on the right side.
- Reduce timestamp spacing and visual weight for both directions so metadata stops dominating the bubble layout.
- Use Telegram-like density and placement cues as a design reference while preserving the existing Aether visual language.
- Protect the updated layout with Compose UI coverage for composer position, incoming metadata geometry, and timestamp compactness.

**Non-Goals:**
- No redesign of navigation, message models, or chat send behavior.
- No exact Telegram theme clone, asset reuse, or one-to-one spacing copy.
- No changes to AIGC action availability or media attachment workflow beyond layout integration.
- No localization or timestamp formatting changes beyond placement and density.

## Decisions

### 1. Treat the composer as a fixed bottom region of the chat screen

The chat screen layout should reserve a persistent bottom region for the composer so new messages or auxiliary panels do not make the input area visually drift. Any secondary content such as the latest generation card should be repositioned so it does not sit below the composer and break the anchored-chat mental model.

Why this decision:
- It directly addresses the user's most obvious interaction complaint.
- It matches the baseline expectation of mainstream chat apps.
- It makes the chat timeline feel scrollable above a stable input surface instead of competing with it.

Alternatives considered:
- Keep the current vertical stacking and only tweak weights: rejected because it leaves the composer vulnerable to other bottom content.
- Overlay the composer above the thread with manual insets only: rejected because it adds more layout complexity than this screen currently needs.

### 2. Compact incoming metadata into a tighter header rhythm

Incoming message presentation should move toward a denser header pattern: avatar on the left, sender/name metadata kept compact, and timestamp aligned to the right so the top metadata line spends less vertical space. The bubble should follow immediately below with reduced unused padding between header information and content.

Why this decision:
- It satisfies the explicit request that the other person's avatar and timestamp feel level and that the timestamp lives on the right.
- It reduces wasted vertical space without hiding important authorship cues.
- It creates a clearer visual parallel between incoming and outgoing metadata density.

Alternatives considered:
- Leave incoming metadata above and below the bubble in separate zones: rejected because it preserves the current loose layout.
- Move all incoming timestamps into the bubble footer immediately: rejected because the user explicitly asked for right-side timestamp alignment relative to the avatar/header rhythm.

### 3. Shrink timestamp footprint instead of removing timestamp information

Timestamp text should remain available but consume less space through tighter placement, smaller typographic emphasis, and reduced dedicated spacing. The layout should use timestamp containers only where they add alignment value and avoid standalone rows that visually inflate the thread.

Why this decision:
- The complaint is about wasted space, not about missing time information.
- Compact metadata supports higher message density while preserving context.
- It aligns with Telegram's habit of making timestamps secondary rather than headline elements.

Alternatives considered:
- Remove timestamps for some messages: rejected because it loses useful context and was not requested.
- Keep the current size and only change color: rejected because spacing, not just tone, is part of the problem.

## Risks / Trade-offs

- [Composer anchoring could conflict with secondary bottom content] -> Move or absorb auxiliary panels into the scrollable region instead of leaving them below the composer.
- [Incoming metadata compaction could become visually cramped on smaller devices] -> Use alignment-focused tests and allow modest spacing buffers rather than chasing pixel-minimal layouts.
- [Telegram-inspired tightening could erode current Aether identity] -> Reuse existing colors, typography tokens, and shapes even while borrowing Telegram's density model.

## Migration Plan

1. Update the `core/im-app` delta spec so composer anchoring and compact incoming metadata behavior are explicit requirements.
2. Add failing Compose UI coverage for bottom-anchored composer position and incoming metadata/timestamp geometry.
3. Refactor the chat layout and message-row metadata structure to satisfy the new density rules.
4. Run chat instrumentation and unit verification, record delivery evidence, and only then move the change toward implementation acceptance.

## Open Questions

- None. Telegram default-theme behavior is being used as a density reference only, so the implementation can proceed with existing Aether tokens and current chat functionality.
