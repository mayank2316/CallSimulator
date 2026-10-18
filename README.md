# Call Simulator

A native Android app that locally simulates incoming and in-call screens. It **never places or receives a real phone call** and does not use the microphone, cellular calling APIs, SMS, or network services.

## Features
- Choose a person with Android's native contact picker, or enter a display name manually.
- Start a simulation immediately or schedule it with date/time pickers.
- Multiple scheduled calls, local persistence, cancellation, and deletion.
- System ringtone picker, preview, volume, vibration and silent mode.
- Incoming-call screen with accept/reject actions and a simulated in-call screen.
- Visual-only mute, speaker, keypad, hold and add-call controls.
- Notifications for scheduled calls; alarm schedules are restored after reboot when permitted.
- Notification and exact-alarm limitations are handled with user-facing guidance.

## Build
1. Install Android Studio (current stable) and Android SDK Platform 35.
2. Open this folder in Android Studio and allow Gradle sync.
3. Run the `app` configuration on an Android 8.0+ device/emulator.
4. To create a debug APK: **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
5. APK output: `app/build/outputs/apk/debug/app-debug.apk`.

Or run `./gradlew assembleDebug` after adding the Gradle wrapper in Android Studio, or use the included GitHub Actions workflow to build an APK.

## Android behavior notes
- Android 13+ requires notification permission for notifications.
- Exact alarms may require special access on newer Android versions. If unavailable, the app uses an inexact alarm where possible and explains that timing may vary.
- Android may not permit launching an activity directly from the background. When that happens, the app posts a notification that the user taps to open the simulated incoming-call screen.
- Reboot recovery is best-effort and subject to Android background execution and exact-alarm restrictions.
- The system contact picker is used, so broad contacts permission is not needed.
- All controls are visual simulation only; no microphone permission is requested and no real call APIs are used.

## Privacy
Contact details and scheduled call data are stored locally on the device. The app has no analytics or network transmission of contact data.
