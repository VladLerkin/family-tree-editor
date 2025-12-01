package com.family.tree.core.genealogy

import com.family.tree.core.ai.AiClient
import com.family.tree.core.ai.AiConfig
import com.family.tree.core.genealogy.client.FamilySearchClient
import com.family.tree.core.genealogy.client.GenealogyApiClient
import com.family.tree.core.genealogy.matching.AiPersonMatcher
import com.family.tree.core.genealogy.matching.BasicPersonMatcher
import com.family.tree.core.genealogy.matching.PersonMatcher
import com.family.tree.core.genealogy.matching.PersonMatchResult
import com.family.tree.core.genealogy.model.ExternalPerson
import com.family.tree.core.genealogy.model.PersonSearchCriteria
import com.family.tree.core.genealogy.model.PersonSearchResult
import com.family.tree.core.genealogy.parser.GenealogyParserFactory
import com.family.tree.core.model.Individual
import io.ktor.client.*

/**
 * Основной сервис для работы с генеалогическими API.
 * Координирует работу клиентов, парсеров и сопоставления персон.
 */
class GenealogyApiService(
    private val httpClient: HttpClient,
    private val aiClient: AiClient? = null,
    private val aiConfig: AiConfig? = null
) {
    private val clients = mutableMapOf<String, GenealogyApiClient>()
    private val configs = mutableMapOf<String, GenealogyApiConfig>()
    
    /**
     * Регистрация провайдера API.
     */
    fun registerProvider(providerName: String, config: GenealogyApiConfig) {
        configs[providerName] = config
        
        // Создание клиента в зависимости от провайдера
        val client = when (config.getProvider()) {
            GenealogyApiProvider.FAMILYSEARCH -> FamilySearchClient(config, httpClient)
            // TODO: Добавить другие провайдеры
            else -> FamilySearchClient(config, httpClient)
        }
        
        clients[providerName] = client
    }
    
    /**
     * Удаление провайдера.
     */
    fun unregisterProvider(providerName: String) {
        clients.remove(providerName)
        configs.remove(providerName)
    }
    
    /**
     * Получение списка зарегистрированных провайдеров.
     */
    fun getRegisteredProviders(): List<String> {
        return clients.keys.toList()
    }
    
    /**
     * Поиск персоны во всех зарегистрированных провайдерах.
     */
    suspend fun searchInAllProviders(
        criteria: PersonSearchCriteria
    ): Map<String, Result<PersonSearchResult>> {
        val results = mutableMapOf<String, Result<PersonSearchResult>>()
        
        for ((providerName, client) in clients) {
            val config = configs[providerName] ?: continue
            if (!config.enabled) continue
            
            results[providerName] = client.searchPersons(criteria)
        }
        
        return results
    }
    
    /**
     * Поиск персоны в конкретном провайдере.
     */
    suspend fun searchInProvider(
        providerName: String,
        criteria: PersonSearchCriteria
    ): Result<PersonSearchResult> {
        val client = clients[providerName]
            ?: return Result.failure(IllegalArgumentException("Provider $providerName not registered"))
        
        return client.searchPersons(criteria)
    }
    
    /**
     * Получение детальной информации о персоне.
     */
    suspend fun getPersonDetails(
        providerName: String,
        externalId: String
    ): Result<ExternalPerson> {
        val client = clients[providerName]
            ?: return Result.failure(IllegalArgumentException("Provider $providerName not registered"))
        
        return client.getPersonById(externalId)
    }
    
    /**
     * Поиск совпадений для локальной персоны во всех провайдерах.
     */
    suspend fun findMatchesForPerson(
        localPerson: Individual,
        matchingConfig: PersonMatchingConfig = PersonMatchingConfig(),
        maxResultsPerProvider: Int = 5
    ): Map<String, List<PersonMatchResult>> {
        val results = mutableMapOf<String, List<PersonMatchResult>>()
        
        // Создание критериев поиска из локальной персоны
        val criteria = PersonSearchCriteria(
            firstName = localPerson.firstName,
            lastName = localPerson.lastName,
            birthYear = localPerson.birthYear,
            deathYear = localPerson.deathYear,
            maxResults = 20
        )
        
        // Создание matcher'а
        val matcher = createMatcher(matchingConfig)
        
        // Поиск в каждом провайдере
        for ((providerName, client) in clients) {
            val config = configs[providerName] ?: continue
            if (!config.enabled) continue
            
            val searchResult = client.searchPersons(criteria)
            
            searchResult.onSuccess { result ->
                val matches = matcher.findBestMatches(
                    localPerson,
                    result.persons,
                    matchingConfig,
                    maxResultsPerProvider
                )
                
                if (matches.isNotEmpty()) {
                    results[providerName] = matches
                }
            }
        }
        
        return results
    }
    
    /**
     * Сравнение локальной персоны с внешней.
     */
    suspend fun matchPerson(
        localPerson: Individual,
        externalPerson: ExternalPerson,
        matchingConfig: PersonMatchingConfig = PersonMatchingConfig()
    ): PersonMatchResult {
        val matcher = createMatcher(matchingConfig)
        return matcher.match(localPerson, externalPerson, matchingConfig)
    }
    
    /**
     * Проверка подключения к провайдеру.
     */
    suspend fun testProvider(providerName: String): Result<Boolean> {
        val client = clients[providerName]
            ?: return Result.failure(IllegalArgumentException("Provider $providerName not registered"))
        
        return client.testConnection()
    }
    
    /**
     * Получение информации о лимитах провайдера.
     */
    suspend fun getProviderRateLimit(providerName: String): Result<com.family.tree.core.genealogy.client.RateLimitInfo> {
        val client = clients[providerName]
            ?: return Result.failure(IllegalArgumentException("Provider $providerName not registered"))
        
        return client.getRateLimitInfo()
    }
    
    /**
     * Создание matcher'а в зависимости от конфигурации.
     */
    private fun createMatcher(config: PersonMatchingConfig): PersonMatcher {
        return if (config.useAiMatching && aiClient != null && aiConfig != null) {
            AiPersonMatcher(aiClient, aiConfig)
        } else {
            BasicPersonMatcher()
        }
    }
    
    /**
     * Парсинг данных из внешнего формата.
     */
    fun parseExternalData(data: String, format: String): Result<List<ExternalPerson>> {
        val parser = GenealogyParserFactory.createParser(format)
        return parser.parse(data)
    }
}

/**
 * Builder для создания GenealogyApiService.
 */
class GenealogyApiServiceBuilder {
    private var httpClient: HttpClient? = null
    private var aiClient: AiClient? = null
    private var aiConfig: AiConfig? = null
    private val providers = mutableListOf<Pair<String, GenealogyApiConfig>>()
    
    fun withHttpClient(client: HttpClient) = apply {
        this.httpClient = client
    }
    
    fun withAiClient(client: AiClient, config: AiConfig) = apply {
        this.aiClient = client
        this.aiConfig = config
    }
    
    fun addProvider(name: String, config: GenealogyApiConfig) = apply {
        providers.add(name to config)
    }
    
    fun build(): GenealogyApiService {
        val client = httpClient ?: throw IllegalStateException("HttpClient is required")
        val service = GenealogyApiService(client, aiClient, aiConfig)
        
        providers.forEach { (name, config) ->
            service.registerProvider(name, config)
        }
        
        return service
    }
}
