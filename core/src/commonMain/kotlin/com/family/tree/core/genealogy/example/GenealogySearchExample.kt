package com.family.tree.core.genealogy.example

import com.family.tree.core.ai.AiClient
import com.family.tree.core.ai.AiConfig
import com.family.tree.core.genealogy.*
import com.family.tree.core.genealogy.matching.MatchType
import com.family.tree.core.genealogy.model.PersonSearchCriteria
import com.family.tree.core.model.Individual
import com.family.tree.core.model.IndividualId
import io.ktor.client.*

/**
 * Пример использования GenealogyApiService для поиска и сопоставления персон.
 */
class GenealogySearchExample(
    private val httpClient: HttpClient,
    private val aiClient: AiClient,
    private val aiConfig: AiConfig
) {
    
    private lateinit var genealogyService: GenealogyApiService
    
    /**
     * Инициализация сервиса с провайдерами.
     */
    fun initialize(familySearchApiKey: String) {
        genealogyService = GenealogyApiServiceBuilder()
            .withHttpClient(httpClient)
            .withAiClient(aiClient, aiConfig)
            .addProvider("familysearch", GenealogyApiConfig(
                provider = "FAMILYSEARCH",
                apiKey = familySearchApiKey,
                dataFormat = "GEDCOM_X",
                enabled = true,
                rateLimitPerMinute = 60
            ))
            .build()
    }
    
    /**
     * Пример 1: Простой поиск персоны по имени и году рождения.
     */
    suspend fun simpleSearch() {
        println("=== Пример 1: Простой поиск ===\n")
        
        val criteria = PersonSearchCriteria(
            firstName = "Иван",
            lastName = "Иванов",
            birthYear = 1850,
            birthYearRange = 5,
            maxResults = 10
        )
        
        val results = genealogyService.searchInAllProviders(criteria)
        
        results.forEach { (provider, result) ->
            result.onSuccess { searchResult ->
                println("Провайдер: $provider")
                println("Найдено: ${searchResult.persons.size} персон")
                println("Время выполнения: ${searchResult.queryTime} мс\n")
                
                searchResult.persons.take(3).forEach { person ->
                    println("  ${person.firstName} ${person.lastName}")
                    println("  Рождение: ${person.birthDate?.original ?: "неизвестно"}")
                    println("  Место: ${person.birthPlace?.original ?: "неизвестно"}")
                    println("  URL: ${person.sourceUrl}")
                    println()
                }
            }
            
            result.onFailure { error ->
                println("Ошибка в $provider: ${error.message}\n")
            }
        }
    }
    
    /**
     * Пример 2: Поиск совпадений для локальной персоны с AI-сопоставлением.
     */
    suspend fun findMatchesWithAi() {
        println("=== Пример 2: Поиск совпадений с AI ===\n")
        
        // Локальная персона из вашего дерева
        val localPerson = Individual(
            id = IndividualId("local-123"),
            firstName = "Александр",
            lastName = "Пушкин",
            birthYear = 1799,
            deathYear = 1837
        )
        
        println("Ищем совпадения для: ${localPerson.displayName}")
        println("Годы жизни: ${localPerson.birthYear}-${localPerson.deathYear}\n")
        
        // Настройки сопоставления с включённым AI
        val matchingConfig = PersonMatchingConfig(
            minConfidenceScore = 0.6,
            useAiMatching = true,
            fuzzyMatchingEnabled = true,
            matchingFields = listOf(
                "firstName", "lastName", 
                "birthDate", "birthPlace",
                "deathDate", "deathPlace"
            )
        )
        
        val matches = genealogyService.findMatchesForPerson(
            localPerson = localPerson,
            matchingConfig = matchingConfig,
            maxResultsPerProvider = 5
        )
        
        matches.forEach { (provider, matchResults) ->
            println("Провайдер: $provider")
            println("Найдено совпадений: ${matchResults.size}\n")
            
            matchResults.forEach { match ->
                println("  Уверенность: ${(match.confidenceScore * 100).toInt()}%")
                println("  Тип: ${match.matchType}")
                
                if (match.aiExplanation != null) {
                    println("  AI объяснение: ${match.aiExplanation}")
                }
                
                if (match.matchedFields.isNotEmpty()) {
                    println("  Совпавшие поля:")
                    match.matchedFields.forEach { field ->
                        println("    - ${field.fieldName}: ${(field.similarity * 100).toInt()}%")
                    }
                }
                
                if (match.conflicts.isNotEmpty()) {
                    println("  Конфликты:")
                    match.conflicts.forEach { conflict ->
                        println("    - ${conflict.fieldName} (${conflict.severity})")
                        println("      Локально: ${conflict.localValue}")
                        println("      Внешне: ${conflict.externalValue}")
                        if (conflict.resolution != null) {
                            println("      Решение: ${conflict.resolution}")
                        }
                    }
                }
                
                if (match.suggestions.isNotEmpty()) {
                    println("  Предложения:")
                    match.suggestions.forEach { suggestion ->
                        println("    - $suggestion")
                    }
                }
                
                println()
            }
        }
    }
    
    /**
     * Пример 3: Детальное сравнение двух конкретных персон.
     */
    suspend fun detailedComparison() {
        println("=== Пример 3: Детальное сравнение ===\n")
        
        val localPerson = Individual(
            id = IndividualId("local-456"),
            firstName = "Мария",
            lastName = "Иванова",
            birthYear = 1875,
            deathYear = 1945
        )
        
        // Сначала ищем кандидатов
        val criteria = PersonSearchCriteria(
            firstName = localPerson.firstName,
            lastName = localPerson.lastName,
            birthYear = localPerson.birthYear,
            birthYearRange = 3,
            maxResults = 5
        )
        
        val searchResult = genealogyService.searchInProvider("familysearch", criteria)
        
        searchResult.onSuccess { result ->
            if (result.persons.isEmpty()) {
                println("Персоны не найдены")
                return
            }
            
            // Берём первого кандидата для детального сравнения
            val externalPerson = result.persons.first()
            
            println("Сравниваем:")
            println("Локальная: ${localPerson.displayName} (${localPerson.birthYear}-${localPerson.deathYear})")
            println("Внешняя: ${externalPerson.firstName} ${externalPerson.lastName} " +
                    "(${externalPerson.birthDate?.year}-${externalPerson.deathDate?.year})")
            println()
            
            val matchingConfig = PersonMatchingConfig(
                useAiMatching = true,
                fuzzyMatchingEnabled = true
            )
            
            val matchResult = genealogyService.matchPerson(
                localPerson = localPerson,
                externalPerson = externalPerson,
                matchingConfig = matchingConfig
            )
            
            println("Результат сопоставления:")
            println("  Уверенность: ${(matchResult.confidenceScore * 100).toInt()}%")
            println("  Тип: ${matchResult.matchType}")
            println()
            
            when (matchResult.matchType) {
                MatchType.EXACT -> println("  ✓ Это точное совпадение!")
                MatchType.HIGH -> println("  ✓ Высокая вероятность совпадения")
                MatchType.MEDIUM -> println("  ? Средняя вероятность, требуется проверка")
                MatchType.LOW -> println("  ⚠ Низкая вероятность совпадения")
                MatchType.NO_MATCH -> println("  ✗ Не совпадает")
            }
            
            if (matchResult.aiExplanation != null) {
                println("\nПодробное объяснение AI:")
                println(matchResult.aiExplanation)
            }
        }
    }
    
    /**
     * Пример 4: Проверка подключения и лимитов.
     */
    suspend fun checkConnectionAndLimits() {
        println("=== Пример 4: Проверка подключения ===\n")
        
        val providers = genealogyService.getRegisteredProviders()
        
        providers.forEach { provider ->
            println("Провайдер: $provider")
            
            // Проверка подключения
            val connectionResult = genealogyService.testProvider(provider)
            connectionResult.onSuccess { isConnected ->
                println("  Подключение: ${if (isConnected) "✓ OK" else "✗ Ошибка"}")
            }
            
            // Проверка лимитов
            val rateLimitResult = genealogyService.getProviderRateLimit(provider)
            rateLimitResult.onSuccess { info ->
                println("  Лимит запросов: ${info.remaining}/${info.limit}")
                val resetIn = if (info.resetTime > 0) (info.resetTime / 1000) else 0
                println("  Сброс через: $resetIn сек")
            }
            
            println()
        }
    }
    
    /**
     * Пример 5: Массовый поиск для списка персон.
     */
    suspend fun batchSearch(persons: List<Individual>) {
        println("=== Пример 5: Массовый поиск ===\n")
        println("Обрабатываем ${persons.size} персон...\n")
        
        val matchingConfig = PersonMatchingConfig(
            minConfidenceScore = 0.7,
            useAiMatching = true
        )
        
        val allMatches = mutableMapOf<String, Int>()
        
        persons.forEach { person ->
            println("Обработка: ${person.displayName}")
            
            val matches = genealogyService.findMatchesForPerson(
                localPerson = person,
                matchingConfig = matchingConfig,
                maxResultsPerProvider = 3
            )
            
            matches.forEach { (provider, matchResults) ->
                val highConfidenceMatches = matchResults.count { 
                    it.confidenceScore >= 0.8 
                }
                
                if (highConfidenceMatches > 0) {
                    println("  $provider: $highConfidenceMatches совпадений с высокой уверенностью")
                    allMatches[provider] = (allMatches[provider] ?: 0) + highConfidenceMatches
                }
            }
            
            println()
        }
        
        println("Итого:")
        allMatches.forEach { (provider, count) ->
            println("  $provider: $count совпадений")
        }
    }
}

/**
 * Запуск примеров.
 */
suspend fun main() {
    val httpClient = HttpClient()
    val aiConfig = AiConfig(
        provider = "OPENAI",
        openaiApiKey = "your-openai-key",
        model = "gpt-4o-mini"
    )
    val aiClient = com.family.tree.core.ai.AiClientFactory.createClient(aiConfig)
    
    
    val example = GenealogySearchExample(httpClient, aiClient, aiConfig)
    
    // Инициализация с вашим FamilySearch API ключом
    example.initialize(familySearchApiKey = "your-familysearch-key")
    
    // Запуск примеров
    example.simpleSearch()
    example.findMatchesWithAi()
    example.detailedComparison()
    example.checkConnectionAndLimits()
    
    // Пример массового поиска
    val testPersons = listOf(
        Individual(
            id = IndividualId("1"),
            firstName = "Лев",
            lastName = "Толстой",
            birthYear = 1828,
            deathYear = 1910
        ),
        Individual(
            id = IndividualId("2"),
            firstName = "Фёдор",
            lastName = "Достоевский",
            birthYear = 1821,
            deathYear = 1881
        )
    )
    
    example.batchSearch(testPersons)
    
    httpClient.close()
}
