# Delivery Workflow - Per-Task Review Gates

`docs/DELIVERY_WORKFLOW.md` is the repository source of truth for how an implementation task becomes accepted.

## Required sequence for every task

1. Finish one scoped task or subtask before starting unrelated follow-up work.
2. Run the verification commands that prove the task works in its current state.
3. Review the task result against the relevant OpenSpec artifacts and `docs/QUALITY_SCORE.md`.
4. Assign a quality score.
5. Record the task evidence using the standard template below.
6. If the task scored `95+`, commit and push the accepted state to the active remote branch.
7. Mark the task checkbox complete only after the push succeeds.

If the review score is below `95`, or the push step fails, the task remains open and the next task must not begin.

## Standard task completion record

Use this template in apply sessions, review notes, or task logs whenever a task is accepted or blocked.

```md
### Task <id>: <task title>

- Verification:
  - `<command>` - pass/fail summary
- Review:
  - Score: `<score>/100`
  - Findings: `No findings` or a concise list of blocking issues
- Upload:
  - Commit: `<sha>` or `not created`
  - Branch: `<branch name>`
  - Push: `<remote>/<branch>` or `failed`
- Result: `accepted` or `blocked`
```

## Blocker handling

- Score below `95`: record the findings, leave the task unchecked, fix the issues, and repeat verification plus review.
- Verification could not run: record the missing verification and the reason, then treat the task as blocked until the risk is accepted explicitly.
- Push failed: record the failure detail, keep the task unchecked, and resume only after the accepted state is uploaded successfully.

## Task sizing guidance

- Write tasks that can be implemented, reviewed, scored, and pushed in one focused session.
- Each task should have one obvious completion point and one obvious verification story.
- If a task cannot be reviewed cleanly on its own diff, split it before implementation starts.

## Expected apply-session output

Use this shape when reporting task completion in an implementation session:

```md
Working on task 2.1: Define a standard per-task completion record

Verification
- `./gradlew testDebugUnitTest` - pass

Review
- Score: 97/100
- Findings: No findings

Upload
- Commit: `abc1234`
- Branch: `feature/delivery-quality-gates`
- Push: `origin/feature/delivery-quality-gates`

✓ Task complete
```

## Remote deployment acceptance example

### Task 4.2: Deploy accepted backend slices to `124.222.15.128`, run remote smoke tests and latency-focused message-flow checks there, and capture the required evidence in `docs/DELIVERY_WORKFLOW.md`.

- Verification:
  - `python (paramiko) -> systemctl is-active gkim-im-backend` - pass (`active`)
  - `python (paramiko) -> systemctl show -p ActiveEnterTimestamp,ExecMainPID gkim-im-backend` - pass (`ExecMainPID=92922`, `ActiveEnterTimestamp=Wed 2026-04-08 17:42:40 CST`)
  - `python (paramiko) -> curl -fsS http://127.0.0.1:18080/health` - pass (`{"service":"gkim-im-backend","status":"ok"}`)
  - `python (paramiko tunnel + requests/websockets)` - pass (`/api/session/dev` + `/api/bootstrap` succeeded, `5` tunneled message rounds completed with receive avg/max `82.99/101.56 ms`, delivery avg/max `164.52/182.16 ms`, read-ack avg/max `144.61/157.77 ms`, and history returned the latest message with both `deliveredAt` and `readAt`)
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `9c5a234`
  - Branch: `master`
  - Push: `origin/master`
- Result: `accepted`
