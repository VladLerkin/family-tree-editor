package com.family.tree.core.genealogy.matching

import com.family.tree.core.ai.AiClient
import com.family.tree.core.ai.AiConfig
import com.family.tree.core.genealogy.PersonMatchingConfig
import com.family.tree.core.genealogy.model.ExternalPerson
import com.family.tree.core.model.Individual
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

/**
 * Сопоставление персон с использованием AI (LLM).
 * Использует уже подключённые LLM для более точного сравнения.
 */
class AiPersonMatcher(
    private val aiClient: AiClient,
    private val aiConfig: AiConfig,
    private val fallbackMatcher: PersonMatcher = BasicPersonMatcher()
) : PersonMatcher {
    
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
    
    override suspend fun match(
        localPerson: Individual,
        externalPerson: ExternalPerson,
        config: PersonMatchingConfig
    ): PersonMatchResult {
        if (!config.useAiMatching) {
            return fallbackMatcher.match(localPerson, externalPerson, config)
        }
        
        return try {
            val prompt = buildMatchingPrompt(localPerson, externalPerson, config)
            val response = aiClient.sendPrompt(prompt, aiConfig)
            
            parseAiMatchingResponse(response, localPerson, externalPerson)
        } catch (e: Exception) {
            // Fallback на базовое сравнение при ошибке AI
            fallbackMatcher.match(localPerson, externalPerson, config)
        }
    }
    
    override suspend fun findBestMatches(
        localPerson: Individual,
        externalPersons: List<ExternalPerson>,
        config: PersonMatchingConfig,
        topN: Int
    ): List<PersonMatchResult> {
        if (!config.useAiMatching || externalPersons.isEmpty()) {
            return fallbackMatcher.findBestMatches(localPerson, externalPersons, config, topN)
        }
        
        return try {
            val prompt = buildBatchMatchingPrompt(localPerson, externalPersons, config)
            val response = aiClient.sendPrompt(prompt, aiConfig)
            
            parseAiBatchMatchingResponse(response, localPerson, externalPersons)
                .filter { it.confidenceScore >= config.minConfidenceScore }
                .sortedByDescending { it.confidenceScore }
                .take(topN)
        } catch (e: Exception) {
            // Fallback на базовое сравнение при ошибке AI
            fallbackMatcher.findBestMatches(localPerson, externalPersons, config, topN)
        }
    }
    
    /**
     * Построение промпта для сравнения одной персоны.
     */
    private fun buildMatchingPrompt(
        localPerson: Individual,
        externalPerson: ExternalPerson,
        config: PersonMatchingConfig
    ): String {
        return """
Ты - эксперт по генеалогии. Твоя задача - сравнить две записи о персоне и определить, являются ли они одним и тем же человеком.

**Локальная запись (из семейного дерева):**
- Имя: ${localPerson.firstName}
- Фамилия: ${localPerson.lastName}
- Пол: ${localPerson.gender?.name ?: "неизвестно"}
- Год рождения: ${localPerson.birthYear ?: "неизвестно"}
- Год смерти: ${localPerson.deathYear ?: "неизвестно"}

**Внешняя запись (из ${externalPerson.sourceProvider}):**
- Имя: ${externalPerson.firstName} ${externalPerson.middleName}
- Фамилия: ${externalPerson.lastName}
- Девичья фамилия: ${externalPerson.maidenName ?: "неизвестно"}
- Пол: ${externalPerson.gender ?: "неизвестно"}
- Дата рождения: ${externalPerson.birthDate?.original ?: "неизвестно"}
- Место рождения: ${externalPerson.birthPlace?.original ?: "неизвестно"}
- Дата смерти: ${externalPerson.deathDate?.original ?: "неизвестно"}
- Место смерти: ${externalPerson.deathPlace?.original ?: "неизвестно"}

Проанализируй эти данные и верни результат в формате JSON:

{
  "confidenceScore": <число от 0.0 до 1.0>,
  "matchType": "<EXACT|HIGH|MEDIUM|LOW|NO_MATCH>",
  "matchedFields": [
    {
      "fieldName": "<название поля>",
      "similarity": <число от 0.0 до 1.0>,
      "comment": "<комментарий>"
    }
  ],
  "conflicts": [
    {
      "fieldName": "<название поля>",
      "severity": "<CRITICAL|MAJOR|MINOR>",
      "resolution": "<предложение по разрешению>"
    }
  ],
  "explanation": "<подробное объяснение>",
  "suggestions": ["<предложение 1>", "<предложение 2>"]
}

Учитывай:
- Возможные опечатки и разные написания имён
- Приблизительные даты
- Исторические изменения названий мест
- Культурные особенности написания имён
- Девичьи фамилии для женщин

Верни ТОЛЬКО JSON, без дополнительного текста.
        """.trimIndent()
    }
    
    /**
     * Построение промпта для пакетного сравнения.
     */
    private fun buildBatchMatchingPrompt(
        localPerson: Individual,
        externalPersons: List<ExternalPerson>,
        config: PersonMatchingConfig
    ): String {
        val externalPersonsText = externalPersons.mapIndexed { index, person ->
            """
**Кандидат ${index + 1}:**
- ID: ${person.externalId}
- Имя: ${person.firstName} ${person.middleName}
- Фамилия: ${person.lastName}
- Пол: ${person.gender ?: "неизвестно"}
- Год рождения: ${person.birthDate?.year ?: "неизвестно"}
- Место рождения: ${person.birthPlace?.original ?: "неизвестно"}
- Год смерти: ${person.deathDate?.year ?: "неизвестно"}
            """.trimIndent()
        }.joinToString("\n\n")
        
        return """
Ты - эксперт по генеалогии. Твоя задача - найти наиболее подходящие совпадения для персоны из списка кандидатов.

**Искомая персона:**
- Имя: ${localPerson.firstName}
- Фамилия: ${localPerson.lastName}
- Пол: ${localPerson.gender?.name ?: "неизвестно"}
- Год рождения: ${localPerson.birthYear ?: "неизвестно"}
- Год смерти: ${localPerson.deathYear ?: "неизвестно"}

**Кандидаты:**
$externalPersonsText

Проанализируй каждого кандидата и верни результат в формате JSON:

{
  "matches": [
    {
      "externalId": "<ID кандидата>",
      "confidenceScore": <число от 0.0 до 1.0>,
      "matchType": "<EXACT|HIGH|MEDIUM|LOW|NO_MATCH>",
      "explanation": "<краткое объяснение>"
    }
  ]
}

Отсортируй результаты по убыванию confidenceScore.
Верни ТОЛЬКО JSON, без дополнительного текста.
        """.trimIndent()
    }
    
    /**
     * Парсинг ответа AI для одной персоны.
     */
    private fun parseAiMatchingResponse(
        response: String,
        localPerson: Individual,
        externalPerson: ExternalPerson
    ): PersonMatchResult {
        val jsonResponse = json.parseToJsonElement(response).jsonObject
        
        val aiResult = json.decodeFromJsonElement(AiMatchingResponse.serializer(), jsonResponse)
        
        val matchedFields = aiResult.matchedFields.map { field ->
            MatchedField(
                fieldName = field.fieldName,
                localValue = "",
                externalValue = "",
                similarity = field.similarity
            )
        }
        
        val conflicts = aiResult.conflicts.map { conflict ->
            FieldConflict(
                fieldName = conflict.fieldName,
                localValue = "",
                externalValue = "",
                severity = ConflictSeverity.valueOf(conflict.severity),
                resolution = conflict.resolution
            )
        }
        
        return PersonMatchResult(
            localPerson = localPerson.id.value,
            externalPerson = externalPerson.externalId,
            confidenceScore = aiResult.confidenceScore,
            matchType = MatchType.valueOf(aiResult.matchType),
            matchedFields = matchedFields,
            conflicts = conflicts,
            aiExplanation = aiResult.explanation,
            suggestions = aiResult.suggestions
        )
    }
    
    /**
     * Парсинг ответа AI для пакетного сравнения.
     */
    private fun parseAiBatchMatchingResponse(
        response: String,
        localPerson: Individual,
        externalPersons: List<ExternalPerson>
    ): List<PersonMatchResult> {
        val jsonResponse = json.parseToJsonElement(response).jsonObject
        val aiResult = json.decodeFromJsonElement(AiBatchMatchingResponse.serializer(), jsonResponse)
        
        return aiResult.matches.map { match ->
            PersonMatchResult(
                localPerson = localPerson.id.value,
                externalPerson = match.externalId,
                confidenceScore = match.confidenceScore,
                matchType = MatchType.valueOf(match.matchType),
                matchedFields = emptyList(),
                conflicts = emptyList(),
                aiExplanation = match.explanation
            )
        }
    }
}

/**
 * Структура ответа AI для одной персоны.
 */
@Serializable
private data class AiMatchingResponse(
    val confidenceScore: Double,
    val matchType: String,
    val matchedFields: List<AiMatchedField> = emptyList(),
    val conflicts: List<AiConflict> = emptyList(),
    val explanation: String = "",
    val suggestions: List<String> = emptyList()
)

@Serializable
private data class AiMatchedField(
    val fieldName: String,
    val similarity: Double,
    val comment: String = ""
)

@Serializable
private data class AiConflict(
    val fieldName: String,
    val severity: String,
    val resolution: String = ""
)

/**
 * Структура ответа AI для пакетного сравнения.
 */
@Serializable
private data class AiBatchMatchingResponse(
    val matches: List<AiBatchMatch>
)

@Serializable
private data class AiBatchMatch(
    val externalId: String,
    val confidenceScore: Double,
    val matchType: String,
    val explanation: String = ""
)
