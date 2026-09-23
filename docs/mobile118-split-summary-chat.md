# Mobile 118: split summaries and chat continuity

## Behavior
- Split siblings sharing a source candidate now display only their own confirmed branch tasks. Old AI summary caches are ignored on reads; original evidence and later confirmed changes are preserved.
- Background summary generation excludes these split records. Apply `sql/split_event_summary_v118.sql` to index candidate lookups.
- Active thinking uses a moving white shimmer. Reasoning height transitions when opening/collapsing, and live replies retain their component identity through server history reconciliation.
- Bottom following measures content after layout and through the transition; manually scrolling up suspends following, and return-to-latest resumes it.

## Validation
- APP syntax check and 65 unit tests passed.
- Android resource build and generated Kotlin/native typecheck passed (existing deprecation warnings only).
- iOS resource build passed; no signed IPA or physical iPhone motion test performed.
- Java compilation, existing Stage1 regression suite, and 13 split-summary regression assertions passed. Cases include contaminated caches, organizer/recipient scope, later task changes, stored-date wording and unchanged original evidence.
- Mocked browser streaming test passed: 285 frame samples, zero clipped duration, stable message DOM node, upward scrolling respected, return-to-latest restored bottom. No real messages sent by this fixture.

## Backend release
- Image: `aimessage-backend:split-summary-20260923-118`.
- Changes only `SmartEventContentService.class` in the deployed jar; other application classes, dependencies and runtime config are preserved.
- Jar SHA256: `8947e2e5754fceffdf018055f71c8fcbeabcb60bd3d5350903a7b3214273d957`.
- Rollback material: `/opt/aimessage/backups/pre-split-summary-20260923-118`.
- Client visuals require rebuilding/installing versionCode 118; server summary read correction works for existing clients.
