## ADDED Requirements

### Requirement: Production-facing app surfaces avoid development-stage commentary
The system SHALL present shipped Android UI copy as product-facing language, and it MUST NOT render development-stage notes, prototype annotations, or internal explanatory commentary directly inside production-facing app surfaces.

#### Scenario: Space header avoids development commentary
- **WHEN** the user opens the `Space` tab
- **THEN** the visible page chrome does not show internal-facing helper copy such as `创作者动态` or long development-oriented explanatory sentences above the feed

## MODIFIED Requirements

### Requirement: Space feed renders developer-oriented rich posts
The system SHALL provide a `Space` feed optimized for developer posts and prompt-discovery content, and it MUST render mixed discovery content through the shared content-rendering pipeline while exposing `为你推荐`, `提示工程`, `AI 工具`, and `动态` as the visible discovery filter row without showing a separate unread-summary card above the feed.

#### Scenario: Space tab focuses on discovery content without unread summary chrome
- **WHEN** the user opens the `Space` tab
- **THEN** the page does not display a `未读信号` summary card or aggregate unread count panel above the feed

#### Scenario: Space filter row restores four visible discovery entries
- **WHEN** the user views the `Space` discovery rail
- **THEN** the page shows `为你推荐`, `提示工程`, `AI 工具`, and `动态` in the filter row

#### Scenario: Markdown developer post is rendered in the feed
- **WHEN** a `Space` post contains Markdown headings, paragraphs, lists, or code blocks
- **THEN** the feed renders the content with the shared developer-post renderer and design-system styles

#### Scenario: Styled post content uses scoped presentation rules
- **WHEN** a post includes supported CSS presentation metadata or style blocks
- **THEN** the renderer applies the supported styling without breaking feed layout or app theme tokens

#### Scenario: MDX-compatible post document enters the renderer
- **WHEN** a post is authored in the MDX-compatible content format defined by the app
- **THEN** the renderer resolves the document through the shared parsing abstraction instead of bypassing the content pipeline
