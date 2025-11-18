package com.family.tree.core.ai

import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUserDefaults
import platform.Foundation.create
import platform.Security.*
import platform.CoreFoundation.*
import platform.posix.memcpy
import kotlinx.cinterop.*

/**
 * iOS implementation using NSUserDefaults for settings and Keychain for secure API key storage.
 */
actual class AiSettingsStorage {
    private val defaults = NSUserDefaults.standardUserDefaults
    
    actual fun saveConfig(config: AiConfig) {
        defaults.setObject(config.provider, KEY_PROVIDER)
        // Сохраняем API ключ в Keychain для безопасности
        if (config.apiKey.isNotBlank()) {
            saveToKeychain(KEYCHAIN_KEY_API_KEY, config.apiKey)
        } else {
            deleteFromKeychain(KEYCHAIN_KEY_API_KEY)
        }
        defaults.setObject(config.model, KEY_MODEL)
        defaults.setObject(config.baseUrl, KEY_BASE_URL)
        defaults.setDouble(config.temperature, KEY_TEMPERATURE)
        defaults.setInteger(config.maxTokens.toLong(), KEY_MAX_TOKENS)
        defaults.synchronize()
    }
    
    actual fun loadConfig(): AiConfig {
        // Загружаем API ключ из Keychain
        val apiKey = loadFromKeychain(KEYCHAIN_KEY_API_KEY) ?: ""
        
        return AiConfig(
            provider = defaults.stringForKey(KEY_PROVIDER) ?: AiPresets.OPENAI_GPT4O_MINI.provider,
            apiKey = apiKey,
            model = defaults.stringForKey(KEY_MODEL) ?: AiPresets.OPENAI_GPT4O_MINI.model,
            baseUrl = defaults.stringForKey(KEY_BASE_URL) ?: "",
            temperature = defaults.doubleForKey(KEY_TEMPERATURE).takeIf { it != 0.0 } ?: 0.7,
            maxTokens = defaults.integerForKey(KEY_MAX_TOKENS).toInt().takeIf { it != 0 } ?: 4000
        )
    }
    
    actual fun clearConfig() {
        defaults.removeObjectForKey(KEY_PROVIDER)
        deleteFromKeychain(KEYCHAIN_KEY_API_KEY)
        defaults.removeObjectForKey(KEY_MODEL)
        defaults.removeObjectForKey(KEY_BASE_URL)
        defaults.removeObjectForKey(KEY_TEMPERATURE)
        defaults.removeObjectForKey(KEY_MAX_TOKENS)
        defaults.synchronize()
    }
    
    /**
     * Сохраняет значение в iOS Keychain.
     */
    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    private fun saveToKeychain(key: String, value: String) {
        // Сначала удаляем существующий элемент, если есть
        deleteFromKeychain(key)
        
        // Подготавливаем данные для сохранения
        val valueData = value.encodeToByteArray()
        val nsData = valueData.usePinned { pinned ->
            NSData.create(bytes = pinned.addressOf(0), length = valueData.size.toULong())
        }
        
        // Создаём словарь атрибутов для Keychain
        val query = mapOf(
            kSecClass to kSecClassGenericPassword,
            kSecAttrAccount to key,
            kSecValueData to nsData,
            kSecAttrAccessible to kSecAttrAccessibleWhenUnlocked
        )
        
        // Сохраняем в Keychain
        SecItemAdd(query as CFDictionaryRef, null)
    }
    
    /**
     * Загружает значение из iOS Keychain.
     */
    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    private fun loadFromKeychain(key: String): String? {
        memScoped {
            val query = mapOf(
                kSecClass to kSecClassGenericPassword,
                kSecAttrAccount to key,
                kSecReturnData to kCFBooleanTrue,
                kSecMatchLimit to kSecMatchLimitOne
            )
            
            val result = alloc<CFTypeRefVar>()
            val status = SecItemCopyMatching(query as CFDictionaryRef, result.ptr)
            
            if (status == errSecSuccess) {
                val data = result.value as? NSData
                return data?.let {
                    val bytes = ByteArray(it.length.toInt())
                    if (bytes.isNotEmpty()) {
                        bytes.usePinned { pinned ->
                            memcpy(pinned.addressOf(0), it.bytes, it.length)
                        }
                    }
                    bytes.decodeToString()
                }
            }
            
            return null
        }
    }
    
    /**
     * Удаляет значение из iOS Keychain.
     */
    @OptIn(ExperimentalForeignApi::class)
    private fun deleteFromKeychain(key: String) {
        val query = mapOf(
            kSecClass to kSecClassGenericPassword,
            kSecAttrAccount to key
        )
        
        SecItemDelete(query as CFDictionaryRef)
    }
    
    companion object {
        private const val KEY_PROVIDER = "ai_provider"
        private const val KEY_MODEL = "ai_model"
        private const val KEY_BASE_URL = "ai_base_url"
        private const val KEY_TEMPERATURE = "ai_temperature"
        private const val KEY_MAX_TOKENS = "ai_max_tokens"
        
        // Keychain key для API ключа
        private const val KEYCHAIN_KEY_API_KEY = "com.family.tree.ai_api_key"
    }
}
