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
        println("[DEBUG_LOG] AiSettingsStorage (iOS): saveConfig called")
        println("[DEBUG_LOG] AiSettingsStorage (iOS): provider=${config.provider}, apiKey=${if (config.apiKey.isBlank()) "empty" else "present (${config.apiKey.length} chars)"}")
        
        defaults.setObject(config.provider, KEY_PROVIDER)
        // Сохраняем API ключ в Keychain для безопасности (deprecated, для обратной совместимости)
        if (config.apiKey.isNotBlank()) {
            println("[DEBUG_LOG] AiSettingsStorage (iOS): Saving API key to Keychain")
            saveToKeychain(KEYCHAIN_KEY_API_KEY, config.apiKey)
        } else {
            println("[DEBUG_LOG] AiSettingsStorage (iOS): Deleting API key from Keychain")
            deleteFromKeychain(KEYCHAIN_KEY_API_KEY)
        }
        defaults.setObject(config.model, KEY_MODEL)
        defaults.setObject(config.baseUrl, KEY_BASE_URL)
        defaults.setDouble(config.temperature, KEY_TEMPERATURE)
        defaults.setInteger(config.maxTokens.toLong(), KEY_MAX_TOKENS)
        defaults.setObject(config.language, KEY_LANGUAGE)
        defaults.setObject(config.transcriptionProvider, KEY_TRANSCRIPTION_PROVIDER)
        // Сохраняем Google API ключ в Keychain для безопасности (deprecated)
        if (config.googleApiKey.isNotBlank()) {
            saveToKeychain(KEYCHAIN_KEY_GOOGLE_API_KEY, config.googleApiKey)
        } else {
            deleteFromKeychain(KEYCHAIN_KEY_GOOGLE_API_KEY)
        }
        
        // Сохраняем новые API ключи для групп провайдеров в Keychain
        if (config.openaiApiKey.isNotBlank()) {
            saveToKeychain(KEYCHAIN_KEY_OPENAI_API_KEY, config.openaiApiKey)
        } else {
            deleteFromKeychain(KEYCHAIN_KEY_OPENAI_API_KEY)
        }
        
        if (config.anthropicApiKey.isNotBlank()) {
            saveToKeychain(KEYCHAIN_KEY_ANTHROPIC_API_KEY, config.anthropicApiKey)
        } else {
            deleteFromKeychain(KEYCHAIN_KEY_ANTHROPIC_API_KEY)
        }
        
        if (config.googleAiApiKey.isNotBlank()) {
            saveToKeychain(KEYCHAIN_KEY_GOOGLE_AI_API_KEY, config.googleAiApiKey)
        } else {
            deleteFromKeychain(KEYCHAIN_KEY_GOOGLE_AI_API_KEY)
        }
        
        defaults.synchronize()
        
        println("[DEBUG_LOG] AiSettingsStorage (iOS): saveConfig completed")
    }
    
    actual fun loadConfig(): AiConfig {
        // Загружаем API ключ из Keychain (deprecated)
        val apiKey = loadFromKeychain(KEYCHAIN_KEY_API_KEY) ?: ""
        // Загружаем Google API ключ из Keychain (deprecated)
        val googleApiKey = loadFromKeychain(KEYCHAIN_KEY_GOOGLE_API_KEY) ?: ""
        
        // Загружаем новые API ключи для групп провайдеров из Keychain
        val openaiApiKey = loadFromKeychain(KEYCHAIN_KEY_OPENAI_API_KEY) ?: ""
        val anthropicApiKey = loadFromKeychain(KEYCHAIN_KEY_ANTHROPIC_API_KEY) ?: ""
        val googleAiApiKey = loadFromKeychain(KEYCHAIN_KEY_GOOGLE_AI_API_KEY) ?: ""
        
        println("[DEBUG_LOG] AiSettingsStorage (iOS): loadConfig called")
        println("[DEBUG_LOG] AiSettingsStorage (iOS): apiKey from Keychain = ${if (apiKey.isBlank()) "empty" else "present (${apiKey.length} chars)"}")
        
        val provider = defaults.stringForKey(KEY_PROVIDER) ?: AiPresets.OPENAI_GPT4O_MINI.provider
        val model = defaults.stringForKey(KEY_MODEL) ?: AiPresets.OPENAI_GPT4O_MINI.model
        val baseUrl = defaults.stringForKey(KEY_BASE_URL) ?: ""
        val temperature = defaults.doubleForKey(KEY_TEMPERATURE).takeIf { it != 0.0 } ?: 0.7
        val maxTokens = defaults.integerForKey(KEY_MAX_TOKENS).toInt().takeIf { it != 0 } ?: 4000
        val language = defaults.stringForKey(KEY_LANGUAGE) ?: ""
        val transcriptionProvider = defaults.stringForKey(KEY_TRANSCRIPTION_PROVIDER) ?: "OPENAI_WHISPER"
        
        println("[DEBUG_LOG] AiSettingsStorage (iOS): provider=$provider, model=$model")
        
        return AiConfig(
            provider = provider,
            apiKey = apiKey,
            model = model,
            baseUrl = baseUrl,
            temperature = temperature,
            maxTokens = maxTokens,
            language = language,
            transcriptionProvider = transcriptionProvider,
            googleApiKey = googleApiKey,
            
            // Новые поля для отдельных ключей групп провайдеров
            openaiApiKey = openaiApiKey,
            anthropicApiKey = anthropicApiKey,
            googleAiApiKey = googleAiApiKey
        )
    }
    
    actual fun clearConfig() {
        defaults.removeObjectForKey(KEY_PROVIDER)
        deleteFromKeychain(KEYCHAIN_KEY_API_KEY)
        defaults.removeObjectForKey(KEY_MODEL)
        defaults.removeObjectForKey(KEY_BASE_URL)
        defaults.removeObjectForKey(KEY_TEMPERATURE)
        defaults.removeObjectForKey(KEY_MAX_TOKENS)
        defaults.removeObjectForKey(KEY_LANGUAGE)
        defaults.removeObjectForKey(KEY_TRANSCRIPTION_PROVIDER)
        deleteFromKeychain(KEYCHAIN_KEY_GOOGLE_API_KEY)
        
        // Удаляем новые ключи из Keychain
        deleteFromKeychain(KEYCHAIN_KEY_OPENAI_API_KEY)
        deleteFromKeychain(KEYCHAIN_KEY_ANTHROPIC_API_KEY)
        deleteFromKeychain(KEYCHAIN_KEY_GOOGLE_AI_API_KEY)
        
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
        
        memScoped {
            // Создаём CFString из Kotlin String
            val cfKey = CFStringCreateWithCString(null, key, kCFStringEncodingUTF8)
            
            // Преобразуем NSData в CFTypeRef через reinterpret
            val cfData: CFTypeRef? = interpretCPointer(nsData.objcPtr())
            
            // Создаём массивы ключей и значений для CFDictionary
            val keys = allocArrayOf(kSecClass, kSecAttrAccount, kSecValueData, kSecAttrAccessible)
            val values = allocArrayOf(kSecClassGenericPassword, cfKey, cfData, kSecAttrAccessibleWhenUnlocked)
            
            val query = CFDictionaryCreate(
                null,
                keys.reinterpret(),
                values.reinterpret(),
                4,
                null,
                null
            )
            
            // Сохраняем в Keychain
            val addStatus = SecItemAdd(query, null)
            println("[DEBUG_LOG] AiSettingsStorage (iOS): saveToKeychain status=$addStatus for key=$key (errSecSuccess=0, errSecDuplicateItem=-25299)")
            CFRelease(query)
            if (cfKey != null) CFRelease(cfKey)
        }
    }
    
    /**
     * Загружает значение из iOS Keychain.
     */
    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    private fun loadFromKeychain(key: String): String? {
        println("[DEBUG_LOG] AiSettingsStorage (iOS): loadFromKeychain called for key=$key")
        
        memScoped {
            // Создаём CFString из Kotlin String
            val cfKey = CFStringCreateWithCString(null, key, kCFStringEncodingUTF8)
            
            // Создаём массивы ключей и значений для CFDictionary
            val keys = allocArrayOf(kSecClass, kSecAttrAccount, kSecReturnData, kSecMatchLimit)
            val values = allocArrayOf(kSecClassGenericPassword, cfKey, kCFBooleanTrue, kSecMatchLimitOne)
            
            val query = CFDictionaryCreate(
                null,
                keys.reinterpret(),
                values.reinterpret(),
                4,
                null,
                null
            )
            
            val result = alloc<CFTypeRefVar>()
            val status = SecItemCopyMatching(query, result.ptr)
            
            println("[DEBUG_LOG] AiSettingsStorage (iOS): Keychain query status=$status (errSecSuccess=0, errSecItemNotFound=-25300)")
            
            var resultString: String? = null
            
            if (status == errSecSuccess && result.value != null) {
                // Правильное преобразование CFTypeRef в NSData
                // CFTypeRef это указатель на ObjC объект, извлекаем rawValue и преобразуем
                val cfDataPtr = result.value
                val nsData: NSData? = interpretObjCPointer(cfDataPtr.rawValue)
                
                println("[DEBUG_LOG] AiSettingsStorage (iOS): NSData cast result: ${if (nsData != null) "success, length=${nsData.length}" else "failed"}")
                
                if (nsData != null) {
                    val bytes = ByteArray(nsData.length.toInt())
                    if (bytes.isNotEmpty()) {
                        bytes.usePinned { pinned ->
                            memcpy(pinned.addressOf(0), nsData.bytes, nsData.length)
                        }
                    }
                    resultString = bytes.decodeToString()
                    println("[DEBUG_LOG] AiSettingsStorage (iOS): Loaded from Keychain: ${resultString.length} chars")
                }
            } else {
                println("[DEBUG_LOG] AiSettingsStorage (iOS): SecItemCopyMatching failed with status=$status")
            }
            
            // Освобождаем ресурсы после извлечения данных
            CFRelease(query)
            if (cfKey != null) CFRelease(cfKey)
            
            return resultString
        }
    }
    
    /**
     * Удаляет значение из iOS Keychain.
     */
    @OptIn(ExperimentalForeignApi::class)
    private fun deleteFromKeychain(key: String) {
        memScoped {
            // Создаём CFString из Kotlin String
            val cfKey = CFStringCreateWithCString(null, key, kCFStringEncodingUTF8)
            
            // Создаём массивы ключей и значений для CFDictionary
            val keys = allocArrayOf(kSecClass, kSecAttrAccount)
            val values = allocArrayOf(kSecClassGenericPassword, cfKey)
            
            val query = CFDictionaryCreate(
                null,
                keys.reinterpret(),
                values.reinterpret(),
                2,
                null,
                null
            )
            
            SecItemDelete(query)
            CFRelease(query)
            if (cfKey != null) CFRelease(cfKey)
        }
    }
    
    companion object {
        private const val KEY_PROVIDER = "ai_provider"
        private const val KEY_MODEL = "ai_model"
        private const val KEY_BASE_URL = "ai_base_url"
        private const val KEY_TEMPERATURE = "ai_temperature"
        private const val KEY_MAX_TOKENS = "ai_max_tokens"
        private const val KEY_LANGUAGE = "ai_language"
        private const val KEY_TRANSCRIPTION_PROVIDER = "ai_transcription_provider"
        
        // Keychain keys для API ключей
        private const val KEYCHAIN_KEY_API_KEY = "com.family.tree.ai_api_key"
        private const val KEYCHAIN_KEY_GOOGLE_API_KEY = "com.family.tree.ai_google_api_key"
        
        // Новые Keychain keys для отдельных ключей групп провайдеров
        private const val KEYCHAIN_KEY_OPENAI_API_KEY = "com.family.tree.ai_openai_api_key"
        private const val KEYCHAIN_KEY_ANTHROPIC_API_KEY = "com.family.tree.ai_anthropic_api_key"
        private const val KEYCHAIN_KEY_GOOGLE_AI_API_KEY = "com.family.tree.ai_google_ai_api_key"
    }
}
