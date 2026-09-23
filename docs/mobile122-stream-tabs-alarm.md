# Mobile 122: iOS reasoning stream, keyboard transition and Android alarm controls

## Changes
- iOS observes SSE with a dedicated URLSessionDataDelegate through a multi-shot native callback. It validates HTTPS/status/MIME, refuses redirects, buffers UTF-8 through complete newline boundaries, and cancels observation on hide without cancelling or resubmitting the server job. Android native SSE remains in place; Web retains the request path.
- ThinkingPanel keeps one reveal clock during successive snapshots. Previously snapshots arriving more often than 32 ms could perpetually restart the timer. Waiting-stage text is replaced as soon as actual reasoning exists.
- Android tabs use a shared physical navigation-bar inset from WindowInsets and fixed native frame edges, rather than capturing different page viewport safe areas before/after native tab-bar hiding.
- iOS account popups animate actual height over 220 ms. Their outer maximum height stays stable so it does not clip the transition immediately as the keyboard appears.
- Android ringing notifications open an explicit alarm Activity with Stop and View Event. Stop is bound to the exact alarm identity, stops audio and reports that the alarm was closed. The page observes ringing/ended state. Full-screen presentation is requested only when allowed; tapping the notification still opens controls when full-screen access is unavailable. The activity is not exported and checks the account before displaying event data.

## Validation and limits
- APP syntax check; 67 existing unit tests plus a new continuous-snapshot display regression passed.
- Final Android resources, all generated Kotlin pages and native Kotlin implementations compile.
- iOS resource compilation passed. Windows cannot run Xcode/Swift native linking; the new Swift transport still requires cloud packaging and an iPhone streaming test.
- Browser fixture: 284 stream-layout samples, stable live message node, final bottom gap -16 px, no JS errors. Manual scroll/expanded thinking/keyboard follow regression passed. These are browser checks, not proof of native frame rates.
- Connected Android was detected, but no signed 122 package was generated or installed. Alarm ringing/notification controls and cross-tab height need verification with that package. No real reminders were sent and no production backend/web changes are needed.

Reference APIs checked: https://doc.dcloud.net.cn/uni-app-x/api/request and https://developer.android.com/about/versions/14/behavior-changes-14 (full-screen alarm intent access).
