## 1. Product fork foundation

- [x] 1.1 Inventory which current social IM, Space, and AI-tooling surfaces will be retained, hidden, or repurposed on `feature/ai-companion-im`, then document the branch-local product direction in the public repo docs and specs.
- [x] 1.2 Introduce shared companion domain models and contracts across Android and backend for persona identity, relationship state, bounded memory summary, and reply lifecycle.

## 2. Android companion-first experience

- [x] 2.1 Refactor the Android inbox and navigation surfaces so AI companion conversations become first-class entry points and companion identity is visible in the main chat flow.
- [x] 2.2 Implement companion chat-detail lifecycle UI for thinking, progressive reply, completion, reconnect recovery, and explicit failure states.
- [x] 2.3 Rework retained settings and AI control surfaces so model/provider/personalization options are framed around companion behavior instead of creator-first AIGC tooling.

## 3. Backend companion orchestration

- [x] 3.1 Add durable backend support for companion personas, per-user memory summaries, and pending companion turn state.
- [x] 3.2 Implement backend orchestration and realtime delivery for in-progress, completed, and failed companion reply lifecycle events.
- [x] 3.3 Add per-user isolation and safety/boundary handling so companion context cannot leak across accounts and blocked turns resolve explicitly.

## 4. Verification and delivery evidence

- [x] 4.1 Add focused Android and backend coverage for companion inbox rendering, reply lifecycle, memory continuity, and reconnect recovery.
- [x] 4.2 Record verification, review, score, and upload evidence in `docs/DELIVERY_WORKFLOW.md` for the AI companion branch pivot.
