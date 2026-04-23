## 1. Bilingual Companion Copy Contract

- [x] 1.1 Introduce shared bilingual companion-copy types for Android models, seed data, and backend DTOs so `displayName`, `roleLabel`, `summary`, and `openingLine` all carry English and Chinese variants.
- [x] 1.2 Update companion-to-UI/contact mapping helpers so tavern cards, draw results, and companion-chat identity labels resolve the active language from the shared bilingual contract.

## 2. Backend Persistence And API Migration

- [x] 2.1 Add a backend migration that expands `companion_characters` from single-language text fields to explicit English/Chinese content and backfills the shipped preset/draw characters.
- [x] 2.2 Update the private backend companion repository, service, and HTTP serialization layers so roster and draw APIs return bilingual companion content for preset, owned, and drawn cards.

## 3. Android Rendering And Language Switching

- [x] 3.1 Update tavern and companion-chat UI rendering to show Chinese companion copy in Chinese mode and English companion copy in English mode without mixed-language card content.
- [x] 3.2 Ensure existing loaded roster state refreshes correctly after an in-app language switch, including latest draw results and active companion identity labels.

## 4. Verification

- [x] 4.1 Add or update Android tests that verify tavern cards, draw results, and companion chat identity surfaces in both Chinese and English.
- [x] 4.2 Add or update backend tests that verify bilingual companion roster and draw payloads, then record evidence using the repository delivery workflow.
