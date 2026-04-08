## ADDED Requirements

### Requirement: Every implementation task MUST pause for review before completion
The system SHALL treat every implementation task or subtask as incomplete until the task owner has finished the required verification steps and completed a code review pass for that task.

#### Scenario: Task finishes implementation work
- **WHEN** the task owner believes a task implementation is complete
- **THEN** the workflow requires verification and code review before the task can be marked done

#### Scenario: Review has not happened yet
- **WHEN** implementation changes exist but no review outcome has been recorded
- **THEN** the task MUST remain open and the next task MUST NOT start as though the current task were accepted

### Requirement: Reviewed tasks MUST reach a quality score of at least 95 before acceptance
The system SHALL block task acceptance when the reviewed result scores below `95`, and it MUST require revision feedback plus a follow-up review until the task reaches the threshold.

#### Scenario: Review score meets the threshold
- **WHEN** a task review produces a score of `95` or higher
- **THEN** the task is eligible for approval and GitHub upload

#### Scenario: Review score is below the threshold
- **WHEN** a task review produces a score below `95`
- **THEN** the workflow records the findings, rejects task completion, and requires the task to be fixed and reviewed again

### Requirement: Approved tasks MUST be uploaded to GitHub before the next task begins
The system SHALL require every approved task result to be committed and pushed to the active GitHub branch before the following task is treated as active work.

#### Scenario: Approved task is ready for upload
- **WHEN** a task has passed review with a score of `95` or higher
- **THEN** the accepted changes are committed and pushed to the active remote branch before the next task starts

#### Scenario: GitHub upload fails
- **WHEN** the commit or push step fails after a task has been approved
- **THEN** the workflow reports the upload failure as a blocker and the task remains not fully complete

### Requirement: Task completion records MUST include review and upload evidence
The system SHALL capture the verification commands, review result, quality score, and GitHub upload outcome for each accepted task so that the workflow can be audited and resumed safely.

#### Scenario: Accepted task is recorded
- **WHEN** a task passes review and upload
- **THEN** the completion record includes the verification evidence, the review score, a summary of findings or no-findings, and the GitHub upload result

#### Scenario: Rejected task is recorded
- **WHEN** a task fails review or upload
- **THEN** the workflow records the blocking reason and leaves the task in a not-done state
