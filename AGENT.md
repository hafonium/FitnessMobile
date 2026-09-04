## Project documentation

Always read and follow `docs/architecture.md` before making code changes.

Always use English as main language.

Read feature-specific documentation only when relevant:

- Database/schema changes: `docs/db_diagram.dbml`
- Settings, weekly goal, or TTS: `docs/settings-feature.md`
- Reminders or notifications: `docs/notifications.md`
- Onboarding or plan recommendation:
  - `docs/workout_plan_selection_guide.md`
  - `docs/workout_plan_catalog.json`
- UI mockup reference: `docs/system.pdf`

When documentation conflicts with the implementation, treat the current code, `AppDatabase.kt`, Room entities, and bundled assets as the source of truth, and mention the discrepancy.



