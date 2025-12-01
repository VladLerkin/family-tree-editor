# AI Text Import

## Обзор

Функция AI Text Import позволяет импортировать генеалогическое древо из произвольного текстового описания родословной. Система может работать с двумя форматами файлов:

1. **JSON с результатами AI анализа** (готовый структурированный формат)
2. **Произвольный текст** (требует предварительной обработки через AI сервис)

## Как использовать

### Вариант 1: Импорт готового JSON с результатами AI

Если у вас уже есть JSON файл с результатами AI анализа:

1. Выберите **File → Import AI Text** (или **Import AI Text** в мобильном меню)
2. Выберите JSON файл
3. Программа автоматически распознает формат и создаст генеалогическое древо

**Формат JSON:**
```json
{
  "persons": [
    {
      "firstName": "Иван",
      "lastName": "Иванов",
      "gender": "MALE",
      "birthYear": 1950,
      "deathYear": 2020,
      "notes": "Дополнительная информация"
    },
    {
      "firstName": "Мария",
      "lastName": "Иванова",
      "gender": "FEMALE",
      "birthYear": 1955,
      "deathYear": null,
      "notes": ""
    }
  ],
  "relationships": [
    {
      "personIndex": 0,
      "relatedPersonIndex": 1,
      "relationType": "SPOUSE"
    }
  ]
}
```

### Вариант 2: Обработка произвольного текста через AI сервис

Если у вас есть текстовое описание родословной:

#### Шаг 1: Подготовьте текст

Создайте текстовый файл с описанием родословной. Например:

```
Семья Дугласов началась с Уильяма Дугласа (1750-1820), шотландского эмигранта.
Он женился на Маргарет Смит (1755-1830). У них было трое детей:
- Джон Дуглас (1780-1850), старший сын
- Элизабет Дуглас (1782-1860)
- Роберт Дуглас (1785-1855), младший сын

Джон женился на Сюзан Браун (1785-1865). Их дети:
- Уильям Дуглас II (1810-1880)
- Мэри Дуглас (1815-1890)
```

#### Шаг 2: Обработайте текст через AI

Отправьте ваш текст в любой AI сервис (ChatGPT, Claude, Gemini и т.д.) с следующим промптом:

```
Analyze this genealogy text and return ONLY a JSON object with this structure:
{
  "persons": [
    {
      "firstName": "string",
      "lastName": "string",
      "gender": "MALE" | "FEMALE" | "UNKNOWN",
      "birthYear": number or null,
      "deathYear": number or null,
      "notes": "string"
    }
  ],
  "relationships": [
    {
      "personIndex": number (index in persons array),
      "relatedPersonIndex": number (index in persons array),
      "relationType": "PARENT" | "CHILD" | "SPOUSE"
    }
  ]
}

Rules:
- Use "PARENT" when personIndex is parent of relatedPersonIndex
- Use "CHILD" when personIndex is child of relatedPersonIndex
- Use "SPOUSE" when personIndex and relatedPersonIndex are married
- Extract birth and death years as numbers (e.g., 1950), not ranges
- If gender is unclear, use "UNKNOWN"
- Put any additional information in the "notes" field

Text to analyze:
[ВАШ ТЕКСТ ЗДЕСЬ]

Return ONLY the JSON object, no explanations.
```

#### Шаг 3: Сохраните результат

1. Скопируйте JSON ответ от AI
2. Сохраните его в файл с расширением `.json`
3. Импортируйте файл через **File → Import AI Text**

## Типы отношений

### SPOUSE (супруги)
Указывает, что две персоны состоят в браке.
```json
{
  "personIndex": 0,
  "relatedPersonIndex": 1,
  "relationType": "SPOUSE"
}
```

### PARENT (родитель)
Указывает, что `personIndex` является родителем `relatedPersonIndex`.
```json
{
  "personIndex": 0,
  "relatedPersonIndex": 2,
  "relationType": "PARENT"
}
```

### CHILD (ребёнок)
Указывает, что `personIndex` является ребёнком `relatedPersonIndex`.
```json
{
  "personIndex": 2,
  "relatedPersonIndex": 0,
  "relationType": "CHILD"
}
```

## Поля персоны

- **firstName** (string, обязательное): Имя
- **lastName** (string, обязательное): Фамилия
- **gender** (string, опциональное): Пол - "MALE", "FEMALE" или "UNKNOWN"
- **birthYear** (number или null, опциональное): Год рождения (целое число)
- **deathYear** (number или null, опциональное): Год смерти (целое число)
- **notes** (string, опциональное): Дополнительные заметки

