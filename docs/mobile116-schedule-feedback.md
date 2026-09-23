# Mobile 116: reasoning separators, notification feedback, schedule evidence

- The reasoning header owns the persistent divider. Only expanded reasoning
  adds a second divider before the answer.
- Checking notifications shows a busy label/spinner and a persistent result.
  Permission denial points to system settings; a successful binding is not a
  guarantee that a notification will bypass Focus or background restrictions.
- Schedule review sends recipient-scoped dates in readable Shanghai local time
  and includes creation-date context for historical relative wording. A batch
  conflict must reference a recipient in at least two proposed tasks.
- Scheduling prompts must not invent an end time or duration. A 15:00 meeting
  and a 16:00 conversation are not a proven overlap without duration evidence.
  Equal starts and explicitly overlapping intervals can still be conflicts.
- Confirmation updates obsolete conflict questions and preserves message
  metadata. Historical confirmed cards render a receipt for the old generated
  question, without rewriting stored conversations. Pending questions remain.

## Validation

- Four calls to the configured model with synthetic schedules: different start
  times with unknown duration and different dates returned no conflict; equal
  starts and an explicit 15:00–17:00 interval overlapping 16:00 returned conflict.
- `ScheduleEvidenceRegression` runs isolated H2 accounts on top of Stage 1/2:
  empty calendars, different recipients, scoped timestamp serialization,
  invalid batch references, stopped schedules, stale approval, and historical
  card display. It does not create production reminders.
- App: 62 tests, syntax checks, Android/iOS/Web resource builds, whole generated
  Android Kotlin compilation, and browser divider/notification-feedback checks.
- Backend production sources compile with Maven. Legacy template test sources
  contain pre-existing syntax errors; the independent H2 regression runner was
  used instead of claiming the Maven test suite passed.

Run the regression from the source root after compiling the backend, using
`output/stage1-classpath.txt`, `code/back/target/classes`, and
`output/stage1-h2.jar` as the classpath. Compile `Stage1Regression.java`,
`Stage2Regression.java`, and `ScheduleEvidenceRegression.java` from `scripts`,
then run `ScheduleEvidenceRegression` with the resulting classes on the path.
