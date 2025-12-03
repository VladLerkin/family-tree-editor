# Yandex SpeechKit Setup for Speech Recognition

## What is Yandex SpeechKit?

Yandex SpeechKit is a cloud-based speech recognition and synthesis service from Yandex. It is excellent for recognizing Russian and CIS languages (Ukrainian, Kazakh, Uzbek, etc.).

## Yandex SpeechKit Advantages

- ✅ **Excellent Russian language support** - one of the best on the market
- ✅ **CIS language support** - Ukrainian, Kazakh, Uzbek, Belarusian, and others
- ✅ **Fast processing** - low latency
- ✅ **Affordable pricing** - first 15 hours per month free
- ✅ **High accuracy** - especially for Russian language

## How to Get an API Key

### Step 1: Create Yandex Cloud Account

1. Go to [cloud.yandex.ru](https://cloud.yandex.ru)
2. Click **"Sign in to Console"** or **"Try for Free"**
3. Sign in with Yandex ID or create a new account
4. Accept the terms of use

### Step 2: Create Billing Account

1. In Yandex Cloud console, create a **billing account**
2. For individuals, basic information is sufficient
3. **Important**: First 15 hours of speech recognition per month are **free**
4. Card binding is required, but there will be no charges within the free tier

### Step 3: Create API Key

1. In Yandex Cloud console, go to **"Service Accounts"** section
2. Click **"Create Service Account"**
3. Enter a name (e.g., `speechkit-service`)
4. Add role **`ai.speechkit-stt.user`** (for speech recognition)
5. Click **"Create"**

6. Open the created service account
7. Go to **"API Keys"** tab
8. Click **"Create API Key"**
9. Copy and save the key (it's shown only once!)

**API Key Format**: `AQVNxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx`

### Step 4: Configure in Application

1. Open Family Tree application
2. Open menu (three dots) → **"AI Settings..."**
3. In **"Transcription Settings"** section:
   - **Provider**: Select **"Yandex SpeechKit"**
   - **API Key**: Paste the copied API key
   - **Language**: Select language (e.g., `ru` for Russian)
4. Click **"Save"**

## Supported Languages

Yandex SpeechKit supports the following languages:

| Language | Code | Quality |
|----------|------|---------|
| Russian | `ru` | ⭐⭐⭐⭐⭐ Excellent |
| English | `en` | ⭐⭐⭐⭐ Good |
| Turkish | `tr` | ⭐⭐⭐⭐ Good |
| Ukrainian | `uk` | ⭐⭐⭐⭐ Good |
| Kazakh | `kk` | ⭐⭐⭐⭐ Good |
| Uzbek | `uz` | ⭐⭐⭐⭐ Good |
| Belarusian | `be` | ⭐⭐⭐ Average |
| Armenian | `hy` | ⭐⭐⭐ Average |
| Georgian | `ka` | ⭐⭐⭐ Average |
| German | `de` | ⭐⭐⭐ Average |
| French | `fr` | ⭐⭐⭐ Average |
| Spanish | `es` | ⭐⭐⭐ Average |

## Pricing

- **Free**: First 15 hours of recognition per month
- **Paid**: After exceeding the limit — ~₽1.20 per minute

For personal use, the free tier is more than sufficient.

## Comparison with Other Providers

| Provider | Russian Language | Speed | Cost | Recommendation |
|----------|------------------|-------|------|----------------|
| **Yandex SpeechKit** | ⭐⭐⭐⭐⭐ | Fast | 15h free | ✅ Best for Russian |
| **Google Speech** | ⭐⭐⭐⭐ | Fast | 60min free | ✅ Universal |
| **OpenAI Whisper** | ⭐⭐⭐⭐ | Slow | $0.006/min | ✅ Many languages |

## Troubleshooting

### Error "API key is required"
- Check that you copied the API key completely
- Ensure "Yandex SpeechKit" provider is selected in settings

### Error "Invalid API key"
- Verify that the API key is valid
- Ensure the service account has the `ai.speechkit-stt.user` role assigned

### Error "Quota exceeded"
- You have exceeded the free 15-hour monthly limit
- Check usage in Yandex Cloud console
- Wait until the next month or top up your balance

### Poor Recognition Quality
- Speak more clearly and louder
- Reduce background noise
- Ensure the correct language is selected in settings

## Useful Links

- [Yandex SpeechKit Documentation](https://cloud.yandex.ru/docs/speechkit/)
- [Yandex Cloud Console](https://console.cloud.yandex.ru/)
- [SpeechKit Pricing](https://cloud.yandex.ru/docs/speechkit/pricing)
- [Supported Languages](https://cloud.yandex.ru/docs/speechkit/stt/models)
