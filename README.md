# Athena Android Client

A minimalistic Android voice assistant client for Athena. Speak to the app, and it sends your prompt to the Athena server, displays the markdown-formatted response, and plays back the audio.

## Features

- Voice input via Android SpeechRecognizer
- Voice selection dropdown (re-queries available voices on each click)
- "None" option for text-only responses (no audio)
- Markdown-formatted text responses
- Auto-plays audio when response arrives
- Replay button for each response with audio
- Animated "Thinking..." indicator while waiting
- Keeps screen awake during loading and audio playback
- Fallback URL support with automatic health checks
- Memory-only conversation history (cleared on app restart)
- Portrait orientation only
- Dark theme by default

## Requirements

- Docker (for building)
- Android device or emulator (API 26+)
- ADB (for installing)

## Setup

### 1. Clone the repository

```bash
git clone <repository-url>
cd athena-android-client
```

### 2. Configure API credentials

Create a `local.properties` file in the project root with your Athena server details:

```properties
api.url=https://your-athena-server.com
api.url.fallback=http://your-internal-server.local
api.token=your-auth-token
```

The fallback URL is optional - if the primary URL health check fails, the app will automatically switch to the fallback.

> **Security Note**: The `local.properties` file is gitignored and should never be committed. The API credentials are baked into the APK at build time. This approach is suitable for personal use only.

### 3. Build the app

```bash
# Build debug APK (uses Docker, no Java required)
make build

# Or build release APK
make release
```

### 4. Install on device

```bash
# Install debug build on connected device
make install
```

## Build Commands

| Command | Description |
|---------|-------------|
| `make build` | Build debug APK using Docker |
| `make debug` | Build debug APK using Docker |
| `make release` | Build release APK using Docker |
| `make install` | Install debug APK on connected device (requires ADB) |
| `make uninstall` | Uninstall app from connected device |
| `make clean` | Clean build artifacts |
| `make docker-clean` | Remove Docker build image |
| `make icons` | Regenerate app icons from logo.png |

## Project Structure

```
athena-android-client/
├── app/src/main/
│   ├── java/com/athena/client/
│   │   ├── AthenaApplication.kt     # Application class
│   │   ├── MainActivity.kt          # Single activity
│   │   ├── data/                    # API layer
│   │   │   ├── ApiClient.kt         # Retrofit setup
│   │   │   ├── AthenaApi.kt         # API interface
│   │   │   └── models/              # Request/response models
│   │   ├── audio/                   # Audio playback
│   │   │   ├── AudioPlayer.kt       # WAV playback from base64
│   │   │   └── ByteArrayMediaDataSource.kt
│   │   ├── speech/                  # Voice recognition
│   │   │   └── SpeechRecognizerManager.kt
│   │   ├── ui/                      # Compose UI
│   │   │   ├── MainScreen.kt        # Main screen composable
│   │   │   ├── components/          # UI components
│   │   │   └── theme/               # App theme
│   │   └── viewmodel/
│   │       └── MainViewModel.kt     # State management
│   └── res/                         # Android resources
├── gradle/                          # Gradle wrapper
├── build.gradle.kts                 # Root build config
├── app/build.gradle.kts             # App build config
└── Makefile                         # Build commands
```

## API Endpoints Used

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/prompt` | POST | Send prompt, receive text + audio response |
| `/api/voices` | GET | List available voices (future use) |
| `/health` | GET | Server health check |

### Request Format

```json
{
  "prompt": "What's the weather like?",
  "speaker": true,
  "speaker_voice": null
}
```

### Response Format

```json
{
  "response": "The weather is sunny with a high of 75°F.",
  "audio": "<base64-encoded WAV>"
}
```

## Configuration

### Environment Variables (CI/CD)

For CI/CD builds, you can set credentials via environment variables:

```bash
export API_URL="https://your-server.com"
export API_TOKEN="your-token"
```

Then modify `app/build.gradle.kts` to read from environment:

```kotlin
buildConfigField(
    "String", "API_URL",
    "\"${System.getenv("API_URL") ?: localProperties.getProperty("api.url", "")}\""
)
```

### Release Signing

For release builds, configure signing in `app/build.gradle.kts`. **Do not commit credentials to source control** - use `local.properties` or environment variables:

```kotlin
signingConfigs {
    create("release") {
        storeFile = file(localProperties.getProperty("signing.storeFile", ""))
        storePassword = localProperties.getProperty("signing.storePassword", "")
        keyAlias = localProperties.getProperty("signing.keyAlias", "")
        keyPassword = localProperties.getProperty("signing.keyPassword", "")
    }
}

buildTypes {
    release {
        signingConfig = signingConfigs.getByName("release")
        // ...
    }
}
```

Then in `local.properties`:
```properties
signing.storeFile=/path/to/keystore.jks
signing.storePassword=your-store-password
signing.keyAlias=your-key-alias
signing.keyPassword=your-key-password
```

## Troubleshooting

### Voice recognition not working

1. Ensure microphone permission is granted
2. Check that Google Speech Services is installed on the device
3. Try restarting the app

### Connection errors

1. Verify the API URL in `local.properties`
2. Ensure the device has internet connectivity
3. Check that the server is reachable from the device's network

### Audio not playing

1. Check device volume settings
2. Verify the server is returning audio in the response
3. Check logcat for AudioPlayer errors

## Credits

App icon generated with [Easy-Peasy.AI](https://easy-peasy.ai/ai-image-generator/images/pegatina-sobre-ai-d0ee6d86-5dec-4a7f-b17d-f011196a078c).

## License

MIT License - See [LICENSE](LICENSE) for details.

---

**TODO**:
- Implement proper multi-user authentication support instead of baked-in credentials
- Add a custom app icon
