---
name: code-review
description: Review diffs, branches, commits, or pull requests in the HomeWorkout Android project for introduced correctness bugs, regressions, data-integrity and security risks, Clean Architecture violations, Android lifecycle issues, and missing verification. Use for code-review requests in this repository; do not use for implementation unless the user also asks for fixes.
---

# HomeWorkout Code Review

Review the requested change, not the repository in the abstract. Prioritize defects that the author can act on and that are introduced or exposed by the reviewed diff.

## Establish scope

1. Read the applicable `AGENTS.md` or `AGENT.md` instructions and always read `docs/architecture.md` completely.
2. Determine the review target from the request: working tree, staged changes, commit, branch, or pull-request diff. If no target is named, review the current working-tree diff against `HEAD`.
3. Inspect `git status` before reviewing. Treat unrelated dirty or untracked files as user-owned and do not modify them.
4. Read every changed hunk plus enough surrounding code, callers, models, SQL, navigation, manifest, and DI wiring to prove or disprove an issue. Do not review a patch in isolation.
5. Keep the review read-only. Run safe builds or tests when useful, but edit files only if the user explicitly asks for fixes.

## Load feature documentation only when relevant

- Room entities, DAOs, schema, migration, or seed changes: read `docs/db_diagram.dbml`, then verify against `AppDatabase.kt`, current entities, and bundled assets.
- Settings, weekly goal, wheel pickers, or TTS: read `docs/settings-feature.md`.
- Alarm, reminder, receiver, permission, or notification changes: read `docs/notifications.md` and inspect `AndroidManifest.xml`.
- Onboarding, fitness profile, or plan recommendation: read `docs/workout_plan_selection_guide.md` and the relevant parts of `docs/workout_plan_catalog.json`.
- UI fidelity questions: inspect the relevant pages of `docs/system.pdf`, but treat it as an older visual reference rather than authoritative behavior.

Documentation contains historical and stale values. When it conflicts with implementation, use current code, Room entities, `AppDatabase.kt`, and runtime assets as the source of truth. Report a documentation mismatch only when it can mislead this change or should be updated with it.

## Project-specific review checks

Apply only the checks relevant to the diff.

### Architecture and wiring

- `domain/` remains pure Kotlin: no Android, Compose, Room, DAO, Entity, or data-layer imports.
- Entity-to-domain mapping and domain repository implementations stay in `data/repositories/`.
- UI code never imports an Entity or `data/local` type and contains no Room/query logic.
- ViewModels call domain use cases, expose state through `StateFlow`, and do not call repositories or DAOs directly.
- Use cases have one responsibility and expose `operator fun invoke(...)`.
- Reusable UI belongs in `ui/components/`; navigation belongs in `ui/navigation/`.
- New repositories, services, use cases, routes, and ViewModel dependencies are wired through the manual DI graph in `ui/App.kt` and `ScreenNavigator.kt`.

Do not flag a pre-existing boundary exception merely because it exists. Flag it when the change introduces, expands, or relies on that violation in a way that creates a concrete risk.

### Room and data integrity

- Every new Entity is registered in the single `AppDatabase`; required DAO access is exposed there.
- Schema changes have an intentional database-version decision. The current destructive-migration setup can erase settings, history, weight logs, fitness profiles, and custom plans, so call out accidental or release-inappropriate data loss.
- Foreign-key delete behavior, unique indexes, ordering, nullable fields, and multi-table writes preserve their business invariants. Require a transaction when partial completion would corrupt a plan or session.
- Enum values are persisted by Kotlin `name`; renames require migration or an explicit destructive-reset decision.
- Timestamps remain epoch milliseconds and reminder time remains `HH:mm` unless the whole read/write path changes together.
- Settings changes update both Entity-to-domain and domain-to-Entity mapping. Whole-object updates must preserve fields not represented by the editing screen.
- Resolve the single local user by the seeded email instead of assuming a fixed numeric ID.
- Session and session-exercise snapshots keep historical behavior stable when plans, exercises, or settings later change.

### Seed data and plan selection

