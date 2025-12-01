# Архитектура интеграции с генеалогическими API

## Обзор

Данная архитектура предоставляет гибкую и расширяемую систему для:
1. Подключения к различным генеалогическим API (FamilySearch, Ancestry, MyHeritage и др.)
2. Парсинга различных форматов данных (GEDCOM X, GEDCOM 5.5, JSON)
3. Интеллектуального сопоставления персон с использованием LLM

## Структура модулей

```
core/src/commonMain/kotlin/com/family/tree/core/genealogy/
│
├── GenealogyApiConfig.kt          # Конфигурация провайдеров
├── GenealogyApiService.kt         # Основной сервис
├── README.md                       # Документация
│
├── client/                         # API клиенты
│   ├── GenealogyApiClient.kt      # Базовый интерфейс
│   └── FamilySearchClient.kt      # Клиент FamilySearch
│
├── model/                          # Модели данных
│   └── ExternalPerson.kt          # Унифицированная модель персоны
│
├── parser/                         # Парсеры форматов
│   └── GenealogyDataParser.kt     # GEDCOM X, GEDCOM 5.5
│
├── matching/                       # Сопоставление персон
│   ├── PersonMatcher.kt           # Базовое сопоставление
│   └── AiPersonMatcher.kt         # AI-powered сопоставление
│
└── example/                        # Примеры использования
    └── GenealogySearchExample.kt
```

## Диаграмма потока данных

```
┌─────────────┐
│   User UI   │
└──────┬──────┘
       │
       ▼
┌──────────────────────────────────────────────────┐
│         GenealogyApiService                      │
│  • registerProvider()                            │
│  • searchInAllProviders()                        │
│  • findMatchesForPerson()                        │
│  • matchPerson()                                 │
└────┬─────────────────────────────────────┬───────┘
     │                                     │
     │ Поиск                               │ Сопоставление
     ▼                                     ▼
┌─────────────────┐              ┌──────────────────┐
│ API Clients     │              │ PersonMatcher    │
├─────────────────┤              ├──────────────────┤
│ FamilySearch    │              │ BasicMatcher     │
│ Ancestry        │              │ AiMatcher        │
│ MyHeritage      │              │   ↓              │
│ WikiTree        │              │ Uses LLM         │
└────┬────────────┘              └────┬─────────────┘
     │                                │
     │ HTTP Request                   │ AI Prompt
     ▼                                ▼
┌─────────────────┐              ┌──────────────────┐
│ External API    │              │ LLM (GPT/Gemini) │
│ (GEDCOM X, etc) │              │                  │
└────┬────────────┘              └──────────────────┘
     │
     │ Response
     ▼
┌─────────────────┐
│ Data Parsers    │
├─────────────────┤
│ GedcomXParser   │
│ Gedcom55Parser  │
└────┬────────────┘
     │
     │ Parsed Data
     ▼
┌─────────────────┐
│ ExternalPerson  │
│ (Unified Model) │
└─────────────────┘
```

## Ключевые принципы дизайна

### 1. **Разделение ответственности (Separation of Concerns)**
- **Клиенты** отвечают только за HTTP коммуникацию
- **Парсеры** отвечают только за преобразование форматов
- **Матчеры** отвечают только за сопоставление
- **Сервис** координирует все компоненты

### 2. **Открыто/Закрыто (Open/Closed Principle)**
- Легко добавить новый провайдер API (реализовать `GenealogyApiClient`)
- Легко добавить новый формат данных (реализовать `GenealogyDataParser`)
- Легко добавить новый алгоритм сопоставления (реализовать `PersonMatcher`)

### 3. **Инверсия зависимостей (Dependency Inversion)**
- Все компоненты зависят от интерфейсов, а не от конкретных реализаций
- `GenealogyApiService` работает с `GenealogyApiClient`, а не с конкретными клиентами
- Легко подменить реализацию для тестирования

### 4. **Единая модель данных (Unified Data Model)**
- `ExternalPerson` - промежуточное представление
- Все парсеры конвертируют в эту модель
- Упрощает сопоставление и обработку

