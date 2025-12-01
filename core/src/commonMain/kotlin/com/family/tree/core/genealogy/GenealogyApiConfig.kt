package com.family.tree.core.genealogy

import kotlinx.serialization.Serializable

/**
 * Типы поддерживаемых генеалогических API провайдеров.
 */
enum class GenealogyApiProvider {
    FAMILYSEARCH,    // FamilySearch API (GEDCOM X)
    ANCESTRY,        // Ancestry.com API
    MYHERITAGE,      // MyHeritage API
    GENI,            // Geni.com API
    WIKITREE,        // WikiTree API
    CUSTOM           // Пользовательский API
}

/**
 * Формат данных, возвращаемый API.
 */
enum class GenealogyDataFormat {
    GEDCOM_X,        // GEDCOM X (JSON)
    GEDCOM_5_5,      // GEDCOM 5.5 (текстовый формат)
    JSON_CUSTOM,     // Кастомный JSON формат
    XML              // XML формат
}

/**
 * Конфигурация для подключения к генеалогическому API.
 */
@Serializable
data class GenealogyApiConfig(
    val provider: String = "FAMILYSEARCH",
    val apiKey: String = "",
    val accessToken: String = "",           // OAuth токен для провайдеров с OAuth
    val baseUrl: String = "",               // Базовый URL для CUSTOM провайдера
    val dataFormat: String = "GEDCOM_X",    // Формат данных
    val enabled: Boolean = false,            // Включен ли провайдер
    val rateLimitPerMinute: Int = 60        // Лимит запросов в минуту
) {
    fun getProvider(): GenealogyApiProvider = try {
        GenealogyApiProvider.valueOf(provider)
    } catch (e: Exception) {
        GenealogyApiProvider.FAMILYSEARCH
    }
    
    fun getDataFormat(): GenealogyDataFormat = try {
        GenealogyDataFormat.valueOf(dataFormat)
    } catch (e: Exception) {
        GenealogyDataFormat.GEDCOM_X
    }
}

/**
 * Настройки для сравнения персон с помощью AI.
 */
@Serializable
data class PersonMatchingConfig(
    val minConfidenceScore: Double = 0.7,   // Минимальный порог уверенности для совпадения (0.0 - 1.0)
    val useAiMatching: Boolean = true,       // Использовать ли AI для сравнения
    val matchingFields: List<String> = listOf(
        "firstName", "lastName", "birthDate", "birthPlace", 
        "deathDate", "deathPlace", "parents", "spouses"
    ),
    val fuzzyMatchingEnabled: Boolean = true // Нечёткое сравнение имён
)

/**
 * Предустановленные конфигурации для популярных генеалогических API.
 */
object GenealogyApiPresets {
    val FAMILYSEARCH = GenealogyApiConfig(
        provider = "FAMILYSEARCH",
        baseUrl = "https://api.familysearch.org",
        dataFormat = "GEDCOM_X",
        rateLimitPerMinute = 60
    )
    
    val ANCESTRY = GenealogyApiConfig(
        provider = "ANCESTRY",
        baseUrl = "https://api.ancestry.com",
        dataFormat = "JSON_CUSTOM",
        rateLimitPerMinute = 100
    )
    
    val MYHERITAGE = GenealogyApiConfig(
        provider = "MYHERITAGE",
        baseUrl = "https://api.myheritage.com",
        dataFormat = "GEDCOM_X",
        rateLimitPerMinute = 60
    )
    
    val WIKITREE = GenealogyApiConfig(
        provider = "WIKITREE",
        baseUrl = "https://api.wikitree.com",
        dataFormat = "JSON_CUSTOM",
        rateLimitPerMinute = 120
    )
    
    fun getAllPresets(): List<Pair<String, GenealogyApiConfig>> = listOf(
        "FamilySearch (GEDCOM X)" to FAMILYSEARCH,
        "Ancestry.com" to ANCESTRY,
        "MyHeritage" to MYHERITAGE,
        "WikiTree" to WIKITREE
    )
}
