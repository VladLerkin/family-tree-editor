package com.family.tree.core.ai

import com.family.tree.core.ProjectData
import com.family.tree.core.ProjectMetadata
import com.family.tree.core.io.LoadedProject
import com.family.tree.core.model.*
import kotlinx.serialization.json.Json

/**
 * Importer for creating a family tree from AI-processed text.
 * 
 * AI analyzes the text description, extracts information about people and their relationships,
 * and returns JSON with the results. This class converts the results into ProjectData.
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
     * Imports a project from an arbitrary text description of genealogy.
     * Sends the text to AI for analysis and creation of structured data.
     * 
     * @param textDescription Arbitrary text description of genealogy
     * @return LoadedProject with created persons and families
     */
    suspend fun importFromText(textDescription: String): LoadedProject {
        println("[DEBUG_LOG] AiTextImporter.importFromText: Starting AI import with ${textDescription.length} chars")
        println("[DEBUG_LOG] AiTextImporter.importFromText: First 200 chars: ${textDescription.take(200)}")
        
        // Send text to AI for analysis
        val aiResultJson = analyzeTextWithAi(textDescription)
        println("[DEBUG_LOG] AiTextImporter.importFromText: Received AI response with ${aiResultJson.length} chars")
        println("[DEBUG_LOG] AiTextImporter.importFromText: AI response preview: ${aiResultJson.take(500)}")
        
        // Parse AI result
        val result = importFromAiResult(aiResultJson)
        println("[DEBUG_LOG] AiTextImporter.importFromText: Parsed result - ${result.data.individuals.size} individuals, ${result.data.families.size} families")
        return result
    }
    
    /**
     * Analyzes text using AI and returns JSON with results.
     * 
     * @param textDescription Text description of genealogy
     * @return JSON string with AI analysis results in AiAnalysisResult format
     */
    private suspend fun analyzeTextWithAi(textDescription: String): String {
        // Build prompt for AI
        val prompt = buildAiPrompt(textDescription)
        
        // Call AI API
        return callAiApi(prompt)
    }
    
    /**
     * Builds a prompt for AI with instructions for text analysis.
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
     * Calls AI API for text analysis.
     * 
     * @param prompt Prompt for AI
     * @return JSON string with analysis results
     */
    private suspend fun callAiApi(prompt: String): String {
        println("[DEBUG_LOG] AiTextImporter.callAiApi: Sending prompt to AI (length=${prompt.length})")
        println("[DEBUG_LOG] AiTextImporter.callAiApi: Prompt preview (first 500 chars): ${prompt.take(500)}")
        val result = aiClient.sendPrompt(prompt, config)
        println("[DEBUG_LOG] AiTextImporter.callAiApi: Received response (length=${result.length})")
        return result
    }
    
    /**
     * Imports a project from AI analysis JSON result.
     * 
     * @param aiResultJson JSON string with AI analysis results in AiAnalysisResult format
     * @return LoadedProject with created persons and families
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
     * Extracts year from date string.
     * Supports formats: "YYYY", "DD.MM.YYYY", "DD/MM/YYYY"
     */
    private fun extractYear(dateString: String?): Int? {
        if (dateString.isNullOrBlank()) return null
        
        // Attempt to extract 4-digit year from string
        val yearRegex = Regex("""\b(\d{4})\b""")
        val match = yearRegex.find(dateString)
        return match?.groupValues?.get(1)?.toIntOrNull()
    }
    
    /**
     * Creates a GEDCOM event from date.
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
     * Converts AI analysis results to ProjectData.
     */
    private fun convertToProject(aiResult: AiAnalysisResult): LoadedProject {
        println("[DEBUG_LOG] AiTextImporter.convertToProject: Starting conversion - persons=${aiResult.persons.size}, relationships=${aiResult.relationships.size}")
        
        val individuals = mutableListOf<Individual>()
        val families = mutableListOf<Family>()
        
        // Create persons
        aiResult.persons.forEachIndexed { index, aiPerson ->
            // Combine firstName and middleName into single firstName field
            val fullFirstName = if (aiPerson.middleName.isNotBlank()) {
                "${aiPerson.firstName} ${aiPerson.middleName}"
            } else {
                aiPerson.firstName
            }
            println("[DEBUG_LOG] AiTextImporter.convertToProject: Creating person $index: $fullFirstName ${aiPerson.lastName}")
            
            // Extract years from dates
            val birthYear = extractYear(aiPerson.birthDate)
            val deathYear = extractYear(aiPerson.deathDate)
            
            // Create events for birth and death dates
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
        
        // Group relationships to create families
        val familyGroups = groupRelationshipsIntoFamilies(aiResult.relationships, individuals.size)
        println("[DEBUG_LOG] AiTextImporter.convertToProject: Grouped into ${familyGroups.size} family groups")
        
        // Create families
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
     * Groups relationships into families.
     * Family = spouses + their common children.
     */
    private fun groupRelationshipsIntoFamilies(
        relationships: List<AiRelationship>,
        personCount: Int
    ): List<FamilyGroup> {
        val familyGroups = mutableListOf<FamilyGroup>()
        val processedSpouses = mutableSetOf<Pair<Int, Int>>()
        
        // Filter valid relationships where all required fields are present and indices are within bounds
        val validRelationships = relationships.filter { rel ->
            rel.personIndex != null && rel.relatedPersonIndex != null && rel.relationType != null &&
            rel.personIndex >= 0 && rel.personIndex < personCount &&
            rel.relatedPersonIndex >= 0 && rel.relatedPersonIndex < personCount
        }
        
        // Find spouse pairs
        val spouseRelations = validRelationships.filter { it.relationType == "SPOUSE" }
        
        for (spouseRel in spouseRelations) {
            val p1 = spouseRel.personIndex!!
            val p2 = spouseRel.relatedPersonIndex!!
            
            val pair1 = Pair(p1, p2)
            val pair2 = Pair(p2, p1)
            
            if (pair1 in processedSpouses || pair2 in processedSpouses) {
                continue
            }
            
            processedSpouses.add(pair1)
            processedSpouses.add(pair2)
            
            // Find children of this couple
            val children = findChildrenOfCouple(
                p1,
                p2,
                validRelationships
            )
            
            familyGroups.add(
                FamilyGroup(
                    spouse1 = p1,
                    spouse2 = p2,
                    children = children
                )
            )
        }
        
        // Process parent-child relationships without explicit spouses
        val parentChildRelations = validRelationships.filter { 
            it.relationType == "PARENT" || it.relationType == "CHILD" 
        }
        
        val childrenWithParents = mutableMapOf<Int, MutableSet<Int>>()
        
        for (rel in parentChildRelations) {
            val pIdx = rel.personIndex!!
            val rpIdx = rel.relatedPersonIndex!!
            
            val (parentIdx, childIdx) = if (rel.relationType == "PARENT") {
                pIdx to rpIdx
            } else {
                rpIdx to pIdx
            }
            
            childrenWithParents.getOrPut(childIdx) { mutableSetOf() }.add(parentIdx)
        }
        
        // Group children by parent combination
        val parentCombinations = mutableMapOf<Set<Int>, MutableList<Int>>()
        
        for ((child, parents) in childrenWithParents) {
            parentCombinations.getOrPut(parents) { mutableListOf() }.add(child)
        }
        
        // Create families for each unique parent combination
        for ((parents, children) in parentCombinations) {
            val parentsList = parents.toList()
            
            // Check if we already created a family for these spouses
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
     * Finds children of a parent couple.
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
            .mapNotNull { 
                if (it.relationType == "PARENT") it.relatedPersonIndex else it.personIndex 
            }
            .toSet()
        
        val childrenOfParent2 = relationships
            .filter { 
                (it.relationType == "PARENT" && it.personIndex == parent2) ||
                (it.relationType == "CHILD" && it.relatedPersonIndex == parent2)
            }
            .mapNotNull { 
                if (it.relationType == "PARENT") it.relatedPersonIndex else it.personIndex 
            }
            .toSet()
        
        // Common children of both parents
        return (childrenOfParent1 intersect childrenOfParent2).toList()
    }
    
    /**
     * Creates a Family object from FamilyGroup.
     */
    private fun createFamily(
        index: Int,
        familyGroup: FamilyGroup,
        individuals: List<Individual>
    ): Family? {
        val spouse1Id = familyGroup.spouse1?.let { individuals.getOrNull(it)?.id }
        val spouse2Id = familyGroup.spouse2?.let { individuals.getOrNull(it)?.id }
        val childrenIds = familyGroup.children.mapNotNull { individuals.getOrNull(it)?.id }
        
        // Family must have at least one member
        if (spouse1Id == null && spouse2Id == null && childrenIds.isEmpty()) {
            return null
        }
        
        // Determine gender of spouses for correct husband/wife assignment
        val husband = if (spouse1Id != null && individuals.getOrNull(familyGroup.spouse1!!)?.gender == Gender.MALE) {
            spouse1Id
        } else if (spouse2Id != null && individuals.getOrNull(familyGroup.spouse2!!)?.gender == Gender.MALE) {
            spouse2Id
        } else {
            spouse1Id  // Default to first spouse
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
     * Parses gender string to Gender enum.
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
     * Helper class for grouping family members.
     */
    private data class FamilyGroup(
        val spouse1: Int?,
        val spouse2: Int?,
        val children: List<Int>
    )
}
