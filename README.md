# NEGATIVE — "Tracking your money won't bring it back."

Native Android app (Kotlin + Jetpack Compose). Fully offline; data stored locally (SharedPreferences).

## Get the APK (no Android Studio needed)
1. Create an empty GitHub repo, upload/push this whole folder (keep the `.github` folder).
2. Open the repo's **Actions** tab -> **Build APK** run. It runs the unit tests, then builds.
3. Download the **NEGATIVE-debug-apk** artifact, unzip, install `app-debug.apk` (allow "install unknown apps").

## Or build locally
Open the folder in Android Studio and press Run, or `gradle assembleDebug` (Gradle 8.9, JDK 17).

## Layout
- `Engine.kt`  all Broke Date math (pure Kotlin, unit-tested)
- `Roasts.kt`  humor text (MILD / SARCASTIC / NO MERCY)
- `Store.kt`   local persistence
- `App.kt`     screens (Home, Evidence, Settings, Setup)
- `src/test`   unit tests for dates, ₱0 cases, negatives, edit/delete, NaN safety

## Rules baked in
Target today = (money at start of day) / max(days until Broke Date, 1). Broke Date today or past with money left => emergency: spend it all.
