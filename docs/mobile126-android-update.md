# Android update installation correction (126)

## Evidence on 2026-09-23

- Published release 125 APK SHA-256: `3419036086f3a39a2c3598df166a0ee1951bc9a33b62aadb854bb5a3e34aa8a5`.
- `aapt2 dump badging` reports package `com.dfyj.xiaoxing`, versionCode `125`, versionName `1.0.1`. The admin label was `1.0.2`; changing this label does not change APK metadata.
- The connected phone initially reported code 118. Download/install from Settings reproduced the OEM “same version” dialog. After tapping reinstall and continue, the installer reported completion, but PackageManager still reported 118 with an updated installation timestamp. This was an actual old-version reinstall, not merely a stale update badge.
- Installing the exact verified release 125 via `adb install -r` succeeded without clearing app data. PackageManager then reported 125, and the app Settings showed `当前版本 1.0.1 (125)` and `当前已是最新版本`. The existing session remained usable.

## Change

The updater formerly reused both `cache/xiaoxing-update.apk` and `content://…/update.apk` for every release. OEM installer reuse of this shared identity is the suspected source of the stale install; the OEM's private cache was not directly inspected.

- Each download/installation attempt now has a unique filename and URI incorporating code, SHA-256 and a UUID. Retries cannot overwrite an APK already handed to the installer.
- The read-only, non-exported provider exposes the exact file's display name and size and grants temporary URI access through Intent/ClipData. Path validation rejects the legacy shared URI, traversal, other files and missing APKs.
- HTTPS host restrictions, download size/hash, package name, newer version and signing certificate checks remain required before launching installation.
- Store the expected installation version, then compare it with PackageManager on returning to Settings. An incomplete installation explicitly says the old version remains; reaching the expected version clears the pending marker.
- Next package: versionName `1.0.126`, versionCode `126`. Enter these exact values in APP version management after building the signed APK with the existing package and certificate.

## Verification and rollout

- Android resource build, full generated/native Kotlin compilation, APP syntax checks and 70 existing tests.
- `tests/UpdateFilesRegression.kt` exercises separate version/retry URIs, preservation of old handed-off file content, legacy/path traversal rejection, missing files and invalid version metadata (7 checks).
- The phone was restored to signed production version 125. It does **not** yet contain the 126 native updater fix. A signed 126 package must be rebuilt; Windows resource/Kotlin checks are not a signed APK build.
- For other devices trapped in the old installer flow, install the new signed package once through a fresh system Files/Downloads location or USB, preserving app data. Do not uninstall or clear user data. Once 126 is installed, future in-app update attempts use distinct URIs.
- Backend records and existing APKs were not modified or withdrawn during this fix.

References: [Android versionCode/versionName](https://developer.android.com/studio/publish/versioning), [OpenableColumns](https://developer.android.com/reference/android/provider/OpenableColumns).
