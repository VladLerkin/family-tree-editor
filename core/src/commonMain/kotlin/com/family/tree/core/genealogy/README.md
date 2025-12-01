# Интеграция с генеалогическими API

## Обзор архитектуры

Модуль `genealogy` предоставляет унифицированный интерфейс для работы с различными генеалогическими API (FamilySearch, Ancestry, MyHeritage и др.) и интеллектуального сопоставления данных с помощью LLM.

## Архитектура

```
┌─────────────────────────────────────────────────────────────┐
│                   GenealogyApiService                        │
│  (Основной сервис для координации всех операций)            │
└────────────┬────────────────────────────────┬───────────────┘
             │                                │
    ┌────────▼────────┐              ┌───────▼────────┐
    │  API Clients    │              │  PersonMatcher │
    │  (Адаптеры)     │              │  (Сопоставление)│
    └────────┬────────┘              └───────┬────────┘
             │                                │
    ┌────────▼────────────┐          ┌───────▼──────────┐
    │ - FamilySearch     │          │ - BasicMatcher   │
    │ - Ancestry         │          │ - AiMatcher      │
    │ - MyHeritage       │          │   (использует    │
    │ - WikiTree         │          │    LLM)          │
    └────────┬────────────┘          └──────────────────┘
             │
    ┌────────▼────────────┐
    │  Data Parsers       │
    │  (Парсеры форматов) │
    └─────────────────────┘
    │ - GEDCOM X         │
    │ - GEDCOM 5.5       │
    │ - JSON Custom      │
    └─────────────────────┘
```

## Основные компоненты

### 1. **GenealogyApiConfig** - Конфигурация провайдеров
Определяет настройки для подключения к различным API:
- Тип провайдера (FamilySearch, Ancestry и т.д.)
- API ключи и токены авторизации
- Формат данных (GEDCOM X, JSON и т.д.)
- Rate limiting

### 2. **GenealogyApiClient** - Интерфейс клиентов
Базовый интерфейс для всех клиентов API:
- `searchPersons()` - поиск персон по критериям
- `getPersonById()` - получение детальной информации
- `getPersonPedigree()` - получение родословной
- `testConnection()` - проверка подключения

### 3. **ExternalPerson** - Унифицированная модель данных
Промежуточное представление персоны из внешних источников:
- Основные данные (имя, фамилия, пол)
- События жизни (рождение, смерть, брак и т.д.)
- Родственные связи
- Источники информации

### 4. **GenealogyDataParser** - Парсеры форматов
Конвертация различных форматов в `ExternalPerson`:
- **GedcomXParser** - парсинг GEDCOM X (JSON)
- **Gedcom55Parser** - парсинг GEDCOM 5.5 (текстовый)

### 5. **PersonMatcher** - Сопоставление персон
Интеллектуальное сравнение локальных и внешних персон:
- **BasicPersonMatcher** - базовое сравнение (алгоритм Левенштейна)
- **AiPersonMatcher** - AI-powered сравнение с использованием LLM

### 6. **GenealogyApiService** - Основной сервис
Координирует работу всех компонентов:
- Управление провайдерами
- Поиск во множественных источниках
- Автоматическое сопоставление

## Пример использования

### Базовая настройка

```kotlin
import com.family.tree.core.genealogy.*
import com.family.tree.core.genealogy.client.*
import com.family.tree.core.genealogy.model.*
import io.ktor.client.*

// 1. Создание HTTP клиента
val httpClient = HttpClient()

// 2. Создание AI клиента (опционально, для интеллектуального сопоставления)
val aiClient = AiClient(httpClient)
val aiConfig = AiConfig(
    provider = "OPENAI",
    openaiApiKey = "your-api-key",
    model = "gpt-4o-mini"
)

// 3. Создание сервиса
val genealogyService = GenealogyApiServiceBuilder()
    .withHttpClient(httpClient)
    .withAiClient(aiClient, aiConfig)
    .addProvider("familysearch", GenealogyApiConfig(
        provider = "FAMILYSEARCH",
        apiKey = "your-familysearch-key",
        enabled = true
    ))
    .build()
```

