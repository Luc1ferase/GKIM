## Context

The repository already has partial guidance for task reviews in `docs/AGENTS.md` and a numeric quality expectation in `docs/QUALITY_SCORE.md`, but those rules are currently advisory and unevenly applied. Recent implementation work also showed that large changes can be broken into many subtasks, yet the repository has no formal delivery contract that says when a subtask is allowed to count as complete, what evidence must be collected, or when code should be pushed to GitHub.

This change formalizes that workflow as an OpenSpec capability. The goal is to make each subtask end with the same gate: verification, code review, numeric scoring, and GitHub synchronization. The constraint is that the new rule must be usable immediately in human-and-agent-driven implementation sessions without depending on new CI infrastructure.

## Goals / Non-Goals

**Goals:**
- Define a repository-wide delivery workflow that requires every implementation subtask to pause for review before it is marked complete.
- Reuse the existing quality-score concept and make `95+` the explicit threshold for task acceptance and GitHub upload.
- Require approved subtask results to be committed and pushed to the working GitHub branch before the next subtask begins.
- Specify what review evidence must be recorded so the workflow is auditable and repeatable.

**Non-Goals:**
- Replace human judgment with a fully automated scoring system in this change.
- Retroactively replay or re-score already archived changes.
- Define product-specific UI or backend behavior outside of the delivery workflow itself.
- Guarantee automatic merge-to-main behavior after each subtask; this change focuses on review approval and remote upload to the active branch.

## Decisions

### 1. Treat per-task review as a mandatory completion gate
Each implementation task will be considered incomplete until the task owner runs the required verification commands, performs a code review pass, and records the review outcome. This turns review from a best practice into a hard workflow checkpoint.

Why this decision:
- It matches the user requirement that every small task be reviewed as soon as it is finished.
- It prevents long changes from accumulating hidden issues until the end.
- It aligns with the existing guidance in `docs/AGENTS.md`, but makes the rule explicit and enforceable in OpenSpec terms.

Alternatives considered:
- Review only at the end of the whole change: rejected because it allows low-quality subtasks to stack up.
- Review only “important” tasks: rejected because it introduces ambiguity and inconsistent enforcement.

### 2. Use the repository quality score as the acceptance threshold and block GitHub upload below 95
A subtask will only be accepted when the review result reaches a quality score of at least `95`. Scores below that threshold must produce revision feedback, and the subtask must remain open until the issues are fixed and re-reviewed.

Why this decision:
- The threshold already exists in `docs/QUALITY_SCORE.md`, so formalizing it avoids inventing a second scoring system.
- A numeric gate is easier to audit than vague wording like “looks good enough”.
- Tying the score to upload prevents remote branches from filling with known-substandard checkpoints.

Alternatives considered:
- Lower thresholds such as 90: rejected because the user explicitly asked for 95+.
- Pass/fail review with no score: rejected because it discards an existing repository convention.

### 3. Define “upload to GitHub” as commit plus push to the active remote branch after approval
Once a subtask passes review, the accepted changes must be committed and pushed to the active working branch before the next subtask starts. If the push fails, the subtask is not fully complete.

Why this decision:
- It creates durable remote checkpoints after every accepted subtask.
- It matches the user’s request to upload accepted work to GitHub immediately.
- It avoids the stronger assumption that every approved subtask should auto-merge into the default branch.

Alternatives considered:
- Delay push until the full change is done: rejected because it breaks the requested per-task upload behavior.
- Auto-merge every accepted task: rejected because many repositories still need branch protection or human integration decisions.

### 4. Record review evidence as part of the task workflow
Each accepted subtask must capture the verification commands run, the review score, the key findings or “no findings” note, and the GitHub upload result such as commit SHA or pushed branch reference.

Why this decision:
- It makes future sessions resumable and auditable.
- It gives humans and agents a shared checklist for what “done” means.
- It reduces disputes about whether a task truly passed review before upload.

Alternatives considered:
- Rely on chat history only: rejected because session history is too easy to lose.
- Require a separate heavyweight review document per task: rejected because it would add too much friction for small subtasks.

## Risks / Trade-offs

- [Per-task review adds overhead to small tasks] → Mitigation: keep the evidence format lightweight and standardized.
- [Quality scoring may remain partly subjective] → Mitigation: anchor scoring to `docs/QUALITY_SCORE.md` and require concrete findings when score is below 95.
- [GitHub push can fail for environmental reasons such as auth or branch protection] → Mitigation: treat push failure as a blocker that must be surfaced immediately instead of silently continuing.
- [Workflow-only enforcement may drift without automation] → Mitigation: define the contract first and leave room for later CI or script-based enforcement.

## Migration Plan

1. Add a new OpenSpec capability for delivery quality gates.
2. Update the main workflow documents and task-writing guidance so future implementations inherit the review-and-push gate by default.
3. Introduce a standard per-task review evidence format that captures verification, score, and GitHub upload result.
4. Begin applying the gate to all new implementation work after this change is accepted.
5. If the workflow proves too heavy, adjust the evidence format later without weakening the core 95+ review gate.

## Open Questions

- Should the repository later add a script or GitHub Action to calculate or validate the recorded score automatically?
- Should pushed per-task checkpoints use one commit per task, or can a task contain multiple commits as long as the final reviewed state is pushed?
- Do we want a dedicated location in the repo for storing per-task review records beyond task files and session output?
