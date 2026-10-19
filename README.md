# IGRIS Mobile

[![Android CI](https://github.com/Abhiramrathod/igris-ai/actions/workflows/android.yml/badge.svg)](https://github.com/Abhiramrathod/igris-ai/actions/workflows/android.yml)

Native Android (Java) client for the **IGRIS** AI platform — a ChatGPT-style mobile chat with the same *"Shadow Monarch — Command Interface"* look as the [igris-ai](https://github.com/Abhiramrathod/igris-ai) web UI.

## Features

- Dark black/gold IGRIS command UI (timeline messages, processing bar, crown branding)
- Two connection modes:
  - **Direct LLM API** — calls an NVIDIA / OpenAI-compatible endpoint yourself (`/v1/chat/completions`)
  - **IGRIS Backend** — talks to your Spring Boot app (`POST /ai/chat`)
- In-app configuration: API key, base URL, model, temperature, max tokens, system prompt
- Model dropdown, auto-populated from the provider's `/models` endpoint
- Multi-turn conversation (full history sent to the model), saved locally
- New-chat reset, offline-safe error bubbles

## Requirements

- Android 7.0+ (minSdk 24)
- An LLM API key (e.g. NVIDIA `nvapi-…`) **or** a running igris-ai backend

---

## How to download

### Option 1 — GitHub Releases (recommended)

1. Open the repository on GitHub and go to **Releases**.
2. Download the latest `IgrisMobile-vX.Y.Z.apk`.
3. Transfer the APK to your phone (or download it directly on the phone).
4. On the phone: allow installs from your browser/file manager when prompted, then install.

### Option 2 — GitHub Actions artifact (every push)

1. Open the repo → **Actions** tab → select the latest **Android CI** run.
2. Under **Artifacts**, download `igris-mobile-debug-apk`.
3. Unzip — it contains `app-debug.apk` — and install it as above.

### Option 3 — Build it yourself

```bash
git clone https://github.com/Abhiramrathod/igris-ai.git
cd igris-ai   # or this project folder

# Debug APK (signed with the debug key, ready to install)
./gradlew assembleDebug
# → app/build/outputs/apk/debug/app-debug.apk
```

Windows: use `gradlew.bat` instead of `./gradlew`.

Or open the folder in **Android Studio** → **Run ▶** on a connected device/emulator.

---

## How to use

### 1. Install

Copy the APK to an Android phone and tap to install, or:

```bash
adb install app-debug.apk
```

### 2. Configure (first launch)

The app opens a **Configure IGRIS** dialog on first start — or tap the **⚙ gear** icon anytime.

| Field | Direct LLM API mode | IGRIS Backend mode |
|---|---|---|
| Connection Mode | Direct LLM API | IGRIS Backend |
| Base URL | `https://integrate.api.nvidia.com/v1` | `http://10.0.2.2:8080` (emulator) or `http://<your-server-ip>:8080` |
| API Key | your NVIDIA/OpenAI key | not required |
| Default Model | `mistralai/mistral-small-4-119b-2603` | model your backend accepts |
| Temperature / Max Tokens | `1.0` / `8192` | ignored by backend |
| System Prompt | persona for the model | ignored by backend |

Tap **Save Configuration**.

> **Testing the local backend from an emulator:** `10.0.2.2` is the host machine's loopback address. From a real phone, use your computer's LAN IP and make sure the backend allows that origin.

### 3. Chat

- Type a command in the input (`Enter your command…`) and press send or the keyboard's send action.
- Switch models from the dropdown in the composer.
- **＋** starts a new session (erases history).
- Messages persist locally — close and reopen the app to continue.

---

## CI/CD pipeline

`.github/workflows/android.yml` runs on every push/PR:

1. **Build Debug APK** — JDK 17 + Gradle, `./gradlew assembleDebug`, uploads the APK as an artifact (kept 30 days).
2. **Publish GitHub Release** — when you push a tag like `v1.0.0`, the release job attaches `IgrisMobile-v1.0.0.apk` to a GitHub Release automatically.

```bash
git tag v1.0.0
git push origin v1.0.0
```

---

## Project structure

```
app/src/main/java/org/abhi/aigris/mobile/
├── ui/
│   ├── MainActivity.java        # chat screen
│   ├── SettingsActivity.java    # API key / URL / model config
│   └── ChatAdapter.java         # message timeline + loading row
├── api/
│   └── LlmClient.java           # OkHttp client (direct LLM + /ai/chat modes)
├── data/
│   └── PrefsManager.java        # settings + chat history persistence
└── model/
    └── ChatMessage.java
```

## Security note

The API key is stored in plain `SharedPreferences` on the device. Don't ship a release build with a production key baked in — enter keys at runtime via the settings screen.

## License

Same license as the igris-ai repository.
