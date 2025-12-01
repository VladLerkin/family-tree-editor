package com.family.tree.core.genealogy.matching

import com.family.tree.core.genealogy.PersonMatchingConfig
import com.family.tree.core.genealogy.model.ExternalPerson
import com.family.tree.core.model.Individual
import kotlinx.serialization.Serializable

/**
 * Результат сравнения персоны из дерева с персоной из внешнего источника.
 */
@Serializable
data class PersonMatchResult(
    val localPerson: String,                    // ID локальной персоны
    val externalPerson: String,                 // ID внешней персоны
    val confidenceScore: Double,                // Оценка уверенности (0.0 - 1.0)
    val matchType: MatchType,                   // Тип совпадения
    val matchedFields: List<MatchedField>,      // Совпавшие поля
    val conflicts: List<FieldConflict>,         // Конфликты в данных
    val aiExplanation: String? = null,          // Объяснение от AI
    val suggestions: List<String> = emptyList() // Предложения по улучшению данных
)

/**
 * Тип совпадения.
 */
enum class MatchType {
    EXACT,          // Точное совпадение
    HIGH,           // Высокая вероятность совпадения
    MEDIUM,         // Средняя вероятность
    LOW,            // Низкая вероятность
    NO_MATCH        // Не совпадает
}

/**
 * Совпавшее поле.
 */
@Serializable
data class MatchedField(
    val fieldName: String,
    val localValue: String,
    val externalValue: String,
    val similarity: Double              // Степень схожести (0.0 - 1.0)
)

/**
 * Конфликт в данных.
 */
@Serializable
data class FieldConflict(
    val fieldName: String,
    val localValue: String,
    val externalValue: String,
    val severity: ConflictSeverity,
    val resolution: String? = null      // Предложение по разрешению конфликта
)

/**
 * Серьёзность конфликта.
 */
enum class ConflictSeverity {
    CRITICAL,       // Критический конфликт (например, разный пол)
    MAJOR,          // Значительный конфликт (например, разные годы рождения)
    MINOR           // Незначительный конфликт (например, разное написание имени)
}

/**
 * Интерфейс для сопоставления персон.
 */
interface PersonMatcher {
    /**
     * Сравнение локальной персоны с внешней.
     */
    suspend fun match(
        localPerson: Individual,
        externalPerson: ExternalPerson,
        config: PersonMatchingConfig
    ): PersonMatchResult
    
    /**
     * Поиск наиболее подходящих совпадений из списка внешних персон.
     */
    suspend fun findBestMatches(
        localPerson: Individual,
        externalPersons: List<ExternalPerson>,
        config: PersonMatchingConfig,
        topN: Int = 5
    ): List<PersonMatchResult>
}

/**
 * Базовый класс для сопоставления персон без использования AI.
 */
class BasicPersonMatcher : PersonMatcher {
    
