package com.family.tree.core.genealogy.client

import com.family.tree.core.genealogy.GenealogyApiConfig
import com.family.tree.core.genealogy.model.*
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Клиент для работы с FamilySearch API.
 * Документация: https://www.familysearch.org/developers/docs/api/
 */
class FamilySearchClient(
    config: GenealogyApiConfig,
    private val httpClient: HttpClient
) : BaseGenealogyApiClient(config) {
    
    private val json = Json { 
        ignoreUnknownKeys = true
        isLenient = true
    }
    
    private val baseUrl = config.baseUrl.ifBlank { "https://api.familysearch.org" }
    
    override suspend fun searchPersons(criteria: PersonSearchCriteria): Result<PersonSearchResult> {
        return executeRequest {
            val queryParams = buildSearchQuery(criteria)
            val response = httpClient.get("$baseUrl/platform/tree/search") {
                url {
                    queryParams.forEach { (key, value) ->
                        parameters.append(key, value)
                    }
                }
                headers {
                    getAuthHeaders().forEach { (key, value) ->
                        append(key, value)
                    }
                    append("Accept", "application/x-gedcomx-v1+json")
                }
            }
            
            parseSearchResponse(response.bodyAsText())
        }
    }
    
    override suspend fun getPersonById(externalId: String): Result<ExternalPerson> {
        return executeRequest {
            val response = httpClient.get("$baseUrl/platform/tree/persons/$externalId") {
                headers {
                    getAuthHeaders().forEach { (key, value) ->
                        append(key, value)
                    }
                    append("Accept", "application/x-gedcomx-v1+json")
                }
            }
            
            parsePersonResponse(response.bodyAsText())
        }
    }
    
    override suspend fun getPersonPedigree(
        externalId: String,
        generations: Int
    ): Result<List<ExternalPerson>> {
        return executeRequest {
            val response = httpClient.get("$baseUrl/platform/tree/ancestry") {
                url {
                    parameters.append("person", externalId)
                    parameters.append("generations", generations.toString())
                }
                headers {
                    getAuthHeaders().forEach { (key, value) ->
                        append(key, value)
                    }
                    append("Accept", "application/x-gedcomx-v1+json")
                }
            }
            
            parsePedigreeResponse(response.bodyAsText())
        }
    }
    
    override suspend fun testConnection(): Result<Boolean> {
        return executeRequest {
            val response = httpClient.get("$baseUrl/platform/collection") {
                headers {
                    getAuthHeaders().forEach { (key, value) ->
                        append(key, value)
                    }
                }
            }
            response.status == HttpStatusCode.OK
        }
    }
    
    override suspend fun getRateLimitInfo(): Result<RateLimitInfo> {
        return executeRequest {
            // FamilySearch обычно возвращает информацию о rate limit в заголовках
            // Используем простое значение, так как точное время не критично для примера
            RateLimitInfo(
                limit = config.rateLimitPerMinute,
                remaining = config.rateLimitPerMinute,
                resetTime = 0L // TODO: Получать из заголовков ответа
            )
        }
    }
    
    /**
     * Построение поискового запроса из критериев.
     */
    private fun buildSearchQuery(criteria: PersonSearchCriteria): Map<String, String> {
        val params = mutableMapOf<String, String>()
        
        criteria.firstName?.let { params["givenName"] = it }
        criteria.lastName?.let { params["surname"] = it }
        
        if (criteria.birthYear != null) {
            val range = criteria.birthYearRange
            params["birthDate"] = "${criteria.birthYear - range}-${criteria.birthYear + range}"
        }
        criteria.birthPlace?.let { params["birthPlace"] = it }
        
        if (criteria.deathYear != null) {
            val range = criteria.deathYearRange
            params["deathDate"] = "${criteria.deathYear - range}-${criteria.deathYear + range}"
        }
        criteria.deathPlace?.let { params["deathPlace"] = it }
        
        criteria.gender?.let { params["gender"] = it }
        params["count"] = criteria.maxResults.toString()
        
        return params
    }
    
    /**
     * Парсинг ответа поиска из GEDCOM X формата.
     */
    private fun parseSearchResponse(responseBody: String): PersonSearchResult {
        val jsonElement = json.parseToJsonElement(responseBody).jsonObject
        val entries = jsonElement["entries"]?.jsonArray ?: return PersonSearchResult(
            persons = emptyList(),
            totalResults = 0,
            provider = "FAMILYSEARCH"
        )
        
        val persons = entries.mapNotNull { entry ->
            try {
                parseGedcomXPerson(entry.jsonObject)
            } catch (e: Exception) {
                null
            }
        }
        
        return PersonSearchResult(
            persons = persons,
            totalResults = jsonElement["results"]?.jsonPrimitive?.content?.toIntOrNull() ?: persons.size,
            hasMore = persons.size >= 20,
            provider = "FAMILYSEARCH",
            queryTime = 0
        )
    }
    
    /**
     * Парсинг одной персоны из GEDCOM X формата.
     */
    private fun parsePersonResponse(responseBody: String): ExternalPerson {
        val jsonElement = json.parseToJsonElement(responseBody).jsonObject
        val persons = jsonElement["persons"]?.jsonArray
        
        if (persons.isNullOrEmpty()) {
            throw IllegalStateException("No person data in response")
        }
        
        return parseGedcomXPerson(persons[0].jsonObject)
    }
    
    /**
     * Парсинг родословной из GEDCOM X формата.
     */
    private fun parsePedigreeResponse(responseBody: String): List<ExternalPerson> {
        val jsonElement = json.parseToJsonElement(responseBody).jsonObject
        val persons = jsonElement["persons"]?.jsonArray ?: return emptyList()
        
        return persons.mapNotNull { person ->
            try {
                parseGedcomXPerson(person.jsonObject)
            } catch (e: Exception) {
                null
            }
        }
    }
    
    /**
     * Парсинг персоны из GEDCOM X JSON объекта.
     * TODO: Полная реализация парсинга GEDCOM X формата
     */
    private fun parseGedcomXPerson(personJson: kotlinx.serialization.json.JsonObject): ExternalPerson {
        val id = personJson["id"]?.jsonPrimitive?.content ?: ""
        
        // Упрощённый парсинг для примера
        // В реальной реализации нужно полностью парсить GEDCOM X структуру
        return ExternalPerson(
            externalId = id,
            sourceProvider = "FAMILYSEARCH",
            sourceUrl = "https://www.familysearch.org/tree/person/$id",
            firstName = "",  // TODO: Извлечь из names
            lastName = "",   // TODO: Извлечь из names
            rawData = personJson.toString()
        )
    }
}
