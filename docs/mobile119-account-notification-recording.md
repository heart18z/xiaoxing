# Mobile 119: account forms, notification feedback and Android waveform

## Changes
- Registration requires email in APP, Web, Bean Validation and the account service. Phone stays optional. Contact inputs occupy separate full-width rows. Existing accounts and profile-edit rules are unchanged.
- Settings notification action shows the shared top tip immediately and reports the result afterwards; existing busy state and inline feedback remain.
- Reset-password popup uses a 410px preferred height (registration 640px), bounded by the viewport/keyboard. Footer stays reachable and content scrolls on smaller screens.
- Android replaces file-only recorder callbacks with one AudioRecord PCM stream for both real RMS levels and mono 16kHz, 16-bit WAV. The UI polls every 100ms; silence stays quiet. Stop finalizes the WAV before upload. Cancel/page exit suppress results and stop capture; recordings stop automatically at 60 seconds. Temporary WAVs expire after a day.

## Verification
- APP syntax and 65 unit tests passed; 6 Web account tests passed.
- Actual account service + Bean Validation H2 regression passed, including phone-only rejection and unchanged duplicate contact/password protection.
- Native PCM tests passed: silence, increasing amplitude, clipping bounds, correct WAV header and untouched PCM bytes.
- Android generated/native Kotlin check and iOS resource build checked separately. No signed APK/IPA was generated or installed; physical microphone waveform still needs verification with version 119.
- Playwright with local mocked APIs verified stacked registration fields, missing email feedback, top tip, password height 410px/button gap 58px, and visible 48px footer button at 360x540. Screenshots reviewed.

## Deployment
Backend image `aimessage-backend:account-feedback-20260923-119`, healthy with zero restarts. Scoped patch changes only Registration and AppAccountService classes; no data migration.
Jar SHA256: `33d5138c48c104eb6b938200c46b6636d49391d34b201184f2837ba14e36fa65`.
Web release `/var/www/aimessage/releases/account-feedback-20260923-119/dist`, index SHA256 `4bc5465ceb0de6f7753bde37dab678fa2d8807948bdbc0e621f61756a6e81480`. Atomic symlink switch preserves previous hashed assets. Backup in `/opt/aimessage/backups/pre-account-feedback-20260923-119`.
Public no-email registration validation returns 400 without creating an account.

## Platform references
- [Recorder compatibility](https://doc.dcloud.net.cn/uni-app-x/api/get-recorder-manager.html): Android does not supply onFrameRecorded callbacks.
- [Android permission API](https://doc.dcloud.net.cn/uni-app-x/uts/utsandroid.html#requestsystempermission).