- Treat `app/src/main/assets/` as the runtime source. When a docs copy is intended to mirror an asset, verify both remain synchronized.
- Seeding stays idempotent and handles destructive migration, partial/empty data, stable external IDs, ordering, and enum parsing without silently creating invalid plans.
- Recommendation changes preserve hard constraints before ranking: safety limitations, explicit exclusions, owned equipment, schedule capacity, and level eligibility.
- Never accept a fallback that relaxes a safety restriction, explicit exclusion, or equipment requirement. Free-text limitations are not proof that injury-specific safety has been implemented.
- Accepted-plan behavior must be explicit: selecting an existing seeded system plan is not equivalent to materializing a per-user generated plan.

### Workout lifecycle and reporting

- Starting a plan resolves one plan day, not every day flattened into one session.
- Resume/restart/abandon/complete transitions preserve the correct day and session; completion writes `COMPLETED` and a non-null `endedAt`.
- Weekly goal, streak, history, and report queries agree on what counts as a completed workout and on their date boundaries/time zone.
- Reset-progress behavior deletes only the intended user's sessions and relies only on verified cascade relationships.

### Compose, Flow, and Android lifecycle

- Room-backed screens do not briefly render hard-coded defaults before their first real emission; use the established readiness pattern where applicable.
- Editable local state is seeded once and is not overwritten by later Flow emissions while the user is editing.
- Long-lived `LaunchedEffect` or collectors do not capture stale callbacks or sibling state; use `rememberUpdatedState` when needed.
- Fast-changing scroll/layout state uses derived state appropriately, list keys are stable, and work is not repeated on every frame without need.
- Side effects are lifecycle-aware and cleaned up: collectors, TTS/media objects, receivers, alarms, and temporary files must not leak or outlive their owner incorrectly.

### Notifications and platform behavior

- Notification permission checks match the supported API levels and denied permission cannot leave persisted reminder state out of sync with scheduling.
- Exact-alarm fallback does not crash; daily alarms re-arm after firing and after boot.
- Receiver export flags, intent filters, PendingIntent identity/flags, and notification-channel behavior are correct.
- Distinguish an application defect from OEM background-killer behavior, but require device/manual verification for behavior a JVM test cannot establish.
- Treat `USE_EXACT_ALARM` as a Play Store policy risk unless the product qualifies for it.

### Security and privacy

- New permissions are necessary and narrowly scoped; exported Android components cannot accept unintended external input.
- Internal broadcasts and PendingIntents use explicit targets and appropriate mutability flags.
- Logs, assets, source, and build configuration contain no secrets, credentials, password material, or unnecessary personal/health data.
- External URLs and intents are validated for the intended destination, and local-only user assumptions are not silently extended into real authentication or synchronization.

## Verification

Choose checks proportional to the changed area. Prefer existing tests, focused Gradle tasks, and `./gradlew assembleDebug` for compile/integration coverage. Do not claim runtime behavior was verified by compilation alone. Notification delivery, boot restore, TTS voice quality, screen-timeout behavior, and OEM background execution may require an emulator or physical device.

If verification cannot be run, state exactly what was not run and why. Missing tests are a finding only when the change introduces meaningful unverified logic or a regression-prone behavior, not simply because the repository has little test coverage.

## Findings format

Present findings first, ordered by severity and then confidence. For each finding include:

- `[P0]` to `[P3]` and a concise title.
- An exact clickable file and line reference.
- The concrete execution path or input that triggers it.
- The user/data/architecture impact.
- A focused fix consistent with this project's layering.

Severity calibration:

- `P0`: catastrophic or broadly irreversible impact, such as widespread unrecoverable data loss or a critical security failure.
- `P1`: serious user-visible failure, safety violation, crash, or likely loss/corruption of user data.
- `P2`: meaningful defect affecting a narrower path, important regression risk, or boundary violation with a concrete consequence.
- `P3`: small but real correctness or maintainability issue worth fixing; omit purely cosmetic preferences.

Do not report speculation as a confirmed bug. Trace values and state across layers, verify platform/API assumptions, and cite the relevant changed line. Consolidate findings with the same root cause. Avoid reporting unrelated pre-existing problems unless they directly invalidate the change; place necessary context under residual risks instead.

After findings, include brief sections for open questions/assumptions and verification performed when they add value. If there are no findings, say so explicitly and still mention material residual risks or untested paths.