### Поиск персоны

```kotlin
// Создание критериев поиска
val criteria = PersonSearchCriteria(
    firstName = "Иван",
    lastName = "Иванов",
    birthYear = 1850,
    birthYearRange = 5,
    birthPlace = "Москва",
    maxResults = 10
)

// Поиск во всех провайдерах
val results = genealogyService.searchInAllProviders(criteria)

results.forEach { (provider, result) ->
    result.onSuccess { searchResult ->
        println("Найдено в $provider: ${searchResult.persons.size} персон")
        searchResult.persons.forEach { person ->
            println("  - ${person.firstName} ${person.lastName} (${person.birthDate?.year})")
        }
    }
}
```

### Сопоставление с локальной персоной

```kotlin
// Локальная персона из дерева
val localPerson = Individual(
    id = IndividualId("123"),
    firstName = "Иван",
    lastName = "Иванов",
    birthYear = 1850,
    deathYear = 1920
)

// Настройки сопоставления
val matchingConfig = PersonMatchingConfig(
    minConfidenceScore = 0.7,
    useAiMatching = true,  // Использовать AI для более точного сравнения
    fuzzyMatchingEnabled = true
)

// Поиск совпадений во всех провайдерах
val matches = genealogyService.findMatchesForPerson(
    localPerson = localPerson,
    matchingConfig = matchingConfig,
    maxResultsPerProvider = 5
)

matches.forEach { (provider, matchResults) ->
    println("Совпадения в $provider:")
    matchResults.forEach { match ->
        println("  - Уверенность: ${match.confidenceScore}")
        println("    Тип: ${match.matchType}")
        println("    Объяснение: ${match.aiExplanation}")
        println("    Конфликты: ${match.conflicts.size}")
    }
}
```

### Детальное сравнение двух персон

```kotlin
// Получение детальной информации о внешней персоне
val externalPersonResult = genealogyService.getPersonDetails(
    providerName = "familysearch",
    externalId = "KWCD-123"
)

externalPersonResult.onSuccess { externalPerson ->
    // Сравнение с локальной персоной
    val matchResult = genealogyService.matchPerson(
        localPerson = localPerson,
        externalPerson = externalPerson,
        matchingConfig = matchingConfig
    )
    
    println("Результат сопоставления:")
    println("  Уверенность: ${matchResult.confidenceScore}")
    println("  Тип совпадения: ${matchResult.matchType}")
    
    println("\nСовпавшие поля:")
    matchResult.matchedFields.forEach { field ->
        println("  - ${field.fieldName}: ${field.similarity}")
    }
    
    println("\nКонфликты:")
    matchResult.conflicts.forEach { conflict ->
        println("  - ${conflict.fieldName} (${conflict.severity})")
        println("    Локально: ${conflict.localValue}")
        println("    Внешне: ${conflict.externalValue}")
        println("    Решение: ${conflict.resolution}")
    }
    
    if (matchResult.aiExplanation != null) {
        println("\nОбъяснение AI:")
        println(matchResult.aiExplanation)
    }
}
```

### Работа с родословной

```kotlin
// Получение родословной персоны (3 поколения)
val pedigreeResult = genealogyService.clients["familysearch"]
    ?.getPersonPedigree("KWCD-123", generations = 3)

pedigreeResult?.onSuccess { persons ->
    println("Получено ${persons.size} персон в родословной")
    persons.forEach { person ->
        println("  ${person.firstName} ${person.lastName}")
        println("    Родители: ${person.parents.size}")
        println("    Супруги: ${person.spouses.size}")
        println("    Дети: ${person.children.size}")
    }
}
```

## Конфигурация провайдеров

### FamilySearch