## Примеры

### Пример 1: Простая семья

**Текст:**
```
Джон Смит (1950) женат на Мэри Джонс (1952). У них двое детей: Том (1975) и Сара (1978).
```

**JSON результат:**
```json
{
  "persons": [
    {"firstName": "Джон", "lastName": "Смит", "gender": "MALE", "birthYear": 1950, "deathYear": null, "notes": ""},
    {"firstName": "Мэри", "lastName": "Джонс", "gender": "FEMALE", "birthYear": 1952, "deathYear": null, "notes": ""},
    {"firstName": "Том", "lastName": "Смит", "gender": "MALE", "birthYear": 1975, "deathYear": null, "notes": ""},
    {"firstName": "Сара", "lastName": "Смит", "gender": "FEMALE", "birthYear": 1978, "deathYear": null, "notes": ""}
  ],
  "relationships": [
    {"personIndex": 0, "relatedPersonIndex": 1, "relationType": "SPOUSE"},
    {"personIndex": 0, "relatedPersonIndex": 2, "relationType": "PARENT"},
    {"personIndex": 1, "relatedPersonIndex": 2, "relationType": "PARENT"},
    {"personIndex": 0, "relatedPersonIndex": 3, "relationType": "PARENT"},
    {"personIndex": 1, "relatedPersonIndex": 3, "relationType": "PARENT"}
  ]
}
```

### Пример 2: Три поколения

**Текст:**
```
Дедушка Иван (1920-2000) и бабушка Ольга (1925-2010) имели сына Петра (1950).
Петр женился на Анне (1955), у них родилась дочь Елена (1980).
```

**JSON результат:**
```json
{
  "persons": [
    {"firstName": "Иван", "lastName": "", "gender": "MALE", "birthYear": 1920, "deathYear": 2000, "notes": "Дедушка"},
    {"firstName": "Ольга", "lastName": "", "gender": "FEMALE", "birthYear": 1925, "deathYear": 2010, "notes": "Бабушка"},
    {"firstName": "Петр", "lastName": "", "gender": "MALE", "birthYear": 1950, "deathYear": null, "notes": ""},
    {"firstName": "Анна", "lastName": "", "gender": "FEMALE", "birthYear": 1955, "deathYear": null, "notes": ""},
    {"firstName": "Елена", "lastName": "", "gender": "FEMALE", "birthYear": 1980, "deathYear": null, "notes": ""}
  ],
  "relationships": [
    {"personIndex": 0, "relatedPersonIndex": 1, "relationType": "SPOUSE"},
    {"personIndex": 0, "relatedPersonIndex": 2, "relationType": "PARENT"},
    {"personIndex": 1, "relatedPersonIndex": 2, "relationType": "PARENT"},
    {"personIndex": 2, "relatedPersonIndex": 3, "relationType": "SPOUSE"},
    {"personIndex": 2, "relatedPersonIndex": 4, "relationType": "PARENT"},
    {"personIndex": 3, "relatedPersonIndex": 4, "relationType": "PARENT"}
  ]
}
```

## Технические детали

### Автоматическое определение формата

Программа автоматически определяет формат файла:
- Если файл начинается с `{` или `[` - обрабатывается как JSON
- Иначе - считается произвольным текстом и показывается инструкция

### Создание семей

Импортер автоматически группирует персоны в семьи на основе отношений:
- Находит пары супругов (SPOUSE)
- Определяет общих детей пар родителей (PARENT/CHILD)
- Назначает husband/wife на основе пола (gender)

### Обработка ошибок

При возникновении ошибок парсинга JSON программа показывает сообщение с деталями:
- Ожидаемый формат данных
- Описание ошибки
- Рекомендации по исправлению

## Планы на будущее

В будущих версиях планируется:
- Прямая интеграция с AI API (OpenAI, Anthropic и т.д.)
- Автоматическая обработка текста без ручного шага
- Поддержка локальных AI моделей
- Пакетная обработка нескольких файлов

## Поддерживаемые платформы

- ✅ Desktop (macOS, Windows, Linux)
- ✅ Android
- ✅ iOS

## Смотрите также

- [GEDCOM Support](GEDCOM_SUPPORT.md) - для импорта стандартных GEDCOM файлов
- [Build Desktop](BUILD_DESKTOP.md) - инструкции по сборке Desktop версии
