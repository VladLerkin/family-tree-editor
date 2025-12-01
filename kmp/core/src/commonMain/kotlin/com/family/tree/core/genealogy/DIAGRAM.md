# Визуальная диаграмма архитектуры

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                                                                             │
│                          USER APPLICATION / UI                              │
│                                                                             │
└──────────────────────────────────┬──────────────────────────────────────────┘
                                   │
                                   ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                                                                             │
│                        GenealogyApiService                                  │
│                        (Координатор)                                        │
│                                                                             │
│  • registerProvider()                                                       │
│  • searchInAllProviders()                                                   │
│  • findMatchesForPerson()                                                   │
│  • matchPerson()                                                            │
│                                                                             │
└────────────┬────────────────────────────────────────────┬───────────────────┘
             │                                            │
             │ Поиск                                      │ Сопоставление
             ▼                                            ▼
┌────────────────────────────┐              ┌─────────────────────────────────┐
│                            │              │                                 │
│    API Clients Layer       │              │    PersonMatcher Layer          │
│    (Адаптеры провайдеров)  │              │    (Сопоставление персон)       │
│                            │              │                                 │
│  ┌──────────────────────┐  │              │  ┌───────────────────────────┐  │
│  │ FamilySearchClient   │  │              │  │  BasicPersonMatcher       │  │
│  │  • searchPersons()   │  │              │  │   • Levenshtein distance  │  │
│  │  • getPersonById()   │  │              │  │   • Fuzzy matching        │  │
│  │  • getPedigree()     │  │              │  └───────────────────────────┘  │
│  └──────────────────────┘  │              │                                 │
│                            │              │  ┌───────────────────────────┐  │
│  ┌──────────────────────┐  │              │  │  AiPersonMatcher          │  │
│  │ AncestryClient       │  │              │  │   • AI-powered matching   │  │
│  │  (TODO)              │  │              │  │   • Context awareness     │  │
│  └──────────────────────┘  │              │  │   • Explanations          │  │
│                            │              │  └───────┬───────────────────┘  │
│  ┌──────────────────────┐  │              │          │                      │
│  │ MyHeritageClient     │  │              │          │ Uses                 │
│  │  (TODO)              │  │              │          ▼                      │
│  └──────────────────────┘  │              │  ┌───────────────────────────┐  │
│                            │              │  │  AiClient (existing)      │  │
│  ┌──────────────────────┐  │              │  │   • OpenAI GPT            │  │
│  │ WikiTreeClient       │  │              │  │   • Google Gemini         │  │
│  │  (TODO)              │  │              │  │   • Yandex GPT            │  │
│  └──────────────────────┘  │              │  │   • Ollama                │  │
│                            │              │  └───────────────────────────┘  │
└────────────┬───────────────┘              └─────────────────────────────────┘
             │                                            │
             │ HTTP Request                               │ AI Prompt
             ▼                                            ▼
┌────────────────────────────┐              ┌─────────────────────────────────┐
│                            │              │                                 │
│  External APIs             │              │  LLM Services                   │
│  (Генеалогические сервисы) │              │  (AI модели)                    │
│                            │              │                                 │
│  • FamilySearch API        │              │  • GPT-4o-mini                  │
│  • Ancestry API            │              │  • Gemini 2.0 Flash             │
│  • MyHeritage API          │              │  • YandexGPT                    │
│  • WikiTree API            │              │  • Llama 3.2                    │
│                            │              │                                 │
└────────────┬───────────────┘              └─────────────────────────────────┘
             │
             │ Response (GEDCOM X, JSON, etc.)
             ▼
┌────────────────────────────┐
│                            │
│  Data Parsers Layer        │
│  (Парсеры форматов)        │
│                            │
│  ┌──────────────────────┐  │
│  │ GedcomXParser        │  │
│  │  • Parse GEDCOM X    │  │
│  │  • Extract persons   │  │
│  │  • Extract events    │  │
│  └──────────────────────┘  │
│                            │
│  ┌──────────────────────┐  │
│  │ Gedcom55Parser       │  │
│  │  (TODO)              │  │
│  └──────────────────────┘  │
│                            │
│  ┌──────────────────────┐  │
│  │ CustomJsonParser     │  │
│  │  (Extensible)        │  │
│  └──────────────────────┘  │
│                            │
└────────────┬───────────────┘
             │
             │ Parsed Data
             ▼
