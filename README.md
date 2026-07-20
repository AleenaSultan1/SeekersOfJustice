# Seekers Of Justice

Mobile application that can be used in Pakistan to record the experiential history of criminal victims both during investigations and during court hearings and trials.

## Prototype overview

Android app (Jetpack Compose, MVVM + repositories, Room) that organizes voice memos by FIR-numbered case and transcribes them **fully on-device** with [whisper.cpp](https://github.com/ggml-org/whisper.cpp) (quantized Whisper `small`, Urdu + English). Urdu memos also get an English translation. Five key points are extracted per memo with TF-based extractive summarization (no LLM). Full English/Urdu localization with RTL and a bundled Noto Nastaliq Urdu font.

### Modules

- `:app` — UI, Room data layer, transcription pipeline (WorkManager), summarizer
- `:whisper` — Android library wrapping vendored whisper.cpp (`third_party/whisper.cpp`, see `VENDORED_VERSION`) behind a small JNI surface
- The transcription backend is swappable via the `Transcriber` interface — `AppContainer` binds either `WhisperTranscriber` (real) or `FakeTranscriber` (canned text, no model needed)

### Build prerequisites

1. Android SDK with **NDK 28.2.13676358** and **CMake 3.31.6** (`sdkmanager "ndk;28.2.13676358" "cmake;3.31.6"`)
2. Download the Whisper model into assets (git-ignored, ~190 MB):

   ```sh
   mkdir -p app/src/main/assets/models
   curl -L -o app/src/main/assets/models/ggml-small-q5_1.bin \
     https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-small-q5_1.bin
   ```

3. `./gradlew assembleDebug` — debug APK is ~220 MB (model bundled for offline-out-of-the-box).

To build without the model, flip `AppContainer.transcriber` back to `FakeTranscriber()`.

### Observed performance (arm64 emulator, 4 cores / 4 GB, 2 whisper threads)

- First transcription adds a one-time model copy from assets + load (~30 s total overhead)
- Whisper `small-q5_1` runs at roughly **4.5× realtime** per pass (19 s Urdu clip: ~88 s transcribe + ~85 s translate)
- English memos need one pass; Urdu memos run a second translate-to-English pass

Expect faster on a real mid-range phone (more usable threads); the summarizer falls back to Whisper segments when Urdu output lacks sentence punctuation.

### Emulator storage note

App install (~250 MB) plus the on-first-run model copy (~190 MB) needs ≳700 MB free on `/data`. If installs fail with `INSTALL_FAILED_INSUFFICIENT_STORAGE`, either `adb shell pm trim-caches 4G` or raise `disk.dataPartition.size` in the AVD's `config.ini` (requires a data wipe).