## Workflow: Поиск и сопоставление персоны

```
1. User Request
   └─> GenealogyApiService.findMatchesForPerson(localPerson)
       │
       ├─> Создание PersonSearchCriteria из localPerson
       │   (firstName, lastName, birthYear, etc.)
       │
       ├─> Для каждого зарегистрированного провайдера:
       │   │
       │   ├─> GenealogyApiClient.searchPersons(criteria)
       │   │   │
       │   │   ├─> HTTP запрос к API
       │   │   │
       │   │   ├─> Получение ответа (GEDCOM X, JSON, etc.)
       │   │   │
       │   │   └─> GenealogyDataParser.parse(response)
       │   │       └─> List<ExternalPerson>
       │   │
       │   └─> PersonMatcher.findBestMatches(localPerson, externalPersons)
       │       │
       │       ├─> Если useAiMatching = true:
       │       │   │
       │       │   ├─> Построение промпта для LLM
       │       │   │   (сравнение полей, учёт контекста)
       │       │   │
       │       │   ├─> AiClient.sendMessage(prompt)
       │       │   │
       │       │   └─> Парсинг AI ответа
       │       │       └─> PersonMatchResult
       │       │
       │       └─> Иначе:
       │           └─> BasicPersonMatcher (Levenshtein distance)
       │               └─> PersonMatchResult
       │
       └─> Агрегация результатов
           └─> Map<Provider, List<PersonMatchResult>>
```

## Интеграция с существующим кодом

### Использование существующих компонентов

1. **AiClient и AiConfig** (уже реализованы)
   - Используются в `AiPersonMatcher` для интеллектуального сопоставления
   - Поддерживают OpenAI, Google Gemini, Yandex GPT, Ollama

2. **Individual** (существующая модель)
   - Используется как входные данные для поиска
   - Сопоставляется с `ExternalPerson`

3. **HttpClient** (Ktor)
   - Используется для HTTP запросов к API
   - Переиспользуется из существующей инфраструктуры

### Точки расширения

1. **Добавление новых провайдеров**
   ```kotlin
   class AncestryClient(config: GenealogyApiConfig, httpClient: HttpClient) 
       : BaseGenealogyApiClient(config) {
       // Реализация методов
   }
   ```

2. **Добавление новых парсеров**
   ```kotlin
   class MyCustomParser : GenealogyDataParser {
       override fun parse(data: String): Result<List<ExternalPerson>>
   }
   ```

3. **Кастомизация сопоставления**
   ```kotlin
   class CustomMatcher : PersonMatcher {
       override suspend fun match(...)
   }
   ```

## Обработка различных форматов

### GEDCOM X (JSON)
```json
{
  "persons": [{
    "id": "KWCD-123",
    "names": [{
      "nameForms": [{
        "parts": [
          {"type": "http://gedcomx.org/Given", "value": "John"},
          {"type": "http://gedcomx.org/Surname", "value": "Smith"}
        ]
      }]
    }],
    "gender": {"type": "http://gedcomx.org/Male"},
    "facts": [{
      "type": "http://gedcomx.org/Birth",
      "date": {"original": "1850"},
      "place": {"original": "New York"}
    }]
  }]
}
```

**Парсинг**: `GedcomXParser` → `ExternalPerson`

### GEDCOM 5.5 (Текстовый)
```
0 @I1@ INDI
1 NAME John /Smith/
1 SEX M
1 BIRT
2 DATE 1850
2 PLAC New York
```

**Парсинг**: `Gedcom55Parser` → `ExternalPerson` (TODO)

## AI-Powered сопоставление

### Промпт для LLM

```
Ты - эксперт по генеалогии. Сравни две записи:

Локальная:
- Имя: Иван Иванов
- Год рождения: 1850

Внешняя:
- Имя: Ivan Ivanov
- Дата рождения: circa 1852
- Место: Moscow, Russia

Верни JSON с оценкой совпадения (0.0-1.0) и объяснением.
```

### Ответ LLM

