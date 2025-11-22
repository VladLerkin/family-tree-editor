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
        // Шифруем API ключ перед сохранением (deprecated, для обратной совместимости)
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
        // Шифруем Google API ключ перед сохранением (deprecated)
        val encryptedGoogleKey = if (config.googleApiKey.isNotBlank()) {
            encryptApiKey(config.googleApiKey)
        } else {
            ""
        }
        prefs.put(KEY_GOOGLE_API_KEY, encryptedGoogleKey)
        
        // Шифруем новые API ключи для каждой группы провайдеров
        val encryptedOpenAiKey = if (config.openaiApiKey.isNotBlank()) {
            encryptApiKey(config.openaiApiKey)
        } else {
            ""
        }
        prefs.put(KEY_OPENAI_API_KEY, encryptedOpenAiKey)
        
        val encryptedAnthropicKey = if (config.anthropicApiKey.isNotBlank()) {
            encryptApiKey(config.anthropicApiKey)
        } else {
            ""
        }
        prefs.put(KEY_ANTHROPIC_API_KEY, encryptedAnthropicKey)
        
        val encryptedGoogleAiKey = if (config.googleAiApiKey.isNotBlank()) {
            encryptApiKey(config.googleAiApiKey)
        } else {
            ""
        }
        prefs.put(KEY_GOOGLE_AI_API_KEY, encryptedGoogleAiKey)
        
        prefs.flush()
    }
    
    actual fun loadConfig(): AiConfig {
        val encryptedKey = prefs.get(KEY_API_KEY, "")
        // Расшифровываем API ключ при загрузке
        val decryptedKey = if (encryptedKey.isNotBlank()) {
            try {
                decryptApiKey(encryptedKey)
            } catch (e: Exception) {
                // Если не удалось расшифровать (старый формат или ошибка), возвращаем как есть
                encryptedKey
            }
        } else {
            ""
        }
        
        val encryptedGoogleKey = prefs.get(KEY_GOOGLE_API_KEY, "")
        // Расшифровываем Google API ключ при загрузке
        val decryptedGoogleKey = if (encryptedGoogleKey.isNotBlank()) {
            try {
                decryptApiKey(encryptedGoogleKey)
            } catch (e: Exception) {
                encryptedGoogleKey
            }
        } else {
            ""
        }
        
        // Расшифровываем новые API ключи для групп провайдеров
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
        
        val encryptedAnthropicKey = prefs.get(KEY_ANTHROPIC_API_KEY, "")
        val decryptedAnthropicKey = if (encryptedAnthropicKey.isNotBlank()) {
            try {
                decryptApiKey(encryptedAnthropicKey)
            } catch (e: Exception) {
                encryptedAnthropicKey
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
            
            // Новые поля для отдельных ключей групп провайдеров
            openaiApiKey = decryptedOpenAiKey,
            anthropicApiKey = decryptedAnthropicKey,
            googleAiApiKey = decryptedGoogleAiKey
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
        
        // Удаляем новые поля
        prefs.remove(KEY_OPENAI_API_KEY)
        prefs.remove(KEY_ANTHROPIC_API_KEY)
        prefs.remove(KEY_GOOGLE_AI_API_KEY)
        
        prefs.flush()
    }
    
    /**
     * Шифрует API ключ с использованием AES.
     */
    private fun encryptApiKey(plainText: String): String {
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
        val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(encryptedBytes)
    }
    
    /**
     * Расшифровывает API ключ.
     */
    private fun decryptApiKey(encryptedText: String): String {
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, getSecretKey())
        val decodedBytes = Base64.getDecoder().decode(encryptedText)
        val decryptedBytes = cipher.doFinal(decodedBytes)
        return String(decryptedBytes, Charsets.UTF_8)
    }
    
    /**
     * Генерирует секретный ключ на основе уникальных параметров системы.
     * Использует комбинацию username, home directory и константы для создания стабильного ключа.
     */
    private fun getSecretKey(): SecretKeySpec {
        // Создаём ключ на основе системных параметров (стабильный для данной системы)
        val keySource = buildString {
            append(System.getProperty("user.name") ?: "default")
            append(System.getProperty("user.home") ?: "default")
            append(SALT) // Дополнительная соль для безопасности
        }
        
        // Хэшируем в SHA-256 для получения 256-битного ключа
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
        
        // Новые ключи для отдельных API ключей групп провайдеров
        private const val KEY_OPENAI_API_KEY = "ai_openai_api_key"
        private const val KEY_ANTHROPIC_API_KEY = "ai_anthropic_api_key"
        private const val KEY_GOOGLE_AI_API_KEY = "ai_google_ai_api_key"
        
        private const val ALGORITHM = "AES"
        private const val SALT = "FamilyTreeApp-AI-Key-Salt-v1"
    }
}
