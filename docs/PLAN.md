## Plan: Android Compose Keyboard Development

An Android keyboard is an `InputMethodService`, not a standard app `Activity`. The current workspace successfully bridges the `InputMethodService` lifecycle with Jetpack Compose.

**Steps**

1. **Enable and Run the Keyboard on Device**
   - Build and install the app on the device.
   - Go to System Settings -> Languages & input -> On-screen keyboard -> Manage on-screen keyboards.
   - Enable `KeyboardService` (the name will be your app name).
   - In any app with a text field, tap the field. Use the keyboard switcher at the bottom right of the navigation bar to select the custom keyboard.
   - *Verification:* The Compose keyboard (`KeyboardScreen`) shows up and keys type text.

2. **InputConnection Management Phase (Next)**
   - Expand `KeyboardScreen` to handle shift states, numbers, and special characters.
   - Integrate `currentInputConnection?.sendKeyEvent(...)` for action keys like Enter/Search or Delete edge cases.

3. **App Setting Phase**
   - Enhance the `MainActivity` to guide the user to enable the keyboard (using Intents) and pick settings (themes, layouts).

**Relevant files**
- `app/src/main/java/com/example/keyboard/KeyboardService.kt` — Service entry point, manages `InputConnection`.
- `app/src/main/java/com/example/keyboard/MainActivity.kt` — Setup screen.
- `app/src/main/AndroidManifest.xml` — Defines the IME service and settings Activity.

**Decisions**
- Use Jetpack Compose for the keyboard UI, bridging with `LifecycleOwner` and `SavedStateRegistryOwner` (already set up).

**Further Considerations**
1. Would you like to add a setup wizard in `MainActivity` to automatically open the Android keyboard settings for the user?
2. What specific features (e.g., emojis, predictive text) are you planning for the keyboard layout next?