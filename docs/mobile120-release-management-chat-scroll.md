# Mobile 120: release management and chat scrolling

## Release administration
- Store the original APK basename at upload and return fileName in list/latest responses. Legacy rows have no original-name metadata; display their real stored `<id>.apk` filename instead of inventing a name.
- Admin-only deletion requires `published=0` in the same atomic SQL delete, rejects published/missing rows, and removes the associated APK. Disk cleanup failures are logged; missing DB rows revoke downloads immediately.
- Existing version codes produce a clear validation error, including concurrent unique-key collisions. Deleting a draft/withdrawn version allows that code to be uploaded again.
- Add `file_name` through idempotent `sql/app_release_v120.sql` before deploying the backend.

## Chat interaction
- Separate taps from drag/scroll state. Scroll events update a plain observed position, never the scroll-top binding; this removes native scroll-command feedback during inertial movement.
- Replace the 32ms repeated layout polling with one coalesced content update and one post-expansion notification. Automatic layout events cannot disable bottom following.
- Explicit expansion reveals the selected thinking panel with a bounded native animated scroll, including historical messages. It does not jump to the end of the chat.
- Deduplicate keyboard height reports and remove competing frame/dock height animations. Refresh cached viewport geometry once per user gesture, not on each scroll event. Reset gesture state when leaving the page.

## Verification
- Java compilation and 15 H2 release regression assertions passed: filename persistence, published-delete rejection, withdrawn deletion/file removal, friendly duplicate handling and code reuse.
- APP syntax and 65 unit tests passed. Android resources/generated Kotlin/native code and iOS resource compilation checked; signed packages and physical iPhone frame-rate testing are not part of these checks.
- Browser streaming fixture: stable live message node, 284 frame samples, maximum transient clipping 160ms during expansion, final/latest gap -16px, manual upward scroll held.
- Browser touch/keyboard fixture: expanded panel within viewport; observed manual position 100px without changing command binding; keyboard open/duplicate callback/close all retain -16px bottom gap. Historical expansion remains 2353px from the latest message and fully visible.
- The final small gesture-geometry refresh and page-exit reset received syntax/native compile verification; browser fixtures exercised the preceding equivalent scrolling path.

## Deployment
Backend: `aimessage-backend:release-scroll-20260923-120`, healthy, zero restarts. Only AppReleaseService and AppReleaseController classes changed in the deployed jar.
Jar SHA256: `b4f8339c22309a54037fe393e96884a82897950a5bf1e4b6418188c65a467b8f`.
Web: `/var/www/aimessage/releases/release-scroll-20260923-120/dist`.
Index SHA256: `291d811f971118a7e928ef0330ea0b896bac44baabb585f208022da8f964b3cf`.
Backups: `/opt/aimessage/backups/pre-release-scroll-20260923-120`. Schema addition is backward compatible with the previous backend. Anonymous delete requests are rejected with 401. No real release was deleted or unpublished during verification.
Client changes require versionCode 120 to be rebuilt and installed.
