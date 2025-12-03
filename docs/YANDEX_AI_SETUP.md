# Yandex AI Setup for Application

The application supports integration with Yandex Cloud AI (YandexGPT and Yandex SpeechKit) for text generation and speech recognition.

## Connection Methods

### Method 1: Service Account API Key (Recommended)

This is the simplest method - you only need an API key. Folder ID can be left as "default", and the folder where the service account was created will be used.

#### Setup Steps:

1. **Register in Yandex Cloud**
   - Go to [cloud.yandex.ru](https://cloud.yandex.ru)
   - Create an account and link a billing account (free grant available)

2. **Create Service Account**
   - In Yandex Cloud console, go to "Identity and Access Management" (or "Service Accounts")
   - Click "Create Service Account"
   - Enter a name (e.g., "family-tree-ai")
   - **IMPORTANT: Assign roles!** Without this, you'll get a `PermissionDenied` error.
     - `ai.languageModels.user` - for YandexGPT (text generation)
     - `ai.speechkit-stt.user` - for Yandex SpeechKit (speech recognition)

3. **Create API Key**
   - Select the created service account
   - Go to "API Keys" section (or "Access Keys" → "Create API Key")
   - Click "Create API Key"
   - **Save the key** - it's shown only once!
   - Key format: `AQVN...`

4. **Configure Application**
   - Open the application
   - Go to menu → "AI Settings"
   - Select provider "YANDEX" (for text generation) or "Yandex SpeechKit" (for speech recognition)
   - Paste the API key in "Yandex Cloud API Key" field
   - In Folder ID field, leave `default` (or specify a specific ID if needed)
   - Click "Continue"

### Method 2: API Key with Explicit Folder ID

If you want to explicitly control which folder is used, you can specify the Folder ID.

#### Additional Steps:

1. **Find Folder ID**
   - In Yandex Cloud console, select the desired folder (catalog)
   - Folder ID is displayed at the top of the page next to the folder name
   - Format: `b1g...`

2. **Specify in Settings**
   - In AI settings, enter the Folder ID in the corresponding field
   - This will override automatic folder detection

## YandexGPT Models

The application supports the following models:

- **YandexGPT 4** (`yandexgpt-4`) - main model, more powerful
- **YandexGPT 4 Lite** (`yandexgpt-4-lite`) - lightweight version, faster and cheaper

## Yandex SpeechKit

For speech recognition, the same API key is used:

1. In AI settings, select "Speech Recognition Provider" → "Yandex SpeechKit"
2. Enter the same API key
3. Yandex SpeechKit is especially good for Russian and CIS languages

## Pricing

- First 1000 requests per month - free (grant for new users)
- YandexGPT Lite: ~0.12₽ per 1000 tokens
- YandexGPT Pro: ~0.6₽ per 1000 tokens
- SpeechKit: ~0.24₽ per minute of audio

Current prices: [yandex.cloud/ru/docs/foundation-models/pricing](https://yandex.cloud/ru/docs/foundation-models/pricing)

## Troubleshooting

### Error "Permission denied" (401 Unauthorized)
**Symptoms:** Error like `Permission to [resource-manager.folder ...] denied`.
**Cause:** Service account doesn't have permissions to perform the operation.
**Solution:**
1. Go to Yandex Cloud console.
2. Find your service account.
3. Ensure it has the following roles assigned:
   - `ai.languageModels.user` (for GPT)
   - `ai.speechkit-stt.user` (for speech recognition)
4. If roles are missing, add them in the "Roles" tab or when editing the folder.

### Error "API key is required"
- Check that you copied the API key completely
- API key should start with `AQVN`

### Error "Folder ID is required"
- If using service account API key, try leaving the Folder ID field with value `default`
- If error persists, specify Folder ID explicitly (see Method 2)

## Useful Links

- [YandexGPT Documentation](https://yandex.cloud/ru/docs/foundation-models/concepts/yandexgpt)
- [SpeechKit Documentation](https://yandex.cloud/ru/docs/speechkit/)
- [Yandex Cloud Console](https://console.cloud.yandex.ru/)
