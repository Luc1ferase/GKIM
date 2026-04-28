## 1. Preservation planning

- [x] 1.1 Inventory the tracked backend source, backend-only operational assets, and public-repo references that must be removed or rewritten before the next public push.
- [x] 1.2 Establish the local/private backend preservation location and Git ignore protection, then verify the current backend tree is safely retained there before any tracked deletion starts.

## 2. Public repository cleanup

- [x] 2.1 Remove tracked backend implementation files and backend-only operational assets from the public repository tip, adding only the minimal sanitized public-facing replacements that are still required.
- [x] 2.2 Update public docs, scripts, specs, and workflow references so the published repository no longer assumes checked-in backend source while private operators still have a clear backend handoff path.

## 3. Verification and evidence

- [x] 3.1 Verify the tracked public diff no longer contains backend source or backend-only deployment assets while the preserved local/private backend copy still exists and is ignored from Git.
- [x] 3.2 Run focused public-repo checks for the surviving docs/workflows, then record the verification and review evidence in `docs/DELIVERY_WORKFLOW.md`.
