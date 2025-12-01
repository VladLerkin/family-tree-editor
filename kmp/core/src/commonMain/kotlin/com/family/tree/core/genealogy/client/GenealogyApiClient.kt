package com.family.tree.core.genealogy.client

import com.family.tree.core.genealogy.GenealogyApiConfig
import com.family.tree.core.genealogy.model.ExternalPerson
import com.family.tree.core.genealogy.model.PersonSearchCriteria
import com.family.tree.core.genealogy.model.PersonSearchResult

/**
 * Интерфейс для работы с генеалогическими API.
 * Каждый провайдер должен реализовать этот интерфейс.
 */
interface GenealogyApiClient {
    
    /**
     * Поиск персон по критериям.
     */
    suspend fun searchPersons(criteria: PersonSearchCriteria): Result<PersonSearchResult>
    
    /**
     * Получение детальной информации о персоне по ID.
     */
    suspend fun getPersonById(externalId: String): Result<ExternalPerson>
    
    /**
     * Получение родословной персоны (предки и потомки).
     */
    suspend fun getPersonPedigree(
        externalId: String, 
        generations: Int = 3
    ): Result<List<ExternalPerson>>
    
    /**
     * Проверка доступности API и валидности учётных данных.
     */
    suspend fun testConnection(): Result<Boolean>
    
    /**
     * Получение информации о лимитах API.
     */
    suspend fun getRateLimitInfo(): Result<RateLimitInfo>
}

/**
 * Информация о лимитах API.
 */
data class RateLimitInfo(
    val limit: Int,                    // Максимальное количество запросов
    val remaining: Int,                // Оставшееся количество запросов
    val resetTime: Long                // Время сброса лимита (Unix timestamp)
)

/**
 * Базовый класс для реализации клиентов генеалогических API.
 */
abstract class BaseGenealogyApiClient(
    protected val config: GenealogyApiConfig
) : GenealogyApiClient {
    
    /**
     * Выполнение HTTP запроса с обработкой ошибок и rate limiting.
     */
    protected suspend fun <T> executeRequest(
        block: suspend () -> T
    ): Result<T> {
        return try {
            // TODO: Добавить rate limiting
            // TODO: Добавить retry логику
            Result.success(block())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Получение заголовков авторизации.
     */
    protected open fun getAuthHeaders(): Map<String, String> {
        return if (config.apiKey.isNotBlank()) {
            mapOf("Authorization" to "Bearer ${config.apiKey}")
        } else if (config.accessToken.isNotBlank()) {
            mapOf("Authorization" to "Bearer ${config.accessToken}")
        } else {
            emptyMap()
        }
    }
}
