## Why

The repository already mentions review and quality-score expectations in `docs/AGENTS.md` and `docs/QUALITY_SCORE.md`, but those expectations are not yet formalized as an OpenSpec-driven delivery workflow. We need a change that turns the review gate into an explicit implementation contract so future task execution consistently stops for code review, enforces a 95+ quality threshold, and syncs accepted task outcomes to GitHub.

## What Changes

- Introduce a delivery workflow capability that requires every implementation subtask to end with a code review step before the task is considered complete.
- Define a quality gate that blocks task completion and GitHub upload until the reviewed code reaches a quality score of 95 or higher.
- Require accepted subtasks to be uploaded to the GitHub repository immediately after passing review instead of waiting until the entire change is finished.
- Clarify the evidence expected for each subtask gate, including review findings, verification results, and the upload outcome.

## Capabilities

### New Capabilities
- `delivery-quality-gates`: Defines per-task review, scoring, approval, and GitHub upload requirements for future implementation work.

### Modified Capabilities
- None.

## Impact

- Affects how future OpenSpec task lists are written and executed.
- Affects agent workflow guidance, review checkpoints, quality-scoring expectations, and GitHub synchronization behavior.
- May require updates to review prompts, task-completion checklists, and any supporting automation used to score or upload changes.
