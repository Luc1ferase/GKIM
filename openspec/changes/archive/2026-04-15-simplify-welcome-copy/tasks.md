## 1. Welcome copy contract coverage

- [x] 1.1 Add or refresh focused Android welcome-screen assertions that prove the new product-facing intro copy is rendered and the removed helper/footer wording is absent.

## 2. Welcome copy implementation

- [x] 2.1 Update the main welcome description in `WelcomeRoute.kt` to a more natural bilingual product introduction while preserving the existing welcome layout and auth CTA structure.
- [x] 2.2 Remove the lower helper sentence and simplify the footer line so it no longer includes `加密连接 / Encrypted connection` wording while keeping the lightweight footer treatment in place.

## 3. Verification and evidence

- [x] 3.1 Run focused Android verification for the cleaned-up welcome copy contract.
- [x] 3.2 Record verification, review, score, and evidence updates in `docs/DELIVERY_WORKFLOW.md`, and update any affected welcome/onboarding guidance before closing the change.
