# Android batch speech recognition — 2026-09-23

The Android path now records mono 16 kHz PCM WAV, stops on confirmation, uploads once to
`/app/reminder/audio/transcriptions`, and inserts the returned text into the draft.
It does not send a chat message. Keep this batch flow; no realtime provider change.

## Server-side measurements

Direct requests from the application server to the configured
`FunAudioLLM/SenseVoiceSmall` provider, using synthetic fixture audio only:

| Probe | Result | Duration | Provider trace |
| --- | --- | --- | --- |
| Connectivity, unauthenticated models endpoint | Expected HTTP 401 | 0.083 s | — |
| WAV, 175 KB multipart | HTTP 200, nonempty transcript | 18.087 s | `ti_1g8d0aefsehpumycbj` |
| MP3, 68 KB multipart | HTTP 200, nonempty transcript | 16.480 s | `ti_jd2fijfl4o53371ql8` |
| MP3, repeat using Python HTTP client | HTTP 200, nonempty transcript | 30.993 s | `ti_l9vkzykiud6pwpzxqn` |

Earlier probes hit client timeouts at 30 s (WAV) and 100 s (MP3), before a response
was available. The successful probes demonstrate substantial upstream latency
without involving Android or the application's upload endpoint. They do not
prove the exact breakdown of the user's one-minute wait or that AAC is slower.
No model settings, credentials, or live user data were changed during testing.

## App diagnostics in version 115

The recording UI separates uploading from waiting for recognition. Android logs
`[speech]` timings for stop-to-callback, upload completion, and response arrival.
The upload-complete event is the client's byte-transfer report, not proof that
the model has started inference. Logs contain no recording path, audio, token,
or transcript. A canceled/stale result cannot overwrite another draft or show
an obsolete error toast.

Rebuild and install the Android package, record a short phrase, and tap
“结束录音”. Observe the stage text and collect only `[speech]` logs. Compare
the wait with provider traces before changing encoding, transport, or models.
Version 115 improves visibility but does not claim to fix provider latency.

## Sealos comparison and configuration switch

At the user's request, tested `https://aiproxy.hzh.sealos.run/v1/audio/transcriptions`
with model `SenseVoiceSmall`, using the same synthetic fixtures. Successful
responses all contained a nonempty 20-character transcript:

| Format | Duration | Result |
| --- | --- | --- |
| MP3 | 16.323 s | HTTP 200 |
| WAV | 0.840 s | HTTP 200 |
| MP3 repeat | 7.022 s | HTTP 200 |
| AAC, mono 16 kHz / 48 kbps | 45.001 s | Client timeout |
| AAC repeat | 18.605 s | HTTP 200 |

The system-default speech configuration was conditionally switched to Sealos
after compatibility checks. The key is encrypted using the existing secret
codec format. The old encrypted row is backed up on the server at
`/opt/aimessage/backups/speech-before-sealos-20260923-093318.json` (restricted
permissions). No application restart is needed: speech configuration is read
per request. Personal provider overrides are unchanged. The new provider has
also shown variable latency; phone confirmation-to-text timing remains to be
verified. Do not promise subsecond recognition based on one WAV result.

The user subsequently enabled the new provider through the admin UI and asked
to use WAV. Version 115 now requests `format: 'wav'` from the Android recorder,
with 16 kHz mono input and the existing 60-second maximum. The installed
HBuilderX Android runtime was inspected: its WAV path writes a RIFF/WAVE header,
16-bit PCM samples, and finalizes the data size on stop. It is not an extension
rename. No server configuration was overwritten after the user's admin change.
This recording-format change needs a rebuilt Android package; existing installs
continue to record AAC. A one-minute PCM recording is approximately 1.92 MB.
A subsequent WAV probe against the currently configured provider also hit a
45-second client timeout. WAV is therefore not a demonstrated cure for all
latency; provider response times remain variable.
