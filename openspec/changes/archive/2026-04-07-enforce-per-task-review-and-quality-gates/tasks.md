## 1. Align repository workflow rules

- [x] 1.1 Update the main workflow guidance so implementation sessions treat per-task review as a required completion gate instead of an optional best practice.
- [x] 1.2 Revise `docs/AGENTS.md` to require a review pass, a quality score of 95 or higher, and a GitHub push before the next implementation task begins.
- [x] 1.3 Revise `docs/QUALITY_SCORE.md` so the 95+ threshold explicitly applies to each accepted task and not only to end-of-change merge decisions.

## 2. Add lightweight review and upload evidence

- [x] 2.1 Define a standard per-task completion record that captures verification commands, review score, key findings or no-findings, and GitHub upload outcome.
- [x] 2.2 Update the implementation workflow instructions so a task cannot be checked off until its review evidence has been recorded.
- [x] 2.3 Clarify the GitHub upload rule as commit plus push to the active working branch, including blocker handling when the upload step fails.

## 3. Validate rollout expectations

- [x] 3.1 Review task-writing guidance and examples so future task lists are small enough to support review-and-push after each task.
- [x] 3.2 Run a consistency pass across the new spec, workflow docs, and review wording to remove conflicting instructions such as end-only review or auto-merge assumptions.
- [x] 3.3 Document the expected apply-session output so each completed task reports the quality score and pushed branch or commit evidence before work continues.
