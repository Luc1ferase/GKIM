## Why

The new tavern surface already follows the app's Chinese/English shell language, but the companion cards still ship with single-language English content. That creates a mixed-language experience in Chinese mode and makes the character roster feel unfinished just after the tavern flow was introduced.

## What Changes

- Add a canonical bilingual copy contract for companion character content, covering display name, role label, summary, and opening line.
- Update tavern-related Android surfaces to render companion copy from the active app language instead of reading single-language card fields.
- Update seed data and backend companion roster responses so preset cards, draw results, owned cards, and active companion selection all carry both English and Chinese copy.
- Define migration/compatibility behavior for existing single-language companion records so the UI does not silently regress while data is being updated.

## Capabilities

### New Capabilities
- `localized-companion-copy`: Defines the required English and Chinese authoring contract for companion character content and how clients resolve user-facing copy by app language.

### Modified Capabilities
- `core/im-app`: The app requirements change so tavern cards, draw results, and companion-chat identity labels render locale-matched companion copy.
- `im-backend`: The backend requirements change so companion roster APIs and persistence expose bilingual companion copy instead of single-language text fields.

## Impact

- Affected Android code: companion character models, tavern UI rendering, seed data, repository mapping, chat-entry identity labels, and related tests.
- Affected backend code: companion roster models, API serialization, persistence schema/data migration, and backend HTTP tests.
- Affected product behavior: Chinese UI shows Chinese companion content, English UI shows English companion content, and language changes apply consistently across preset, drawn, and active characters.
