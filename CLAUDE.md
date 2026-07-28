# CLAUDE.md

Android prototype for recording and transcribing voice memos ("bayanat") of crime victims/witnesses in Pakistan, organized by FIR-numbered case. Fully offline: transcription runs on-device via whisper.cpp. UI is bilingual English/Urdu with RTL support.

## Build & run

```sh
./gradlew assembleDebug              # needs the model in assets first (see below) — APK ~220 MB
./gradlew testDebugUnitTest          # JVM tests (summarizer)
./gradlew connectedDebugAndroidTest  # Room tests; needs a running emulator/device
```

- Toolchain (installed, versions matter): AGP 8.13.0 / Gradle 8.14.1 / Kotlin 2.2.20 / KSP 2.2.20-2.0.2, **NDK 28.2.13676358**, **CMake 3.31.6** (3.22.x cannot parse NDK r28's Clang 19 — "No known features for CXX compiler"). Don't bump one without rechecking the matrix; avoid AGP 9.x (needs Gradle 9).
- Whisper model (git-ignored, required for the real backend):
  `curl -L -o app/src/main/assets/models/ggml-small-q5_1.bin https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-small-q5_1.bin`
- To build/demo **without** the model: bind `FakeTranscriber()` instead of `WhisperTranscriber(context)` in `AppContainer.kt` (one line; FakeTranscriber returns canned Urdu/English text after a 4 s delay).

## Architecture

MVVM + repositories, manual DI (no Hilt): `AppContainer` (built in `App.onCreate`) owns the DB, repositories, transcriber, summarizer, queue. ViewModels get it via `viewModelFactory` companions reading `APPLICATION_KEY`.

```
app/src/main/java/com/seekersofjustice/app/
├── domain/          Case/Memo models (enums stored by .name), Transcriber interface
├── data/db          Room: CaseEntity (unique firNumber index), MemoEntity (FK cascade),
│                    Flow DAOs; keyPoints stored as -joined string (Converters.kt)
├── data/repo        CaseRepository (typed DuplicateFir result), MemoRepository
├── transcription/   FakeTranscriber, WhisperTranscriber, ExtractiveSummarizer,
│                    TranscriptionQueue + TranscriptionWorker (WorkManager)
├── audio/           WavRecorder, AudioImporter (SAF), AudioDecoder, MemoPlayer
└── ui/              4 screens (cases, casedetail, record, memo) + navigation + theme

whisper/             Android library: CMake build of vendored whisper.cpp + jni.c +
                     WhisperContext (all native calls on one dedicated thread)
third_party/whisper.cpp   pruned v1.8.2 snapshot (see VENDORED_VERSION); not a submodule
```

Key invariants:

- **Audio format**: everything decodes to 16 kHz mono float32 PCM (`AudioDecoder`); `WavRecorder` records at 16 kHz directly so recordings never need resampling. Imports accept WAV/FLAC only (magic-byte sniff), copied into `filesDir/audio/`.
- **Transcription pipeline**: memos are enqueued as WorkManager *unique work* (`APPEND_OR_REPLACE`) → strictly sequential, one Whisper context user at a time. Worker: PROCESSING → decode → transcribe → (if effective language is Urdu) second translate pass → summarize → DONE, all statuses via Room so UI reacts through Flows. Failures set FAILED + message (UI offers Retry); worker never returns failure. `App.onCreate` resets stuck PROCESSING → PENDING and re-enqueues (process-death recovery).
- **Model loading**: `WhisperTranscriber` copies the asset to `filesDir/models/` on first use (whisper reads a plain path), loads once per process, `nThreads = cores−2 (2..6)`.
- **Summarizer**: TF sentence ranking, English + Urdu stopword sets, ICU sentence split with regex fallback; **falls back to Whisper segments when < 3 sentences** — load-bearing, Whisper's Urdu output is punctuation-sparse.
- **Localization**: every user-visible string is in `values/strings.xml` + `values-ur/strings.xml` (enum labels via `ui/common/UiText.kt`). Language toggle uses `AppCompatDelegate.setApplicationLocales` — that's why `MainActivity` is an `AppCompatActivity`. Urdu typography uses bundled Noto Nastaliq with ~2× line heights (`ui/theme/Type.kt`) or the script clips. Use start/end, never left/right; wrap LTR fragments shown in RTL text in `⁦…⁩` isolates (see the player time label).

## Testing / verification

- Emulator: AVD `Medium_Phone_API_36.1` (config `~/.android/avd/Medium_Phone.avd/config.ini`) — must have ≥4 cores / 4 GB RAM (1-core/2 GB default causes ANRs and dropped input) and ≥8 GB data partition (app ~250 MB + model copy 190 MB + install staging; `INSTALL_FAILED_INSUFFICIENT_STORAGE` otherwise → `adb shell pm trim-caches 4G` or grow `disk.dataPartition.size` + wipe).
  Boot headless: `emulator -avd Medium_Phone_API_36.1 -no-window -no-audio -no-boot-anim -gpu swiftshader_indirect`
- Known-good English check: import whisper.cpp's `samples/jfk.wav` with language=English → canonical JFK transcript.
- Urdu without a speaker: `say -v Lekha -o test.wav --data-format=LEI16@16000 "<Hindi text>"` then import with language=Urdu (spoken Hindi ≈ spoken Urdu; Whisper outputs Urdu script).
- Perf (arm64 emulator, 2 threads): ~4.5× realtime per pass; Urdu memos run two passes. First transcription ever adds ~30 s (model copy + load).
- The mic on a headless emulator records silence — use file import for content-dependent tests.

## Gotchas learned the hard way

- WorkManager foreground workers crash without `androidx.work.impl.foreground.SystemForegroundService` declared in **our** manifest with `android:foregroundServiceType="dataSync"` + `tools:node="merge"`.
- `MediaMetadataRetriever.close()` is API 29+; minSdk is 26 → use `release()` in try/finally, not `.use {}`.
- If Stage-3-style native build fails oddly under the configuration cache, try `--no-configuration-cache` first.
- `noCompress += "bin"` in `app/build.gradle.kts` keeps the model uncompressed in the APK — required.
- Never commit `app/src/main/assets/models/` (190 MB, git-ignored).
