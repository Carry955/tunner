# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**音准器** (Pitch Tuner) is an Android application that detects vocal pitch in real-time and displays it on a piano roll visualization. Built with Kotlin, Jetpack Compose, and the YIN pitch detection algorithm.

## Build System

- **Gradle** 9.4.1 with **Kotlin DSL** (`.kts` files)
- **Android Gradle Plugin (AGP)** 9.2.1
- **Kotlin** 2.2.10
- **JVM toolchain**: Java 21 via foojay-resolver-convention (source compatibility: Java 11)
- **Version catalog**: `gradle/libs.versions.toml` — all dependency versions centralized here
- **Configuration cache** enabled (`gradle.properties`)
- **No ProGuard/R8** — optimization explicitly disabled in release builds

## Common Commands

```bash
# Build
./gradlew assembleDebug          # Debug APK (use .\gradlew on Windows)
./gradlew assembleRelease        # Release APK
./gradlew build                  # Full build (includes lint)

# Test
./gradlew test                   # All unit tests (JVM)
./gradlew testDebugUnitTest      # Debug unit tests only
./gradlew test --tests "*Note*"  # Run a single test class by pattern
./gradlew connectedAndroidTest   # Instrumented tests (requires device/emulator)

# Install & Run
./gradlew installDebug           # Install debug APK on connected device

# Clean
./gradlew clean
```

## Architecture

Single-module project (`:app`, package `com.carry.tunner`) using MVVM pattern with Jetpack Compose.

```
app/src/main/java/com/carry/tunner/
├── MainActivity.kt              -- Entry point, handles RECORD_AUDIO permission
├── audio/
│   ├── AudioCapture.kt          -- Microphone PCM capture via AudioRecord
│   └── PitchDetector.kt         -- YIN algorithm (autocorrelation-based) pitch detection
├── model/
│   ├── Note.kt                  -- Musical note (name, octave, frequency, MIDI#, cents deviation)
│   └── PitchData.kt             -- Single pitch detection result data point
├── ui/
│   ├── theme/                   -- Material3 custom theme (blue/gray palette, dark/light)
│   ├── PianoRollView.kt         -- Canvas-based piano roll with vertical pinch-to-zoom
│   └── TunerScreen.kt           -- Main screen: note display + piano roll + control bar
└── viewmodel/
    └── TunerViewModel.kt        -- State management, noise gate, audio processing pipeline
```

### Data Flow

1. `AudioCapture` reads PCM samples (2048 frames at 44100Hz, mono, 16-bit) via `AudioRecord` on `Dispatchers.IO`
2. Short-to-float conversion (`/32768.0f`) and callback to `TunerViewModel.processAudioData()`
3. ViewModel calculates RMS amplitude → noise gate check (default threshold 0.02)
4. `PitchDetector.detect()` runs YIN algorithm → frequency or null (range: 80-1100Hz)
5. `Note.fromFrequency()` converts frequency to musical note using A4=440Hz reference
6. `PitchData` appended to history list (max ~1800 entries, ~30 seconds)
7. UI observes `StateFlow`s and renders via `PianoRollView` Canvas (shows last 5 seconds)

### Audio Processing Notes

- **Processing rate**: ~21.5 frames/sec at 44100Hz with 2048-sample frames
- **Noise gate**: Configurable 0.0-1.0 slider in UI (default 0.02), below-threshold frames are marked as silence
- **YIN algorithm steps**: difference function → cumulative mean normalization → absolute threshold (0.15) → parabolic interpolation for sub-sample precision
- **Cents deviation**: Color-coded in UI — green (<5¢), amber (<15¢), red (≥15¢)

### Key Technical Details

- **Compile SDK**: 36 (minorApiLevel 1) | **Min SDK**: 26 | **Target SDK**: 36
- **UI**: Jetpack Compose with Material3 (BOM 2026.02.01), custom blue/gray palette (dynamic color disabled by default)
- **Piano Roll**: Canvas drawing, MIDI range C2-C6 (36-84), shows 5-second rolling window, supports vertical pinch-to-zoom (0.5x-3x)
- **No DI framework** — ViewModel uses `AndroidViewModel` for Application context
- **Tests**: Placeholder only — `ExampleUnitTest` (JUnit 4) and `ExampleInstrumentedTest` (AndroidJUnit4/Espresso) are not actual tests
- **No CI/CD** — no GitHub Actions, GitLab CI, or other pipeline configs exist
