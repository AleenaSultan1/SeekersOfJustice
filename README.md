# Seekers Of Justice

Mobile application that can be used in Pakistan to record the experiential history of criminal victims both during investigations and during court hearings and trials.

This repository contains a working Android prototype: voice memos are organized by FIR-numbered case and transcribed **fully offline, on-device** in Urdu or English, with an English translation for Urdu memos and five automatically extracted key points per memo. No account, no server, no network needed after setup.

## What the app does

1. **Cases** — list of cases by FIR number. Add a case with the FIR number (free text, must be unique) and the person's details: full name, age, sex, relation to the crime (victim / witness / relative / other).
2. **Case detail** — the person's summary and all voice memos for that case, each with a live status chip (Waiting → Processing → Done, or Failed with a Retry button).
3. **New memo** — record in-app (16 kHz WAV) or import an existing WAV/FLAC file, and choose the memo's language: Urdu, English, or Auto-detect.
4. **Memo view** — play the audio, read the transcript, the English translation (for Urdu memos), and 5 key points.

The whole UI is available in **English and Urdu** (RTL layout, Noto Nastaliq Urdu typeface); toggle with the globe icon on the Cases screen. Transcription runs as a background job and survives app restarts — interrupted memos are picked up again on next launch.

## How it works

- **Stack**: Kotlin, Jetpack Compose, MVVM + repositories, Room, WorkManager, Navigation-Compose.
- **Transcription**: [whisper.cpp](https://github.com/ggml-org/whisper.cpp) (vendored at `third_party/whisper.cpp`, see `VENDORED_VERSION`) running the quantized Whisper `small` model bundled in the APK. The `:whisper` module wraps it behind a small JNI surface. Urdu memos get a second Whisper pass that translates to English.
- **Key points**: extractive summarization (TF-based sentence ranking with Urdu + English stopword lists) — no LLM, no network.
- **Swappable backend**: everything above sits behind the `Transcriber` interface. `AppContainer` binds `WhisperTranscriber`; swap in `FakeTranscriber` (canned text) to develop or demo without the model.

Deeper architecture notes, invariants, and troubleshooting live in [CLAUDE.md](CLAUDE.md).

## Building

Prerequisites:

1. Android SDK with **NDK 28.2.13676358** and **CMake 3.31.6**:
   `sdkmanager "ndk;28.2.13676358" "cmake;3.31.6"`
2. The Whisper model (git-ignored, ~190 MB) placed in assets:

   ```sh
   mkdir -p app/src/main/assets/models
   curl -L -o app/src/main/assets/models/ggml-small-q5_1.bin \
     https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-small-q5_1.bin
   ```

Then:

```sh
./gradlew assembleDebug              # debug APK, ~220 MB (model bundled for offline use)
./gradlew testDebugUnitTest          # JVM tests
./gradlew connectedDebugAndroidTest  # Room tests (needs a running emulator/device)
```

To build without downloading the model, bind `FakeTranscriber()` in
`app/src/main/java/com/seekersofjustice/app/AppContainer.kt`.

## Performance expectations

On an arm64 emulator (4 cores / 4 GB, 2 whisper threads) the `small-q5_1` model runs at roughly **4.5× realtime** per pass; a 19 s Urdu memo (transcribe + translate) takes ~3 minutes. The first transcription after install adds a one-time ~30 s model copy + load. Real phones with more usable cores should be faster; for snappier demos a `ggml-base-q5_1` (~60 MB) drop-in is possible at some accuracy cost.

## Emulator notes

- Give the AVD at least 4 CPU cores / 4 GB RAM and an 8 GB data partition; the stock 1-core/2 GB config thrashes and drops input.
- If installs fail with `INSTALL_FAILED_INSUFFICIENT_STORAGE`: `adb shell pm trim-caches 4G`, or raise `disk.dataPartition.size` in the AVD's `config.ini` (requires a data wipe, and the host needs ~1.2× that free on disk).
- Known-good smoke test: import `third_party`-era whisper.cpp `samples/jfk.wav` as an English memo → the canonical JFK quote.
