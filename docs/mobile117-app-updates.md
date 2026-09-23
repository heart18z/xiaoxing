# Version 117: Android application updates and login

## Usage

Administrators: 智能提醒 → APP版本管理. Upload a complete signed APK (maximum 300 MB), enter its exact manifest versionCode, version label and release notes. Upload creates a draft; Publish exposes it. Withdraw hides it. The highest published versionCode is offered. Version codes must be unique and increasing. Keep the installed package name com.dfyj.xiaoxing and signing certificate.

Android: 我的 settings icon shows a red dot when a newer version is found (checked on entry, throttled five minutes). Settings checks again and shows current version, release notes and install action. First grant the system permission to install apps from this source, return, then tap download/install again. Installation always requires the system confirmation. This is not a silent update; iOS continues to use TestFlight/App Store.

Existing installations must first install version 117 manually. Subsequent signed APKs can use this flow. No release has been uploaded/published during deployment.

Native checks: HTTPS download restricted to the configured service hosts; authentication included; redirects rejected; streamed download capped at 300 MB; exact size and SHA-256 checked; APK package/version/signatures compared with the installed app before launching the system installer. Only the verified cache APK is shared using a non-exported, read-only content provider with a temporary URI grant. Error/status feedback remains visible in Settings. No account secrets are logged.

## Login

Both confirmation buttons are equal flex widths and 48 px tall, with cancel padding removed. iOS portable SM2 encryption yields by elapsed 12 ms computation slices rather than approximately 320 fixed timer waits. Ciphertext arithmetic/layout unchanged. Logs contain only durations for encryption, OAuth and bootstrap; bootstrap authentication validation remains mandatory.

## Verification

- APP syntax checks and 62 tests passed, including SM2 interoperability and responsive encryption.
- Android resource build and all generated/native Kotlin compilation passed; iOS resource and Web builds passed. These are not signed APK/IPA builds.
- Backend compiled; isolated H2 release regression covers drafts, publication/withdrawal, downloads, SHA256, invalid APKs, duplicate versions and failed-upload cleanup (10 checks).
- Admin production build passed. Browser confirmation buttons each measure 152 x 48 at 390 px viewport; cancel makes no second login request.
- Desktop portable encryption comparison: previous 1158 ms, revised 783 ms, single sample; not a phone end-to-end latency measurement.
- Connected Android remains version 113. Version 117 installation/upgrade requires a signed APK and has not been tested on the phone.

## Deployment and rollback

SQL: sql/app_release_v117.sql (idempotent new table and admin menu grant).
Backend image: aimessage-backend:app-updates-20260923-117. Added exactly AppReleaseController and AppReleaseService class entries to previous live jar; existing classes/config/libraries retained. Multipart limits supplied through compose environment (300/301 MB).
Web release: /var/www/aimessage/releases/app-updates-20260923-117.
Backups: /opt/aimessage/backups/pre-app-updates-20260923-117 (compose, original nginx files, previous web symlink target). nginx increases upload allowance only at /api/blade-smart/admin/releases/upload; ordinary API limits retained. APK files persist in the existing uploads volume.
Backend rollback: python3 /opt/aimessage/deploy-release117.py rollback. Restore prior web symlink and backed-up nginx files, validate nginx before reload. Do not drop release data or restore the entire database. Backend health and anonymous access protection must be checked after rollout.
