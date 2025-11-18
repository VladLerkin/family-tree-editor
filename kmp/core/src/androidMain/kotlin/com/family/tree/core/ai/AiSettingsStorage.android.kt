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
            maxTokens = prefs.getInt(KEY_MAX_TOKENS, 4000)
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
        
        private var appContext: Context? = null
        
        /**
         * Must be called from Application.onCreate() or Activity before using storage.
         */
        fun setContext(context: Context) {
            appContext = context.applicationContext
        }
    }
}
