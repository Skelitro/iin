# SpellBee AI — mobile + Android build

This project contains two coordinated parts:

- `src/`: the redesigned, touch-friendly React/Vite web interface.
- `app/`: the real native Android project that provides the floating overlay and offline Vosk speech recognition.

## What was fixed

The UI was rebuilt mobile-first with a dark ink / honey palette, rounded bento-style cards, liquid-glow backgrounds, stronger typography, touch-sized controls, and a compact floating assistant. The redesign takes visual cues from the supplied Dribbble references: warm bee-yellow accents, bold editorial type, modular cards, and soft blue/neutral secondary surfaces. The web preview remains responsive; the actual Android bubble is native.

The speech recognition lifecycle was also tightened so the web recognizer does not keep restarting after the user stops it.

The native project is now materialized as real Gradle/Kotlin/XML files instead of living only as generated source text. Android 14 microphone foreground-service requirements are handled by promoting the service with the microphone foreground-service type after the runtime microphone permission has been granted.

## Build the web preview

Requires Node.js.

```bash
npm install
npm run dev
```

Set `VITE_GEMINI_API_KEY` in `.env.local` to enable Ultra AI suggestions in the browser.

## Build the Android APK

### Easiest method: GitHub Actions

1. Put this project in a GitHub repository.
2. Push to `main` or `master`.
3. Open **Actions → Build SpellBee AI APK → Run workflow**.
4. Download the generated `SpellBee-AI-debug` artifact.
5. Inside that artifact is `app-debug.apk`.
6. Copy the APK to your Android phone and open it to install. Android may ask you to allow installation from that source.

The workflow downloads the small English Vosk model during the build, then packages it into the APK as `model-en-us`.

### Android Studio

Open the project root in Android Studio. Let Gradle sync, then run/build the `app` module.

A local build needs internet access the first time so Gradle can fetch Android/Jetpack/Vosk dependencies and the Vosk model. The project uses Vosk 0.3.32 from Maven Central.

## Important Android permissions

The native overlay asks for:

- microphone permission
- “display over other apps” / overlay permission

The overlay is started from the visible activity after those permissions are granted. This is important on Android 12+ and especially Android 14+, where foreground microphone services are subject to background-start and permission restrictions.

## About file-to-APK converters

A generic HTML-to-APK converter can package the **web UI**, but it cannot reproduce the native floating WindowManager bubble and offline Vosk service correctly. For the full functionality in this project, use the included Android project/GitHub Actions build.

## Visual direction

The redesign blends:
- honey/bee warmth inspired by the Bees & Hops palette
- bold modular/bento composition inspired by Easy Bio
- restrained dark neutral / blue secondary surfaces inspired by Dentore
- soft “liquid” glow treatment for depth and motion

The references were used as visual inspiration, not copied as exact layouts or assets.
