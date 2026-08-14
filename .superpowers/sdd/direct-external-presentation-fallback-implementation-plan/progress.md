# Direct external fallback implementation ledger

- Task 1: complete on isolated stack branch at `3ff2f22d`; two review fix rounds closed all Critical/Important implementation findings.
- Task 1 verification: focused tests passed; compile and assemble passed; full suite 118 tests with exact frozen five failures, 0 errors, 0 skipped.
- Task 2: complete at `70b1c1f61bae7e6838712c65fa1fdb4655f38945`; reviewed three-commit aggregate integrated as one commit without conflict.
- Task 2 verification: combined focused tests passed; compile, native build, and lowLatencyDebug assemble passed; full suite 205 tests with exact frozen five failures, 0 errors, 0 skipped; APK identity and signature verified.
- Standards review: the approved direct-first design intentionally conflicts with the older local `AGENTS.md` Presentation-only prohibition on `ActivityOptions.setLaunchDisplayId()`; the local guide remains unstaged and should be reconciled separately.
- Spec review: pass with two non-blocking minor gaps (explicit-Presentation fallback reason wording; direct-IDLE stop/destroy test depth).
- Publication decision: keep branch as-is; no push or PR until PR #589 is merged.
