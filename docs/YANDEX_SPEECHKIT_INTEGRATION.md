# Yandex SpeechKit Integration

## What's New?

The project now includes support for **Yandex SpeechKit** as an additional speech recognition provider for voice input.

## Changes

### New Files

1. **`core/src/commonMain/kotlin/com/family/tree/core/ai/YandexSpeechClient.kt`**
   - Client for working with Yandex SpeechKit API
   - Support for formats: OggOpus, LPCM
   - Automatic language code conversion

2. **`docs/YANDEX_SPEECHKIT_SETUP.md`**
   - Detailed setup instructions for Yandex SpeechKit
   - How to obtain an API key
   - Comparison with other providers

### Modified Files

1. **`core/src/commonMain/kotlin/com/family/tree/core/ai/AiConfig.kt`**
   - Added `YANDEX_SPEECHKIT` to `TranscriptionProvider` enum
   - Added `yandexApiKey: String` field for storing API key
   - Updated `getApiKeyForTranscription()` method to support Yandex

2. **`core/src/commonMain/kotlin/com/family/tree/core/ai/TranscriptionClient.kt`**
   - Added `YandexSpeechClient` to `TranscriptionClientFactory`

3. **`core/src/commonMain/kotlin/com/family/tree/core/ai/VoiceInputProcessor.kt`**
   - Updated log messages to display Yandex SpeechKit

4. **`docs/VOICE_INPUT.md`**
   - Updated documentation with Yandex SpeechKit mention
   - Added information about new files
   - Updated architecture diagram

## How to Use?

### 1. Get Yandex Cloud API Key

Follow the instructions in [`docs/YANDEX_SPEECHKIT_SETUP.md`](docs/YANDEX_SPEECHKIT_SETUP.md)

### 2. Configure in Application

1. Open menu → **"AI Settings..."**
2. In **"Transcription Settings"** section:
   - **Provider**: Select **"Yandex SpeechKit"**
   - **API Key**: Paste your API key
   - **Language**: Select `ru` (or other supported language)
3. Click **"Save"**

### 3. Use Voice Input

1. Open menu → **"Voice Input 🎤"**
2. Speak a phrase describing relatives
3. The system will automatically recognize speech via Yandex SpeechKit

## Yandex SpeechKit Advantages

- ✅ **Excellent Russian language support** - one of the best on the market
- ✅ **CIS language support** - Ukrainian, Kazakh, Uzbek, and others
- ✅ **Fast processing** - low latency
- ✅ **Free tier** - 15 hours per month free

## Supported Transcription Providers

Three providers are now available:

| Provider | Best For | Free Tier |
|----------|----------|-----------|
| **OpenAI Whisper** | Multilingual | None (paid) |
| **Google Speech-to-Text** | Universal | 60 min/month |
| **Yandex SpeechKit** | Russian language | 15 hours/month |

## Technical Details

### API Endpoint
```
https://stt.api.cloud.yandex.net/speech/v1/stt:recognize
```

### Supported Audio Formats
- OggOpus (recommended)
- LPCM (Linear PCM)

### Supported Languages
- Russian (`ru-RU`)
- English (`en-US`)
- Turkish (`tr-TR`)
- Ukrainian (`uk-UA`)
- Kazakh (`kk-KZ`)
- Uzbek (`uz-UZ`)
- And others (see documentation)

## Testing

Project compiled successfully:
```bash
./gradlew :core:build
# BUILD SUCCESSFUL
```

## Future Improvements

- [ ] Add streaming API support for real-time recognition
- [ ] Add recognition model configuration (general, numbers, dates)
- [ ] Add profanity filter (optional)
- [ ] Add custom dictionary support

## Documentation

- [Yandex SpeechKit Setup](docs/YANDEX_SPEECHKIT_SETUP.md)
- [Voice Input](docs/VOICE_INPUT.md)
- [Official Yandex SpeechKit Documentation](https://cloud.yandex.ru/docs/speechkit/)
