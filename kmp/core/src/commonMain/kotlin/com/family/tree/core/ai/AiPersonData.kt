package com.family.tree.core.ai

import kotlinx.serialization.Serializable

/**
 * Данные о персоне, извлечённые из текста с помощью AI.
 */
@Serializable
data class AiPersonData(
    val firstName: String = "",
    val lastName: String = "",
    val gender: String? = null,  // "MALE", "FEMALE", "UNKNOWN"
    val birthYear: Int? = null,
    val deathYear: Int? = null,
    val notes: String = ""
)

/**
 * Родственная связь между двумя персонами.
 */
@Serializable
data class AiRelationship(
    val personIndex: Int,  // Индекс персоны в списке persons
    val relatedPersonIndex: Int,  // Индекс связанной персоны
    val relationType: String  // "PARENT", "CHILD", "SPOUSE"
)

/**
 * Результат обработки текста AI: список персон и их связей.
 */
@Serializable
data class AiAnalysisResult(
    val persons: List<AiPersonData> = emptyList(),
    val relationships: List<AiRelationship> = emptyList()
)
