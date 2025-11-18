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
        // Шифруем API ключ перед сохранением
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
        
        return AiConfig(
            provider = prefs.get(KEY_PROVIDER, AiPresets.OPENAI_GPT4O_MINI.provider),
            apiKey = decryptedKey,
            model = prefs.get(KEY_MODEL, AiPresets.OPENAI_GPT4O_MINI.model),
            baseUrl = prefs.get(KEY_BASE_URL, ""),
            temperature = prefs.getDouble(KEY_TEMPERATURE, 0.7),
            maxTokens = prefs.getInt(KEY_MAX_TOKENS, 4000)
        )
    }
    
    actual fun clearConfig() {
        prefs.remove(KEY_PROVIDER)
        prefs.remove(KEY_API_KEY)
        prefs.remove(KEY_MODEL)
        prefs.remove(KEY_BASE_URL)
        prefs.remove(KEY_TEMPERATURE)
        prefs.remove(KEY_MAX_TOKENS)
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
        
        private const val ALGORITHM = "AES"
        private const val SALT = "FamilyTreeApp-AI-Key-Salt-v1"
    }
}