```kotlin
val familySearchConfig = GenealogyApiConfig(
    provider = "FAMILYSEARCH",
    apiKey = "your-api-key",  // или accessToken для OAuth
    baseUrl = "https://api.familysearch.org",
    dataFormat = "GEDCOM_X",
    enabled = true,
    rateLimitPerMinute = 60
)
```

**Получение API ключа:**
1. Зарегистрируйтесь на https://www.familysearch.org/developers/
2. Создайте приложение
3. Получите API ключ или настройте OAuth

### Ancestry (пример)

```kotlin
val ancestryConfig = GenealogyApiConfig(
    provider = "ANCESTRY",
    accessToken = "your-oauth-token",
    baseUrl = "https://api.ancestry.com",
    dataFormat = "JSON_CUSTOM",
    enabled = true,
    rateLimitPerMinute = 100
)
```

## Настройка AI-сопоставления

AI-сопоставление использует уже подключённые LLM для более точного сравнения:

```kotlin
val matchingConfig = PersonMatchingConfig(
    minConfidenceScore = 0.7,        // Минимальный порог уверенности
    useAiMatching = true,             // Включить AI
    matchingFields = listOf(
        "firstName", "lastName", 
        "birthDate", "birthPlace",
        "deathDate", "deathPlace",
        "parents", "spouses"
    ),
    fuzzyMatchingEnabled = true       // Нечёткое сравнение имён
)
```

**Преимущества AI-сопоставления:**
- Учёт культурных особенностей написания имён
- Распознавание опечаток и вариантов написания
- Понимание исторического контекста
- Интеллектуальная оценка конфликтов
- Предложения по улучшению данных

## Обработка ошибок

```kotlin
val result = genealogyService.searchInProvider("familysearch", criteria)

result.onSuccess { searchResult ->
    // Обработка успешного результата
    println("Найдено: ${searchResult.persons.size}")
}

result.onFailure { error ->
    when (error) {
        is IllegalArgumentException -> {
            println("Провайдер не зарегистрирован")
        }
        is java.net.UnknownHostException -> {
            println("Нет подключения к интернету")
        }
        else -> {
            println("Ошибка: ${error.message}")
        }
    }
}
```

## Rate Limiting

Сервис автоматически управляет лимитами запросов:

```kotlin
val rateLimitInfo = genealogyService.getProviderRateLimit("familysearch")

rateLimitInfo.onSuccess { info ->
    println("Лимит: ${info.limit}")
    println("Осталось: ${info.remaining}")
    println("Сброс через: ${info.resetTime - System.currentTimeMillis()} мс")
}
```

## Расширение функциональности

### Добавление нового провайдера

1. Создайте класс, реализующий `GenealogyApiClient`:

```kotlin
class MyCustomClient(
    config: GenealogyApiConfig,
    httpClient: HttpClient
) : BaseGenealogyApiClient(config) {
    
    override suspend fun searchPersons(
        criteria: PersonSearchCriteria
    ): Result<PersonSearchResult> {
        // Реализация поиска
    }
    
    // ... другие методы
}
```

2. Добавьте провайдер в enum `GenealogyApiProvider`
3. Обновите фабрику в `GenealogyApiService`

### Добавление нового парсера

```kotlin
class MyCustomParser : GenealogyDataParser {
    override fun parse(data: String): Result<List<ExternalPerson>> {
        // Реализация парсинга
    }
}
```

## TODO / Планы развития

- [ ] Полная реализация GEDCOM X парсера
- [ ] Реализация GEDCOM 5.5 парсера
- [ ] Добавление клиентов для Ancestry, MyHeritage, WikiTree
- [ ] Кэширование результатов поиска
- [ ] Продвинутый rate limiting с очередями
- [ ] Batch операции для массового поиска
- [ ] Экспорт результатов в различные форматы
- [ ] UI компоненты для визуализации совпадений
- [ ] Автоматическое обогащение данных из внешних источников

## Лицензии и ограничения

При использовании внешних API обязательно ознакомьтесь с:
- Условиями использования API
- Лимитами запросов
- Требованиями к атрибуции данных
- Политикой конфиденциальности