    override suspend fun match(
        localPerson: Individual,
        externalPerson: ExternalPerson,
        config: PersonMatchingConfig
    ): PersonMatchResult {
        val matchedFields = mutableListOf<MatchedField>()
        val conflicts = mutableListOf<FieldConflict>()
        var totalScore = 0.0
        var fieldCount = 0
        
        // Сравнение имени
        if ("firstName" in config.matchingFields) {
            val similarity = calculateStringSimilarity(
                localPerson.firstName,
                externalPerson.firstName,
                config.fuzzyMatchingEnabled
            )
            matchedFields.add(MatchedField(
                "firstName",
                localPerson.firstName,
                externalPerson.firstName,
                similarity
            ))
            totalScore += similarity
            fieldCount++
        }
        
        // Сравнение фамилии
        if ("lastName" in config.matchingFields) {
            val similarity = calculateStringSimilarity(
                localPerson.lastName,
                externalPerson.lastName,
                config.fuzzyMatchingEnabled
            )
            matchedFields.add(MatchedField(
                "lastName",
                localPerson.lastName,
                externalPerson.lastName,
                similarity
            ))
            totalScore += similarity
            fieldCount++
        }
        
        // Сравнение года рождения
        if ("birthDate" in config.matchingFields && localPerson.birthYear != null && externalPerson.birthDate?.year != null) {
            val yearDiff = kotlin.math.abs(localPerson.birthYear!! - externalPerson.birthDate!!.year!!)
            val similarity = when {
                yearDiff == 0 -> 1.0
                yearDiff <= 2 -> 0.8
                yearDiff <= 5 -> 0.5
                else -> 0.0
            }
            
            matchedFields.add(MatchedField(
                "birthYear",
                localPerson.birthYear.toString(),
                externalPerson.birthDate?.year.toString(),
                similarity
            ))
            
            if (yearDiff > 5) {
                conflicts.add(FieldConflict(
                    "birthYear",
                    localPerson.birthYear.toString(),
                    externalPerson.birthDate?.year.toString(),
                    ConflictSeverity.MAJOR
                ))
            }
            
            totalScore += similarity
            fieldCount++
        }
        
        // Сравнение года смерти
        if ("deathDate" in config.matchingFields && localPerson.deathYear != null && externalPerson.deathDate?.year != null) {
            val yearDiff = kotlin.math.abs(localPerson.deathYear!! - externalPerson.deathDate!!.year!!)
            val similarity = when {
                yearDiff == 0 -> 1.0
                yearDiff <= 2 -> 0.8
                yearDiff <= 5 -> 0.5
                else -> 0.0
            }
            
            matchedFields.add(MatchedField(
                "deathYear",
                localPerson.deathYear.toString(),
                externalPerson.deathDate?.year.toString(),
                similarity
            ))
            
            totalScore += similarity
            fieldCount++
        }
        
        val confidenceScore = if (fieldCount > 0) totalScore / fieldCount else 0.0
        
        val matchType = when {
            confidenceScore >= 0.9 -> MatchType.EXACT
            confidenceScore >= 0.7 -> MatchType.HIGH
            confidenceScore >= 0.5 -> MatchType.MEDIUM
            confidenceScore >= 0.3 -> MatchType.LOW
            else -> MatchType.NO_MATCH
        }
        
        return PersonMatchResult(
            localPerson = localPerson.id.value,
            externalPerson = externalPerson.externalId,
            confidenceScore = confidenceScore,
            matchType = matchType,
            matchedFields = matchedFields,
            conflicts = conflicts
        )
    }
    
    override suspend fun findBestMatches(
        localPerson: Individual,
        externalPersons: List<ExternalPerson>,
        config: PersonMatchingConfig,
        topN: Int
    ): List<PersonMatchResult> {
        return externalPersons
            .map { match(localPerson, it, config) }
            .filter { it.confidenceScore >= config.minConfidenceScore }
            .sortedByDescending { it.confidenceScore }
            .take(topN)
    }
    
    /**
     * Вычисление схожести строк.
     */
    private fun calculateStringSimilarity(
        str1: String,
        str2: String,
        fuzzyMatching: Boolean
    ): Double {
        if (str1.isEmpty() && str2.isEmpty()) return 1.0
        if (str1.isEmpty() || str2.isEmpty()) return 0.0
        
        val s1 = str1.lowercase().trim()
        val s2 = str2.lowercase().trim()
        
        if (s1 == s2) return 1.0
        
        if (!fuzzyMatching) {
            return if (s1 == s2) 1.0 else 0.0
        }
        
        // Простое вычисление схожести на основе расстояния Левенштейна
        val distance = levenshteinDistance(s1, s2)
        val maxLen = maxOf(s1.length, s2.length)
        
        return 1.0 - (distance.toDouble() / maxLen)
    }
    
    /**
     * Расстояние Левенштейна.
     */
    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j
        
        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,      // deletion
                    dp[i][j - 1] + 1,      // insertion
                    dp[i - 1][j - 1] + cost // substitution
                )
            }
        }
        
        return dp[s1.length][s2.length]
    }
}
