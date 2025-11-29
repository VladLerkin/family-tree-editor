package com.family.tree.core.ai

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Android implementation using EncryptedSharedPreferences for secure API key storage.
 * Note: Requires context to be set before use via setContext().
 */
actual class AiSettingsStorage {
    private val prefs: SharedPreferences by lazy {
        val context = requireNotNull(appContext) { 
            "Context must be set via AiSettingsStorage.setContext() before use" 
        }
        
        // Создаём MasterKey для шифрования
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        
        // Создаём EncryptedSharedPreferences с автоматическим шифрованием
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
    
    actual fun saveConfig(config: AiConfig) {
        prefs.edit().apply {
            putString(KEY_PROVIDER, config.provider)
            putString(KEY_API_KEY, config.apiKey)
            putString(KEY_MODEL, config.model)
            putString(KEY_BASE_URL, config.baseUrl)
            putFloat(KEY_TEMPERATURE, config.temperature.toFloat())
            putInt(KEY_MAX_TOKENS, config.maxTokens)
            putString(KEY_LANGUAGE, config.language)
            putString(KEY_TRANSCRIPTION_PROVIDER, config.transcriptionProvider)
            putString(KEY_GOOGLE_API_KEY, config.googleApiKey)  // Deprecated, но сохраняем для обратной совместимости
            
            // Новые поля для отдельных ключей групп провайдеров
            putString(KEY_OPENAI_API_KEY, config.openaiApiKey)
            putString(KEY_ANTHROPIC_API_KEY, config.anthropicApiKey)
            putString(KEY_GOOGLE_AI_API_KEY, config.googleAiApiKey)
            putString(KEY_YANDEX_API_KEY, config.yandexApiKey)
            putString(KEY_YANDEX_FOLDER_ID, config.yandexFolderId)
            apply()
        }
    }
    
    actual fun loadConfig(): AiConfig {
        return AiConfig(
            provider = prefs.getString(KEY_PROVIDER, AiPresets.OPENAI_GPT4O_MINI.provider) ?: AiPresets.OPENAI_GPT4O_MINI.provider,
            apiKey = prefs.getString(KEY_API_KEY, "") ?: "",
            model = prefs.getString(KEY_MODEL, AiPresets.OPENAI_GPT4O_MINI.model) ?: AiPresets.OPENAI_GPT4O_MINI.model,
            baseUrl = prefs.getString(KEY_BASE_URL, "") ?: "",
            temperature = prefs.getFloat(KEY_TEMPERATURE, 0.7f).toDouble(),
            maxTokens = prefs.getInt(KEY_MAX_TOKENS, 4000),
            language = prefs.getString(KEY_LANGUAGE, "") ?: "",
            transcriptionProvider = prefs.getString(KEY_TRANSCRIPTION_PROVIDER, "OPENAI_WHISPER") ?: "OPENAI_WHISPER",
            googleApiKey = prefs.getString(KEY_GOOGLE_API_KEY, "") ?: "",
            
            // Новые поля для отдельных ключей групп провайдеров
            openaiApiKey = prefs.getString(KEY_OPENAI_API_KEY, "") ?: "",
            anthropicApiKey = prefs.getString(KEY_ANTHROPIC_API_KEY, "") ?: "",
            googleAiApiKey = prefs.getString(KEY_GOOGLE_AI_API_KEY, "") ?: "",
            yandexApiKey = prefs.getString(KEY_YANDEX_API_KEY, "") ?: "",
            yandexFolderId = prefs.getString(KEY_YANDEX_FOLDER_ID, "") ?: ""
        )
    }
    
    actual fun clearConfig() {
        prefs.edit().apply {
            remove(KEY_PROVIDER)
            remove(KEY_API_KEY)
            remove(KEY_MODEL)
            remove(KEY_BASE_URL)
            remove(KEY_TEMPERATURE)
            remove(KEY_MAX_TOKENS)
            remove(KEY_LANGUAGE)
            remove(KEY_TRANSCRIPTION_PROVIDER)
            remove(KEY_GOOGLE_API_KEY)
            
            // Удаляем новые поля
            remove(KEY_OPENAI_API_KEY)
            remove(KEY_ANTHROPIC_API_KEY)
            remove(KEY_GOOGLE_AI_API_KEY)
            remove(KEY_YANDEX_API_KEY)
            remove(KEY_YANDEX_FOLDER_ID)
            apply()
        }
    }
    
    companion object {
        private const val PREFS_NAME = "ai_settings"
        private const val KEY_PROVIDER = "ai_provider"
        private const val KEY_API_KEY = "ai_api_key"
        private const val KEY_MODEL = "ai_model"
        private const val KEY_BASE_URL = "ai_base_url"
        private const val KEY_TEMPERATURE = "ai_temperature"
        private const val KEY_MAX_TOKENS = "ai_max_tokens"
        private const val KEY_LANGUAGE = "ai_language"
        private const val KEY_TRANSCRIPTION_PROVIDER = "ai_transcription_provider"
        private const val KEY_GOOGLE_API_KEY = "ai_google_api_key"
        
        // Новые ключи для отдельных API ключей групп провайдеров
        private const val KEY_OPENAI_API_KEY = "ai_openai_api_key"
        private const val KEY_ANTHROPIC_API_KEY = "ai_anthropic_api_key"
        private const val KEY_GOOGLE_AI_API_KEY = "ai_google_ai_api_key"
        private const val KEY_YANDEX_API_KEY = "ai_yandex_api_key"
        private const val KEY_YANDEX_FOLDER_ID = "ai_yandex_folder_id"
        
        private var appContext: Context? = null
        
        /**
         * Must be called from Application.onCreate() or Activity before using storage.
         */
        fun setContext(context: Context) {
            appContext = context.applicationContext
        }
    }
}
