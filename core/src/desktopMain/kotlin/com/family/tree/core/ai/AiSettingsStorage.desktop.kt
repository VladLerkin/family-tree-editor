package com.family.tree.core.ai

import java.util.prefs.Preferences
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import java.security.MessageDigest
import java.util.Base64

/**
 * Desktop implementation using Java Preferences API with AES encryption for API key.
 */
actual class AiSettingsStorage {
    private val prefs: Preferences = Preferences.userNodeForPackage(AiSettingsStorage::class.java)
    
    actual fun saveConfig(config: AiConfig) {
        prefs.put(KEY_PROVIDER, config.provider)
        // Encrypt API key before saving (deprecated, for backward compatibility)
        val encryptedKey = if (config.apiKey.isNotBlank()) {
            encryptApiKey(config.apiKey)
        } else {
            ""
        }
        prefs.put(KEY_API_KEY, encryptedKey)
        prefs.put(KEY_MODEL, config.model)
        prefs.put(KEY_BASE_URL, config.baseUrl)
        prefs.putDouble(KEY_TEMPERATURE, config.temperature)
        prefs.putInt(KEY_MAX_TOKENS, config.maxTokens)
        prefs.put(KEY_LANGUAGE, config.language)
        prefs.put(KEY_TRANSCRIPTION_PROVIDER, config.transcriptionProvider)
        // Encrypt Google API key before saving (deprecated)
        val encryptedGoogleKey = if (config.googleApiKey.isNotBlank()) {
            encryptApiKey(config.googleApiKey)
        } else {
            ""
        }
        prefs.put(KEY_GOOGLE_API_KEY, encryptedGoogleKey)
        
        // Encrypt new API keys for each provider group
        val encryptedOpenAiKey = if (config.openaiApiKey.isNotBlank()) {
            encryptApiKey(config.openaiApiKey)
        } else {
            ""
        }
        prefs.put(KEY_OPENAI_API_KEY, encryptedOpenAiKey)
        

        
        val encryptedGoogleAiKey = if (config.googleAiApiKey.isNotBlank()) {
            encryptApiKey(config.googleAiApiKey)
        } else {
            ""
        }
        prefs.put(KEY_GOOGLE_AI_API_KEY, encryptedGoogleAiKey)
        
        // Encrypt Yandex API key before saving
        val encryptedYandexKey = if (config.yandexApiKey.isNotBlank()) {
            encryptApiKey(config.yandexApiKey)
        } else {
            ""
        }
        prefs.put(KEY_YANDEX_API_KEY, encryptedYandexKey)
        
        // Save Yandex Folder ID (no encryption needed, not a secret)
        prefs.put(KEY_YANDEX_FOLDER_ID, config.yandexFolderId)
        
        prefs.flush()
    }
    
    actual fun loadConfig(): AiConfig {
        val encryptedKey = prefs.get(KEY_API_KEY, "")
        // Decrypt API key on load
        val decryptedKey = if (encryptedKey.isNotBlank()) {
            try {
                decryptApiKey(encryptedKey)
            } catch (e: Exception) {
                // If decryption failed (old format or error), return as is
                encryptedKey
            }
        } else {
            ""
        }
        
        val encryptedGoogleKey = prefs.get(KEY_GOOGLE_API_KEY, "")
        // Decrypt Google API key on load
        val decryptedGoogleKey = if (encryptedGoogleKey.isNotBlank()) {
            try {
                decryptApiKey(encryptedGoogleKey)
            } catch (e: Exception) {
                encryptedGoogleKey
            }
        } else {
            ""
        }
        
        // Decrypt new API keys for provider groups
        val encryptedOpenAiKey = prefs.get(KEY_OPENAI_API_KEY, "")
        val decryptedOpenAiKey = if (encryptedOpenAiKey.isNotBlank()) {
            try {
                decryptApiKey(encryptedOpenAiKey)
            } catch (e: Exception) {
                encryptedOpenAiKey
            }
        } else {
            ""
        }
        

        
        val encryptedGoogleAiKey = prefs.get(KEY_GOOGLE_AI_API_KEY, "")
        val decryptedGoogleAiKey = if (encryptedGoogleAiKey.isNotBlank()) {
            try {
                decryptApiKey(encryptedGoogleAiKey)
            } catch (e: Exception) {
                encryptedGoogleAiKey
            }
        } else {
            ""
        }
        
        // Decrypt Yandex API key on load
        val encryptedYandexKey = prefs.get(KEY_YANDEX_API_KEY, "")
        val decryptedYandexKey = if (encryptedYandexKey.isNotBlank()) {
            try {
                decryptApiKey(encryptedYandexKey)
            } catch (e: Exception) {
                encryptedYandexKey
            }
        } else {
            ""
        }
        
        return AiConfig(
            provider = prefs.get(KEY_PROVIDER, AiPresets.OPENAI_GPT4O_MINI.provider),
            apiKey = decryptedKey,
            model = prefs.get(KEY_MODEL, AiPresets.OPENAI_GPT4O_MINI.model),
            baseUrl = prefs.get(KEY_BASE_URL, ""),
            temperature = prefs.getDouble(KEY_TEMPERATURE, 0.7),
            maxTokens = prefs.getInt(KEY_MAX_TOKENS, 4000),
            language = prefs.get(KEY_LANGUAGE, ""),
            transcriptionProvider = prefs.get(KEY_TRANSCRIPTION_PROVIDER, "OPENAI_WHISPER"),
            googleApiKey = decryptedGoogleKey,
            
            // New fields for separate provider group keys
            openaiApiKey = decryptedOpenAiKey,

            googleAiApiKey = decryptedGoogleAiKey,
            yandexApiKey = decryptedYandexKey,
            // If folderId is not saved, use default value
            yandexFolderId = prefs.get(KEY_YANDEX_FOLDER_ID, "").ifBlank { "b1guuckqs9tjoc2aiuge" }
        )
    }
    
    actual fun clearConfig() {
        prefs.remove(KEY_PROVIDER)
        prefs.remove(KEY_API_KEY)
        prefs.remove(KEY_MODEL)
        prefs.remove(KEY_BASE_URL)
        prefs.remove(KEY_TEMPERATURE)
        prefs.remove(KEY_MAX_TOKENS)
        prefs.remove(KEY_LANGUAGE)
        prefs.remove(KEY_TRANSCRIPTION_PROVIDER)
        prefs.remove(KEY_GOOGLE_API_KEY)
        
        // Remove new fields
        prefs.remove(KEY_OPENAI_API_KEY)

        prefs.remove(KEY_GOOGLE_AI_API_KEY)
        prefs.remove(KEY_YANDEX_API_KEY)
        prefs.remove(KEY_YANDEX_FOLDER_ID)
        
        prefs.flush()
    }
    
    /**
     * Encrypts API key using AES.
     */
    private fun encryptApiKey(plainText: String): String {
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
        val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(encryptedBytes)
    }
    
    /**
     * Decrypts API key.
     */
    private fun decryptApiKey(encryptedText: String): String {
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, getSecretKey())
        val decodedBytes = Base64.getDecoder().decode(encryptedText)
        val decryptedBytes = cipher.doFinal(decodedBytes)
        return String(decryptedBytes, Charsets.UTF_8)
    }
    
    /**
     * Generates a secret key based on unique system parameters.
     * Uses a combination of username, home directory, and a constant to create a stable key.
     */
    private fun getSecretKey(): SecretKeySpec {
        // Create key based on system parameters (stable for this system)
        val keySource = buildString {
            append(System.getProperty("user.name") ?: "default")
            append(System.getProperty("user.home") ?: "default")
            append(SALT) // Additional salt for security
        }
        
        // Hash with SHA-256 to get a 256-bit key
        val digest = MessageDigest.getInstance("SHA-256")
        val keyBytes = digest.digest(keySource.toByteArray(Charsets.UTF_8))
        
        return SecretKeySpec(keyBytes, "AES")
    }
    
    companion object {
        private const val KEY_PROVIDER = "ai_provider"
        private const val KEY_API_KEY = "ai_api_key"
        private const val KEY_MODEL = "ai_model"
        private const val KEY_BASE_URL = "ai_base_url"
        private const val KEY_TEMPERATURE = "ai_temperature"
        private const val KEY_MAX_TOKENS = "ai_max_tokens"
        private const val KEY_LANGUAGE = "ai_language"
        private const val KEY_TRANSCRIPTION_PROVIDER = "ai_transcription_provider"
        private const val KEY_GOOGLE_API_KEY = "ai_google_api_key"
        
        // New keys for separate provider group API keys
        private const val KEY_OPENAI_API_KEY = "ai_openai_api_key"

        private const val KEY_GOOGLE_AI_API_KEY = "ai_google_ai_api_key"
        private const val KEY_YANDEX_API_KEY = "ai_yandex_api_key"
        private const val KEY_YANDEX_FOLDER_ID = "ai_yandex_folder_id"
        
        private const val ALGORITHM = "AES"
        private const val SALT = "FamilyTreeApp-AI-Key-Salt-v1"
    }
}
