package com.family.tree.core.ai

import com.family.tree.core.ProjectData
import com.family.tree.core.ProjectMetadata
import com.family.tree.core.io.LoadedProject
import com.family.tree.core.model.*
import kotlinx.serialization.json.Json

/**
 * Импортер для создания генеалогического древа из текста, обработанного AI.
 * 
 * AI анализирует текстовое описание, извлекает информацию о персонах и их родственных связях,
 * и возвращает JSON с результатами. Этот класс преобразует результаты в ProjectData.
 */
class AiTextImporter(
    private val config: AiConfig = AiPresets.OPENAI_GPT4O_MINI
) {
    
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }
    
    private val aiClient: AiClient = AiClientFactory.createClient(config)
    
    /**
     * Импортирует проект из произвольного текстового описания родословной.
     * Отправляет текст на AI для анализа и создания структурированных данных.
     * 
     * @param textDescription Произвольное текстовое описание родословной
     * @return LoadedProject с созданными персонами и семьями
     */
    suspend fun importFromText(textDescription: String): LoadedProject {
        println("[DEBUG_LOG] AiTextImporter.importFromText: Starting AI import with ${textDescription.length} chars")
        println("[DEBUG_LOG] AiTextImporter.importFromText: First 200 chars: ${textDescription.take(200)}")
        
        // Отправляем текст на AI для анализа
        val aiResultJson = analyzeTextWithAi(textDescription)
        println("[DEBUG_LOG] AiTextImporter.importFromText: Received AI response with ${aiResultJson.length} chars")
        println("[DEBUG_LOG] AiTextImporter.importFromText: AI response preview: ${aiResultJson.take(500)}")
        
        // Парсим результат AI
        val result = importFromAiResult(aiResultJson)
        println("[DEBUG_LOG] AiTextImporter.importFromText: Parsed result - ${result.data.individuals.size} individuals, ${result.data.families.size} families")
        return result
    }
    
    /**
     * Анализирует текст с помощью AI и возвращает JSON с результатами.
     * 
     * @param textDescription Текстовое описание родословной
     * @return JSON строка с результатами AI-анализа в формате AiAnalysisResult
     */
    private suspend fun analyzeTextWithAi(textDescription: String): String {
        // Формируем промпт для AI
        val prompt = buildAiPrompt(textDescription)
        
        // Вызываем AI API
        return callAiApi(prompt)
    }
    
    /**
     * Строит промпт для AI с инструкциями по анализу текста.
     */
    private fun buildAiPrompt(textDescription: String): String {
        return """
You are a genealogy expert. Analyze the following text and extract information about people and their relationships.

Return a JSON object with this exact structure:
{
  "persons": [
    {
      "firstName": "string",
      "middleName": "string",
      "lastName": "string",
      "gender": "MALE" | "FEMALE" | "UNKNOWN",
      "birthDate": "string or null",
      "deathDate": "string or null",
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
- Extract birth and death dates as strings in format "DD.MM.YYYY" or "YYYY" if only year is known
- If only year is available, use just the year (e.g., "1950")
- If full date is available, use format "DD.MM.YYYY" (e.g., "15.03.1950")
- If date is unclear or not mentioned, use null
- If gender is unclear, use "UNKNOWN"
- For Russian names, extract patronymic (отчество) as middleName if present
- If middleName is not present, use empty string ""
- Put any additional information in the "notes" field

Text to analyze:
$textDescription

Return ONLY the JSON object, no explanations.
        """.trimIndent()
    }
    
    /**
     * Вызывает AI API для анализа текста.
     * 
     * @param prompt Промпт для AI
     * @return JSON строка с результатами анализа
     */
    private suspend fun callAiApi(prompt: String): String {
        return aiClient.sendPrompt(prompt, config)
    }
    
    /**
     * Импортирует проект из JSON-результата AI-анализа.
     * 
     * @param aiResultJson JSON строка с результатами AI-анализа в формате AiAnalysisResult
     * @return LoadedProject с созданными персонами и семьями
     */
    fun importFromAiResult(aiResultJson: String): LoadedProject {
        println("[DEBUG_LOG] AiTextImporter.importFromAiResult: Input JSON length=${aiResultJson.length}")
        
        // Clean markdown code blocks from LLM response
        val cleanedJson = cleanJsonFromMarkdown(aiResultJson)
        println("[DEBUG_LOG] AiTextImporter.importFromAiResult: Cleaned JSON length=${cleanedJson.length}")
        println("[DEBUG_LOG] AiTextImporter.importFromAiResult: Cleaned JSON preview: ${cleanedJson.take(500)}")
        
        val aiResult = json.decodeFromString<AiAnalysisResult>(cleanedJson)
        println("[DEBUG_LOG] AiTextImporter.importFromAiResult: Parsed - persons=${aiResult.persons.size}, relationships=${aiResult.relationships.size}")
        
        return convertToProject(aiResult)
    }
    
    /**
     * Removes markdown code block formatting from JSON response.
     * LLMs often wrap JSON in ```json ... ``` blocks, which breaks parsing.
     */
    private fun cleanJsonFromMarkdown(text: String): String {
        var cleaned = text.trim()
        
        // Remove ```json opening tag
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.removePrefix("```json").trimStart()
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.removePrefix("```").trimStart()
        }
        
        // Remove closing ``` tag
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.removeSuffix("```").trimEnd()
        }
        
        return cleaned.trim()
    }
    
    /**
     * Извлекает год из строки даты.
     * Поддерживает форматы: "YYYY", "DD.MM.YYYY", "DD/MM/YYYY"
     */
    private fun extractYear(dateString: String?): Int? {
        if (dateString.isNullOrBlank()) return null
        
        // Попытка извлечь 4-значный год из строки
        val yearRegex = Regex("""\b(\d{4})\b""")
        val match = yearRegex.find(dateString)
        return match?.groupValues?.get(1)?.toIntOrNull()
    }
    
    /**
     * Создает событие GEDCOM из даты.
     */
    private fun createEvent(type: String, date: String?, index: Int): GedcomEvent? {
        if (date.isNullOrBlank()) return null
        return GedcomEvent(
            id = GedcomEventId.generate(),
            type = type,
            date = date,
            place = ""
        )
    }
    
    /**
     * Преобразует результаты AI-анализа в ProjectData.
     */
    private fun convertToProject(aiResult: AiAnalysisResult): LoadedProject {
        println("[DEBUG_LOG] AiTextImporter.convertToProject: Starting conversion - persons=${aiResult.persons.size}, relationships=${aiResult.relationships.size}")
        
        val individuals = mutableListOf<Individual>()
        val families = mutableListOf<Family>()
        
        // Создаём персон
        aiResult.persons.forEachIndexed { index, aiPerson ->
            // Объединяем firstName и middleName в одно поле firstName
            val fullFirstName = if (aiPerson.middleName.isNotBlank()) {
                "${aiPerson.firstName} ${aiPerson.middleName}"
            } else {
                aiPerson.firstName
            }
            println("[DEBUG_LOG] AiTextImporter.convertToProject: Creating person $index: $fullFirstName ${aiPerson.lastName}")
            
            // Извлекаем годы из дат
            val birthYear = extractYear(aiPerson.birthDate)
            val deathYear = extractYear(aiPerson.deathDate)
            
            // Создаём события для дат рождения и смерти
            val events = mutableListOf<GedcomEvent>()
            createEvent("BIRT", aiPerson.birthDate, index)?.let { events.add(it) }
            createEvent("DEAT", aiPerson.deathDate, index)?.let { events.add(it) }
            
            val individual = Individual(
                id = IndividualId("ai_person_$index"),
                firstName = fullFirstName,
                lastName = aiPerson.lastName,
                gender = parseGender(aiPerson.gender),
                birthYear = birthYear,
                deathYear = deathYear,
                events = events,
                notes = if (aiPerson.notes.isNotBlank()) {
                    listOf(Note(id = NoteId("note_$index"), text = aiPerson.notes))
                } else {
                    emptyList()
                }
            )
            individuals.add(individual)
        }
        println("[DEBUG_LOG] AiTextImporter.convertToProject: Created ${individuals.size} individuals")
        
        // Группируем отношения для создания семей
        val familyGroups = groupRelationshipsIntoFamilies(aiResult.relationships, individuals.size)
        println("[DEBUG_LOG] AiTextImporter.convertToProject: Grouped into ${familyGroups.size} family groups")
        
        // Создаём семьи
        familyGroups.forEachIndexed { index, familyGroup ->
            val family = createFamily(index, familyGroup, individuals)
            if (family != null) {
                println("[DEBUG_LOG] AiTextImporter.convertToProject: Created family $index: spouse1=${familyGroup.spouse1}, spouse2=${familyGroup.spouse2}, children=${familyGroup.children.size}")
                families.add(family)
            } else {
                println("[DEBUG_LOG] AiTextImporter.convertToProject: Family $index was null (skipped)")
            }
        }
        println("[DEBUG_LOG] AiTextImporter.convertToProject: Created ${families.size} families")
        
        val projectData = ProjectData(
            individuals = individuals,
            families = families,
            metadata = ProjectMetadata(
                name = "Проект из AI-анализа текста",
                createdAt = 0L,
                modifiedAt = 0L
            )
        )
        
        println("[DEBUG_LOG] AiTextImporter.convertToProject: Final result - ${projectData.individuals.size} individuals, ${projectData.families.size} families")
        
        return LoadedProject(
            data = projectData,
            layout = null,
            meta = projectData.metadata
        )
    }
    
    /**
     * Группирует отношения в семьи.
     * Семья = супруги + их общие дети.
     */
    private fun groupRelationshipsIntoFamilies(
        relationships: List<AiRelationship>,
        personCount: Int
    ): List<FamilyGroup> {
        val familyGroups = mutableListOf<FamilyGroup>()
        val processedSpouses = mutableSetOf<Pair<Int, Int>>()
        
        // Находим пары супругов
        val spouseRelations = relationships.filter { it.relationType == "SPOUSE" }
        
        for (spouseRel in spouseRelations) {
            val pair1 = Pair(spouseRel.personIndex, spouseRel.relatedPersonIndex)
            val pair2 = Pair(spouseRel.relatedPersonIndex, spouseRel.personIndex)
            
            if (pair1 in processedSpouses || pair2 in processedSpouses) {
                continue
            }
            
            processedSpouses.add(pair1)
            processedSpouses.add(pair2)
            
            // Находим детей этой пары
            val children = findChildrenOfCouple(
                spouseRel.personIndex,
                spouseRel.relatedPersonIndex,
                relationships
            )
            
            familyGroups.add(
                FamilyGroup(
                    spouse1 = spouseRel.personIndex,
                    spouse2 = spouseRel.relatedPersonIndex,
                    children = children
                )
            )
        }
        
        // Обрабатываем родитель-ребёнок отношения без явных супругов
        val parentChildRelations = relationships.filter { 
            it.relationType == "PARENT" || it.relationType == "CHILD" 
        }
        
        val childrenWithParents = mutableMapOf<Int, MutableSet<Int>>()
        
        for (rel in parentChildRelations) {
            val (parentIdx, childIdx) = if (rel.relationType == "PARENT") {
                rel.personIndex to rel.relatedPersonIndex
            } else {
                rel.relatedPersonIndex to rel.personIndex
            }
            
            childrenWithParents.getOrPut(childIdx) { mutableSetOf() }.add(parentIdx)
        }
        
        // Группируем детей по комбинации родителей
        val parentCombinations = mutableMapOf<Set<Int>, MutableList<Int>>()
        
        for ((child, parents) in childrenWithParents) {
            parentCombinations.getOrPut(parents) { mutableListOf() }.add(child)
        }
        
        // Создаём семьи для каждой уникальной комбинации родителей
        for ((parents, children) in parentCombinations) {
            val parentsList = parents.toList()
            
            // Проверяем, не создали ли мы уже семью для этих супругов
            val alreadyProcessed = familyGroups.any { family ->
                val familySpouses = setOfNotNull(family.spouse1, family.spouse2)
                familySpouses == parents
            }
            
            if (!alreadyProcessed && children.isNotEmpty()) {
                familyGroups.add(
                    FamilyGroup(
                        spouse1 = parentsList.getOrNull(0),
                        spouse2 = parentsList.getOrNull(1),
                        children = children
                    )
                )
            }
        }
        
        return familyGroups
    }
    
    /**
     * Находит детей пары родителей.
     */
    private fun findChildrenOfCouple(
        parent1: Int,
        parent2: Int,
        relationships: List<AiRelationship>
    ): List<Int> {
        val childrenOfParent1 = relationships
            .filter { 
                (it.relationType == "PARENT" && it.personIndex == parent1) ||
                (it.relationType == "CHILD" && it.relatedPersonIndex == parent1)
            }
            .map { 
                if (it.relationType == "PARENT") it.relatedPersonIndex else it.personIndex 
            }
            .toSet()
        
        val childrenOfParent2 = relationships
            .filter { 
                (it.relationType == "PARENT" && it.personIndex == parent2) ||
                (it.relationType == "CHILD" && it.relatedPersonIndex == parent2)
            }
            .map { 
                if (it.relationType == "PARENT") it.relatedPersonIndex else it.personIndex 
            }
            .toSet()
        
        // Общие дети обоих родителей
        return (childrenOfParent1 intersect childrenOfParent2).toList()
    }
    
    /**
     * Создаёт объект Family из FamilyGroup.
     */
    private fun createFamily(
        index: Int,
        familyGroup: FamilyGroup,
        individuals: List<Individual>
    ): Family? {
        val spouse1Id = familyGroup.spouse1?.let { individuals.getOrNull(it)?.id }
        val spouse2Id = familyGroup.spouse2?.let { individuals.getOrNull(it)?.id }
        val childrenIds = familyGroup.children.mapNotNull { individuals.getOrNull(it)?.id }
        
        // Семья должна иметь хотя бы одного члена
        if (spouse1Id == null && spouse2Id == null && childrenIds.isEmpty()) {
            return null
        }
        
        // Определяем пол супругов для правильного назначения husband/wife
        val husband = if (spouse1Id != null && individuals.getOrNull(familyGroup.spouse1!!)?.gender == Gender.MALE) {
            spouse1Id
        } else if (spouse2Id != null && individuals.getOrNull(familyGroup.spouse2!!)?.gender == Gender.MALE) {
            spouse2Id
        } else {
            spouse1Id  // По умолчанию первый супруг
        }
        
        val wife = if (spouse1Id != null && spouse1Id != husband) {
            spouse1Id
        } else if (spouse2Id != null && spouse2Id != husband) {
            spouse2Id
        } else {
            null
        }
        
        return Family(
            id = FamilyId("ai_family_$index"),
            husbandId = husband,
            wifeId = wife,
            childrenIds = childrenIds
        )
    }
    
    /**
     * Парсит строку пола в enum Gender.
     */
    private fun parseGender(genderString: String?): Gender? {
        return when (genderString?.uppercase()) {
            "MALE", "M" -> Gender.MALE
            "FEMALE", "F" -> Gender.FEMALE
            "UNKNOWN", "U" -> Gender.UNKNOWN
            else -> null
        }
    }
    
    /**
     * Вспомогательный класс для группировки членов семьи.
     */
    private data class FamilyGroup(
        val spouse1: Int?,
        val spouse2: Int?,
        val children: List<Int>
    )
}