```json
{
  "confidenceScore": 0.85,
  "matchType": "HIGH",
  "matchedFields": [
    {"fieldName": "firstName", "similarity": 0.95, "comment": "Транслитерация"},
    {"fieldName": "birthYear", "similarity": 0.8, "comment": "Разница 2 года, приемлемо"}
  ],
  "conflicts": [],
  "explanation": "Высокая вероятность совпадения. Имена идентичны (транслитерация), годы рождения близки.",
  "suggestions": ["Проверить место рождения для подтверждения"]
}
```

### Преимущества AI-сопоставления

1. **Культурный контекст**: понимает транслитерацию, разные системы написания
2. **Нечёткое сравнение**: учитывает опечатки, сокращения
3. **Исторический контекст**: понимает изменения названий мест, календарей
4. **Объяснения**: предоставляет понятные объяснения решений
5. **Предложения**: даёт рекомендации по улучшению данных

## Безопасность и конфиденциальность

### API ключи
- Хранятся в `GenealogyApiConfig`
- Не логируются
- Передаются только в заголовках HTTPS запросов

### Данные персон
- Локальные данные не отправляются на внешние серверы без согласия
- При AI-сопоставлении отправляются только необходимые поля
- Результаты поиска кэшируются локально

### Rate Limiting
- Автоматическое управление лимитами запросов
- Предотвращение блокировки аккаунта
- Очереди запросов (TODO)

## Производительность

### Оптимизации

1. **Параллельный поиск**: запросы к разным провайдерам выполняются параллельно
2. **Кэширование**: результаты поиска кэшируются (TODO)
3. **Batch операции**: массовая обработка персон (TODO)
4. **Lazy loading**: загрузка детальной информации по требованию

### Метрики

- Время выполнения запроса (`queryTime` в `PersonSearchResult`)
- Количество использованных запросов (`RateLimitInfo`)
- Уверенность сопоставления (`confidenceScore`)

## Тестирование

### Unit тесты
```kotlin
class PersonMatcherTest {
    @Test
    fun testExactMatch() {
        val matcher = BasicPersonMatcher()
        val result = matcher.match(localPerson, externalPerson, config)
        assertEquals(MatchType.EXACT, result.matchType)
    }
}
```

### Integration тесты
```kotlin
class FamilySearchClientTest {
    @Test
    suspend fun testSearch() {
        val client = FamilySearchClient(config, httpClient)
        val result = client.searchPersons(criteria)
        assertTrue(result.isSuccess)
    }
}
```

### Mock провайдеры
```kotlin
class MockGenealogyClient : GenealogyApiClient {
    override suspend fun searchPersons(...) = Result.success(mockData)
}
```

## Roadmap

### Фаза 1: MVP (Текущая)
- ✅ Базовая архитектура
- ✅ FamilySearch клиент (базовый)
- ✅ GEDCOM X парсер (базовый)
- ✅ Базовое и AI сопоставление
- ✅ Документация и примеры

### Фаза 2: Расширение
- [ ] Полная реализация GEDCOM X парсера
- [ ] GEDCOM 5.5 парсер
- [ ] Ancestry клиент
- [ ] MyHeritage клиент
- [ ] WikiTree клиент
- [ ] Кэширование результатов

### Фаза 3: Продвинутые функции
- [ ] Batch операции
- [ ] Автоматическое обогащение данных
- [ ] Визуализация совпадений
- [ ] Экспорт результатов
- [ ] Продвинутый rate limiting
- [ ] Offline режим

### Фаза 4: UI интеграция
- [ ] UI компоненты для поиска
- [ ] UI для просмотра совпадений
- [ ] UI для разрешения конфликтов
- [ ] Интеграция с редактором дерева

## Заключение

Данная архитектура предоставляет:
- **Гибкость**: легко добавлять новые провайдеры и форматы
- **Расширяемость**: модульная структура позволяет расширять функциональность
- **Интеллектуальность**: использование LLM для точного сопоставления
- **Производительность**: параллельная обработка и кэширование
- **Безопасность**: защита API ключей и данных пользователей

Архитектура готова к использованию и дальнейшему развитию!
