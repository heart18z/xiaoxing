# Account access and chat image fixes — 1.0.130

## User-visible changes

- Password login accepts an account name or normalized email. Email-code login follows the same account, client and device-confirmation policy.
- Registration and password recovery are independent pages. Registration keeps contact uniqueness checks and automatic login, and requires the user to choose a password.
- Password recovery verifies an email code and revokes existing access and refresh token indices.
- Settings saves language independently of model preferences. A removed model no longer blocks a language change.
- Chat history serializes ID arrays as strings. This fixes file IDs above JavaScript's safe integer range becoming invalid after history refresh.
- The composer orb uses its complete round PNG without a native image-layer shadow. Login confirmation actions share the same component and equal-width containers.
- Account closure requires two confirmations and the current password. It disables the account, releases its email/phone, removes personal chats, friendships, preferences and push bindings, and stops affected reminder branches. Shared event history remains for other participants. The client clears its session and local alarm owner. In-flight conversations must finish or stop before closure.

## Verification

- Existing 78 client tests passed; two additional email-login request tests passed (17 request tests in total).
- AccountAccessRegression: real JDBC transactions with isolated mail/token delivery; purpose isolation, invalid attempts, confirmation rollback, one-time consumption, password reset and revocation, closure scoping, language independence, 64-bit IDs and controller parameter metadata.
- RegistrationMailRegression passed, including concurrent registration, uniqueness and expiration.
- Browser checks: standalone navigation, email-code login cancellation, two-stage closure, language-only request with a stale model, and equal 158×48px confirmation buttons at 402px viewport. No page errors.
- HBuilderX 5.24: iOS and Android resource builds passed. Generated Android pages, shared Kotlin and native Android implementation passed the full local type check. iOS bridge lifetime checks passed.
- Production temporary-account checks passed: email/password, code login and confirmation, code replay rejection, language save, exact file IDs in chat history, email reset, previous-token rejection, closure authorization and second confirmation, closed-account rejection. Test records were removed. No test email was sent.

## Deployment and packaging

Backend image: `aimessage-backend:account-access-20261013`.
Release: `/opt/aimessage/releases/account-access-20261013`.
Rollback configuration: `/opt/aimessage/backups/pre-account-access-20261013`.

Deployment patches the exact previous JAR, preserving unrelated classes, dependencies, SMTP and model settings. The migration only adds `blade_account_code`; registration codes remain separate. Explicit public endpoint allowlists cover code sending, email login and password recovery. Account closure remains authenticated.

The first production smoke check exposed missing Java parameter metadata in the scoped compilation. That artifact was rolled back, rebuilt with `-parameters`, checked by regression, and replaced. The final backend is healthy and passed all checks above.

The app manifest is **1.0.130 / 130**. Build new signed Android and iOS packages with HBuilderX to receive the new pages and settings actions. Local resource/type checks do not produce a signed APK/IPA or prove real-device rendering. The image-ID correction is already active server-side.

## Reproduce checks

From the repository root, with the existing test dependency classpath and H2/Jakarta Mail jars:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/run-account-access-regression.ps1
```

From `code/APP`, run `node --test tests/*.test.mjs` and `node tests/check-project.mjs`. After native resource builds, run `node tests/check-ios-generated.mjs` and `tests/check-android-generated.ps1`.

Browser checks use the existing Playwright CLI fixture, then `tests/visual/account130.js` and `tests/visual/account130-language.js`. All API calls are intercepted in that isolated browser session.
