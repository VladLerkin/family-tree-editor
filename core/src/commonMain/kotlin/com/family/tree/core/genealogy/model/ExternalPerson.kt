package com.family.tree.core.genealogy.model

import kotlinx.serialization.Serializable

/**
 * Унифицированная модель персоны из внешнего генеалогического API.
 * Используется как промежуточное представление данных из разных источников.
 */
@Serializable
data class ExternalPerson(
    val externalId: String,                          // ID в системе источника
    val sourceProvider: String,                       // Провайдер (FAMILYSEARCH, ANCESTRY и т.д.)
    val sourceUrl: String? = null,                   // URL на страницу персоны в источнике
    
    // Основные данные
    val firstName: String = "",
    val middleName: String = "",
    val lastName: String = "",
    val maidenName: String? = null,
    val gender: String? = null,                      // "MALE", "FEMALE", "UNKNOWN"
    
    // События жизни
    val birthDate: ExternalDate? = null,
    val birthPlace: ExternalPlace? = null,
    val deathDate: ExternalDate? = null,
    val deathPlace: ExternalPlace? = null,
    val burialDate: ExternalDate? = null,
    val burialPlace: ExternalPlace? = null,
    
    // Другие события
    val events: List<ExternalEvent> = emptyList(),
    
    // Родственные связи
    val parents: List<ExternalPersonReference> = emptyList(),
    val spouses: List<ExternalPersonReference> = emptyList(),
    val children: List<ExternalPersonReference> = emptyList(),
    
    // Дополнительная информация
    val notes: List<String> = emptyList(),
    val sources: List<ExternalSource> = emptyList(),
    val confidence: Double? = null,                  // Уровень уверенности источника в данных (0.0 - 1.0)
    
    // Метаданные
    val lastModified: String? = null,                // ISO 8601 дата последнего изменения
    val rawData: String? = null                      // Сырые данные в оригинальном формате (для отладки)
)

/**
 * Дата из внешнего источника.
 */
@Serializable
data class ExternalDate(
    val original: String,                            // Оригинальная строка даты
    val normalized: String? = null,                  // Нормализованная дата (ISO 8601)
    val year: Int? = null,
    val month: Int? = null,
    val day: Int? = null,
    val isApproximate: Boolean = false,              // Приблизительная дата
    val isRange: Boolean = false,                    // Диапазон дат
    val rangeEnd: String? = null                     // Конец диапазона
)

/**
 * Место из внешнего источника.
 */
@Serializable
data class ExternalPlace(
    val original: String,                            // Оригинальная строка места
    val normalized: String? = null,                  // Нормализованное название
    val city: String? = null,
    val region: String? = null,
    val country: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
)

/**
 * Событие из внешнего источника.
 */
@Serializable
data class ExternalEvent(
    val type: String,                                // Тип события (BIRTH, DEATH, MARRIAGE, CENSUS и т.д.)
    val date: ExternalDate? = null,
    val place: ExternalPlace? = null,
    val description: String? = null,
    val sources: List<ExternalSource> = emptyList()
)

/**
 * Ссылка на другую персону.
 */
@Serializable
data class ExternalPersonReference(
    val externalId: String,
    val name: String? = null,
    val relationshipType: String? = null             // Тип связи (PARENT, CHILD, SPOUSE)
)

/**
 * Источник информации.
 */
@Serializable
data class ExternalSource(
    val title: String,
    val citation: String? = null,
    val url: String? = null,
    val repository: String? = null
)

/**
 * Результат поиска персон во внешнем API.
 */
@Serializable
data class PersonSearchResult(
    val persons: List<ExternalPerson> = emptyList(),
    val totalResults: Int = 0,
    val hasMore: Boolean = false,
    val nextPageToken: String? = null,
    val provider: String,
    val queryTime: Long = 0                          // Время выполнения запроса в мс
)

/**
 * Критерии поиска персоны.
 */
@Serializable
data class PersonSearchCriteria(
    val firstName: String? = null,
    val lastName: String? = null,
    val birthYear: Int? = null,
    val birthYearRange: Int = 5,                     // Диапазон поиска по году рождения (+/-)
    val birthPlace: String? = null,
    val deathYear: Int? = null,
    val deathYearRange: Int = 5,
    val deathPlace: String? = null,
    val gender: String? = null,
    val maxResults: Int = 20
)