┌────────────────────────────────────────────────────────────────────────────┐
│                                                                            │
│                    Unified Data Model                                      │
│                                                                            │
│  ┌──────────────────────────────────────────────────────────────────────┐ │
│  │  ExternalPerson                                                      │ │
│  │   • externalId, sourceProvider                                       │ │
│  │   • firstName, lastName, gender                                      │ │
│  │   • birthDate, birthPlace                                            │ │
│  │   • deathDate, deathPlace                                            │ │
│  │   • events, parents, spouses, children                               │ │
│  │   • sources, notes                                                   │ │
│  └──────────────────────────────────────────────────────────────────────┘ │
│                                                                            │
│  ┌──────────────────────────────────────────────────────────────────────┐ │
│  │  PersonMatchResult                                                   │ │
│  │   • confidenceScore (0.0 - 1.0)                                      │ │
│  │   • matchType (EXACT, HIGH, MEDIUM, LOW, NO_MATCH)                   │ │
│  │   • matchedFields, conflicts                                         │ │
│  │   • aiExplanation, suggestions                                       │ │
│  └──────────────────────────────────────────────────────────────────────┘ │
│                                                                            │
└────────────────────────────────────────────────────────────────────────────┘


═══════════════════════════════════════════════════════════════════════════════

                              DATA FLOW DIAGRAM

═══════════════════════════════════════════════════════════════════════════════

1. ПОИСК ПЕРСОНЫ:
   
   User → GenealogyApiService.searchInAllProviders(criteria)
            │
            ├─→ FamilySearchClient.searchPersons(criteria)
            │    │
            │    ├─→ HTTP GET https://api.familysearch.org/...
            │    │
            │    ├─→ Response: GEDCOM X JSON
            │    │
            │    ├─→ GedcomXParser.parse(response)
            │    │
            │    └─→ List<ExternalPerson>
            │
            ├─→ AncestryClient.searchPersons(criteria)
            │    └─→ ...
            │
            └─→ Map<Provider, PersonSearchResult>


2. СОПОСТАВЛЕНИЕ С ЛОКАЛЬНОЙ ПЕРСОНОЙ:

   User → GenealogyApiService.findMatchesForPerson(localPerson)
            │
            ├─→ Создание PersonSearchCriteria из Individual
            │
            ├─→ Поиск во всех провайдерах (см. выше)
            │    └─→ List<ExternalPerson>
            │
            └─→ PersonMatcher.findBestMatches(localPerson, externalPersons)
                 │
                 ├─→ Если useAiMatching = true:
                 │    │
                 │    ├─→ Построение промпта:
                 │    │    "Сравни локальную персону: Иван Иванов (1850)
                 │    │     с внешней: Ivan Ivanov (circa 1852)..."
                 │    │
                 │    ├─→ AiClient.sendMessage(prompt)
                 │    │    └─→ LLM (GPT/Gemini/Yandex)
                 │    │
                 │    ├─→ Response: JSON с оценкой совпадения
                 │    │
                 │    └─→ PersonMatchResult
                 │         • confidenceScore: 0.85
                 │         • matchType: HIGH
                 │         • explanation: "Высокая вероятность..."
                 │
                 └─→ Иначе:
                      │
                      ├─→ BasicPersonMatcher
                      │    • Levenshtein distance для имён
                      │    • Сравнение дат
                      │
                      └─→ PersonMatchResult


═══════════════════════════════════════════════════════════════════════════════

                            COMPONENT INTERACTIONS

═══════════════════════════════════════════════════════════════════════════════

┌─────────────┐         ┌──────────────┐         ┌─────────────┐
│             │         │              │         │             │
│  Individual │────────▶│  Genealogy   │────────▶│  External   │
│  (Local)    │         │  ApiService  │         │  Person     │
│             │         │              │         │             │
└─────────────┘         └──────┬───────┘         └─────────────┘
                               │                        │
                               │                        │
                        ┌──────▼───────┐        ┌──────▼──────┐
                        │              │        │             │
                        │ PersonMatcher│───────▶│   Match     │
                        │              │        │   Result    │
                        └──────────────┘        └─────────────┘


═══════════════════════════════════════════════════════════════════════════════

                              EXTENSION POINTS

═══════════════════════════════════════════════════════════════════════════════

1. Добавление нового провайдера API:
   
   class MyCustomClient(config, httpClient) : BaseGenealogyApiClient(config) {
       override suspend fun searchPersons(...) { ... }
   }
   
   └─→ Регистрация в GenealogyApiService

2. Добавление нового парсера:
   
   class MyCustomParser : GenealogyDataParser {
       override fun parse(data: String) { ... }
   }
   
   └─→ Регистрация в GenealogyParserFactory

3. Добавление нового алгоритма сопоставления:
   
   class MyCustomMatcher : PersonMatcher {
       override suspend fun match(...) { ... }
   }
   
   └─→ Использование в GenealogyApiService


═══════════════════════════════════════════════════════════════════════════════
```
