package com.family.tree.core.ai.agent

import ai.koog.agents.core.tools.annotations.Tool
import com.family.tree.core.ProjectData
import com.family.tree.core.ai.AiClient
import com.family.tree.core.ai.AiConfig
import com.family.tree.core.platform.ResourceLoader
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.*

class GenealogyTools(
        private val projectData: ProjectData,
        private val tavilyClient: TavilyClient,
        val httpClient: HttpClient,
        private val apiKey: String,
        private val repoPath: String = "files/autoresearch-genealogy",
        private val aiClient: AiClient? = null,
        private val aiConfig: AiConfig? = null,
        private val pamyatNarodaCookies: String? = null,
        private val familySearchCookies: String? = null,
        private val onLog: (String) -> Unit = {}
) {
    private val loader = ResourceLoader()
    private var liveCookies: String? = pamyatNarodaCookies
    private var liveFamilySearchCookies: String? = familySearchCookies
    private var liveStaticHash: String? = null

    @Tool("Read a specific methodology workflow guide (e.g., 'discrepancy-resolution.md').")
    suspend fun readMethodology(fileName: String): String {
        if (fileName.isBlank() || fileName == "null") {
            return "Error: fileName is required. Use 'listMethodologyGuides' to find the available file names first."
        }
        onLog("📖 [TOOL] readMethodology(\"$fileName\")")
        val result =
                loader.readFile(repoPath, "workflows/$fileName")
                        ?: "Error: Workflow $fileName not found in $repoPath/workflows/"
        onLog("📖 [TOOL RESULT] readMethodology → $result")
        return result
    }

    @Tool(
            "Read an archive guide for a specific country or region (e.g., 'norway.md', 'usa-census.md')."
    )
    suspend fun readArchiveGuide(fileName: String): String {
        if (fileName.isBlank() || fileName == "null") {
            return "Error: fileName is required. Use 'listArchiveGuides' to find the available file names first."
        }
        onLog("📖 [TOOL] readArchiveGuide(\"$fileName\")")
        val result =
                loader.readFile(repoPath, "archives/$fileName")
                        ?: "Error: Archive guide $fileName not found in $repoPath/archives/"
        onLog("📖 [TOOL RESULT] readArchiveGuide → $result")
        return result
    }

    @Tool("List all available archive guides for countries and regions.")
    suspend fun listArchiveGuides(): String {
        onLog("📋 [TOOL] listArchiveGuides()")
        val files = loader.listDirectory(repoPath, "archives")
        val result =
                if (files.isEmpty()) {
                    "No archive guides found."
                } else {
                    "Available archive guides: ${files.joinToString(", ")}"
                }
        if (files.isNotEmpty()) {
            onLog(
                    "📋 [TOOL RESULT] listArchiveGuides found ${files.size} files:\n" +
                            files.joinToString("\n") { "  - $it" }
            )
        } else {
            onLog("📋 [TOOL RESULT] listArchiveGuides → $result")
        }
        return result
    }

    @Tool("List all available professional methodology workflow guides.")
    suspend fun listMethodologyGuides(): String {
        onLog("📋 [TOOL] listMethodologyGuides()")
        val files = loader.listDirectory(repoPath, "workflows")
        val result =
                if (files.isEmpty()) {
                    "No methodology guides found."
                } else {
                    "Available methodology guides: ${files.joinToString(", ")}"
                }
        if (files.isNotEmpty()) {
            onLog(
                    "📋 [TOOL RESULT] listMethodologyGuides found ${files.size} files:\n" +
                            files.joinToString("\n") { "  - $it" }
            )
        } else {
            onLog("📋 [TOOL RESULT] listMethodologyGuides → $result")
        }
        return result
    }

    @Tool("List all available research examples showing how to apply methodology.")
    suspend fun listExamples(): String {
        onLog("📋 [TOOL] listExamples()")
        val files = loader.listDirectory(repoPath, "examples")
        val result =
                if (files.isEmpty()) {
                    "No research examples found."
                } else {
                    "Available examples: ${files.joinToString(", ")}"
                }
        if (files.isNotEmpty()) {
            onLog(
                    "📋 [TOOL RESULT] listExamples found ${files.size} files:\n" +
                            files.joinToString("\n") { "  - $it" }
            )
        } else {
            onLog("📋 [TOOL RESULT] listExamples → $result")
        }
        return result
    }

    @Tool(
            "Read a specific research example (e.g., 'tree-expansion-session.md') to see professional protocols in action."
    )
    suspend fun readExample(fileName: String): String {
        if (fileName.isBlank() || fileName == "null") {
            return "Error: fileName is required. Use 'listExamples' to find the available file names first."
        }
        onLog("📖 [TOOL] readExample(\"$fileName\")")
        val result =
                loader.readFile(repoPath, "examples/$fileName")
                        ?: "Error: Example $fileName not found in $repoPath/examples/"
        onLog("📖 [TOOL RESULT] readExample → $result")
        return result
    }

    @Tool("List all available reference guides (standard conventions, glossaries).")
    suspend fun listReferenceGuides(): String {
        onLog("📋 [TOOL] listReferenceGuides()")
        val files = loader.listDirectory(repoPath, "reference")
        val result =
                if (files.isEmpty()) {
                    "No reference guides found."
                } else {
                    "Available reference guides: ${files.joinToString(", ")}"
                }
        if (files.isNotEmpty()) {
            onLog(
                    "📋 [TOOL RESULT] listReferenceGuides found ${files.size} files:\n" +
                            files.joinToString("\n") { "  - $it" }
            )
        } else {
            onLog("📋 [TOOL RESULT] listReferenceGuides → $result")
        }
        return result
    }

    @Tool("Read a specific reference guide (e.g., 'gedcom-guide.md', 'confidence-tiers.md').")
    suspend fun readReferenceGuide(fileName: String): String {
        if (fileName.isBlank() || fileName == "null") {
            return "Error: fileName is required. Use 'listReferenceGuides' to find the available file names first."
        }
        onLog("📖 [TOOL] readReferenceGuide(\"$fileName\")")
        val result =
                loader.readFile(repoPath, "reference/$fileName")
                        ?: "Error: Reference guide $fileName not found in $repoPath/reference/"
        onLog("📖 [TOOL RESULT] readReferenceGuide → $result")
        return result
    }

    @Tool(
            "Get a summary of all unique geographic locations (countries, regions) found in the family tree."
    )
    fun getGeographicProfile(): String {
        onLog("🗺️ [TOOL] getGeographicProfile()")
        val places =
                projectData
                        .individuals
                        .flatMap {
                            it.events.mapNotNull { e -> e.place.trim() }.filter { it.isNotBlank() }
                        }
                        .distinct()
                        .sorted()

        val result =
                if (places.isEmpty()) {
                    "No geographic locations found in the current family tree."
                } else {
                    "The family tree contains the following locations: ${places.joinToString(", ")}"
                }
        onLog("🗺️ [TOOL RESULT] getGeographicProfile → $result")
        return result
    }

    @Tool(
            "Search for specific individuals or facts in the local family tree by name or location keywords."
    )
    fun searchFamilyTree(query: String): String {
        onLog("🔍 [TOOL] searchFamilyTree(query=\"$query\")")
        val results =
                projectData
                        .individuals
                        .filter { person ->
                            person.displayName.contains(query, ignoreCase = true) ||
                                    person.events.any {
                                        it.place.contains(query, ignoreCase = true)
                                    } ||
                                    person.events.any { it.date.contains(query, ignoreCase = true) }
                        }
                        .take(10)

        val result =
                if (results.isEmpty()) {
                    "No matching individuals found in the local tree for query: '$query'."
                } else {
                    "Found ${results.size} matches:\n" +
                            results.joinToString("\n") {
                                "- ${it.displayName} (${it.birthYear ?: "?"} - ${it.deathYear ?: "?"})"
                            }
                }
        onLog("🔍 [TOOL RESULT] searchFamilyTree → $result")
        return result
    }

    @Tool(
            "CRITICAL: Do NOT use for specific person search if person is eligible for specialized archives (e.g. WWII, USSR 1880-1930). Use ONLY for finding archive URLs, general history, or if specialized tools fail or do not exist."
    )
    suspend fun generalWebSearch(
            name: String = "",
            birth_location: String? = null,
            life_span: String? = null,
            query: String? = null,
            region: String? = null,
            targetSite: String? = null
    ): String {
        val baseQuery =
                if (query.isNullOrBlank()) {
                    buildString {
                        if (name.isNotBlank()) append(name)
                        birth_location?.let { append(" born in $it") }
                        life_span?.let { append(" ($it)") }
                        region?.let { append(" $it") }
                        targetSite?.let { append(" site:$it") }
                        if (isNotBlank()) append(" genealogy records")
                        else append("genealogy records")
                    }
                } else {
                    var q = query
                    if (name.isNotBlank() && !q.contains(name, ignoreCase = true)) q = "$name $q"
                    if (region != null && !q.contains(region, ignoreCase = true)) q = "$q $region"
                    if (targetSite != null && !q.contains(targetSite, ignoreCase = true))
                            q = "$q site:$targetSite"
                    q
                }

        onLog(
                "🌐 [TAVILY REQUEST] query=\"$baseQuery\"  apiKey=${if (apiKey.isBlank()) "MISSING!" else "***${apiKey.takeLast(4)}"}"
        )

        if (apiKey.isBlank()) {
            val err = "❌ Tavily API key is not set. Go to AI Settings and enter your Tavily key."
            onLog(err)
            return err
        }

        val result = tavilyClient.search(apiKey, baseQuery, searchDepth = "advanced", onLog = onLog)

        if (aiClient != null && aiConfig != null) {
            onLog("🤖 [TOOL SUB-AGENT] Summarizing raw search results to save tokens...")
            try {
                val summaryPrompt =
                        """
                    You are a sub-agent for a genealogy researcher. 
                    The main researcher requested a web search targeting the individual or query: "$baseQuery".
                    Below are the raw search results returned by Tavily.
                    Your task is to filter out all noise and extract ONLY the relevant genealogical facts (births, deaths, relatives, locations) that strictly belong to the primary person in the query. Do NOT include facts about other people who happen to share a similar last name or appear in the same document.
                    You MUST preserve the specific source ID (e.g. Source #1) and the exact verbatim URL for every extracted fact so the main agent can cite them.
                    If the results contain no relevant genealogical facts for THIS specific person, clearly state that no relevant facts were found.
                    Do NOT hallucinate. Do NOT invent URLs. Keep the summary under 1500 characters.

                    RAW RESULTS:
                    $result
                """.trimIndent()
                val summarized = aiClient.sendPrompt(summaryPrompt, aiConfig)
                onLog("🤖 [TOOL SUB-AGENT RESULT] $summarized")
                return summarized
            } catch (e: Exception) {
                onLog(
                        "⚠️ [TOOL SUB-AGENT ERROR] Failed to summarize. Returning raw data. Error: ${e.message}"
                )
            }
        }

        val logsToShow = result.take(1500)
        onLog(
                "🌐 [TAVILY RESPONSE] ${if (result.length > 1500) "$logsToShow... (truncated in logs)" else result}"
        )
        return result
    }
    @Tool(
            "[MANDATORY PRIMARY TOOL for USSR/Russia 1890-1930] Search the 'Pamyat Naroda' WWII database for Soviet military records. Target individuals born ~1880-1930 who could have served in 1941-1945."
    )
    suspend fun searchPamyatNaroda(
            firstName: String,
            lastName: String,
            patronymic: String? = null,
            birthYear: String? = null
    ): String {
        onLog("🔎 [PAMYAT NARODA] Query: $lastName $firstName ${patronymic ?: ""} ($birthYear)")

        // 0. Age Filter: 1890 - 1930
        val year = birthYear?.toIntOrNull()
        if (year != null && (year < 1890 || year > 1930)
        ) { // Slightly expanded range for late starters
            val msg =
                    "Skipping Pamyat Naroda search: Year $year is outside the WWII candidate range (1890-1935)."
            onLog("⚠️ [PAMYAT NARODA] $msg")
            return msg
        }

        // 1. Fetch tokens first
        val tokens =
                try {
                    fetchSecurityTokens()
                } catch (e: Exception) {
                    onLog("⚠️ [PAMYAT NARODA] Failed to fetch security tokens: ${e.message}")
                    null
                }
        val csrfToken = tokens?.first
        val staticHash = tokens?.second ?: liveStaticHash

        // IMPORTANT: Inject tokens into cookies if they are not already there
        val tokensToInject = mutableListOf<String>()
        csrfToken?.let { tokensToInject.add("csrf=$it") }
        staticHash?.let { tokensToInject.add("static_hash=$it") }
        if (tokensToInject.isNotEmpty()) {
            updateLiveCookies(tokensToInject)
        }

        val apiUrl = "https://pamyat-naroda.ru/entrypoint/api/"
        return try {
            onLog("🔎 [PAMYAT NARODA REQUEST] URL: $apiUrl")
            val response =
                    httpClient.post(apiUrl) {
                        header(
                                "User-Agent",
                                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36"
                        )
                        header("Referer", "https://pamyat-naroda.ru/heroes/")
                        header("Origin", "https://pamyat-naroda.ru")
                        header("X-Requested-With", "XMLHttpRequest")
                        header("Accept", "application/json, text/javascript, */*; q=0.01")
                        header("Sec-Fetch-Dest", "empty")
                        header("Sec-Fetch-Mode", "cors")
                        header("Sec-Fetch-Site", "same-origin")

                        if (!csrfToken.isNullOrBlank()) {
                            header("X-Csrf-Token", csrfToken)
                        }

                        if (!staticHash.isNullOrBlank()) {
                            header("X-Static-Hash", staticHash)
                        }

                        if (!liveCookies.isNullOrBlank()) {
                            header("Cookie", liveCookies?.trim())
                        }
                        contentType(ContentType.Application.Json)
                        setBody(
                                """
                    {
                        "entrypoint": "heroes/search",
                        "parameters": {
                            "query": {
                                "first_name": "$firstName",
                                "last_name": "$lastName",
                                "middle_name": "${patronymic ?: ""}",
                                "birth_date_from": "${birthYear ?: ""}",
                                "birth_place_ids": [],
                                "award_id": [],
                                "location": [],
                                "exclude_ids": [],
                                "exclude_guids": [],
                                "kld_source_id": [],
                                "kr_source_id": [],
                                "division_ids": [],
                                "exclude_birthplace_region_ids": []
                            },
                            "page": 1,
                            "size": 10,
                            "options": {
                                "person": true
                            }
                        }
                    }
                """.trimIndent()
                        )
                    }
            val body = response.bodyAsText()
            onLog(
                    "🔎 [PAMYAT NARODA RESPONSE] Received ${body.length} chars: ${if (body.length > 500) body.take(500) + "..." else body}"
            )

            if (body.contains("url='https://pamyat-naroda.ru/login/'") ||
                            body.contains("href=\"https://pamyat-naroda.ru/login/\"")
            ) {
                onLog("⚠️ [PAMYAT NARODA] SESSION EXPIRED OR LOGIN REQUIRED.")
                val cookieSnippet =
                        if (!liveCookies.isNullOrBlank()) {
                            liveCookies?.take(10) + "..."
                        } else "empty"
                onLog(
                        "🔎 [PAMYAT NARODA DEBUG] Cookie length: ${liveCookies?.length ?: 0}, snippet: $cookieSnippet"
                )
                onLog(
                        "💡 TIP: Log in to pamyat-naroda.ru in your browser, go to Network tab, copy full 'Cookie' header, and paste it in AI Settings."
                )
                return "Error: Pamyat Naroda requires authorization. Please provide cookies in AI Settings."
            }

            if (aiClient != null && aiConfig != null) {
                onLog("🤖 [TOOL SUB-AGENT] Cleaning and summarizing Pamyat Naroda API response...")
                val cleanedData =
                        try {
                            cleanPamyatNarodaData(body)
                        } catch (e: Exception) {
                            onLog(
                                    "⚠️ [PAMYAT NARODA] JSON parsing failed, falling back to raw truncation: ${e.message}"
                            )
                            body.take(4000)
                        }

                val summaryPrompt =
                        """
                    Extract Soviet WWII military records from this Pamyat Naroda data for $lastName $firstName ${patronymic ?: ""}.
                    The data below has been cleaned and structured. 
                    Include ranks, units, awards, dates of birth, and dates of death/MIA for all potentially matching individuals.
                    If multiple records seem to belong to the same person, consolidate them into a single military path.
                    If nothing found, say so. 
                    
                    DATA:
                    $cleanedData
                """.trimIndent()
                val summarized = aiClient.sendPrompt(summaryPrompt, aiConfig)
                onLog("🤖 [TOOL SUB-AGENT RESULT] $summarized")
                summarized
            } else {
                body.take(1500)
            }
        } catch (e: Exception) {
            onLog("❌ [PAMYAT NARODA ERROR] ${e.message}")
            "Error searching Pamyat Naroda: ${e.message}"
        }
    }

    @Tool(
        "Search the FamilySearch database for historical records. Requires session cookies in AI Settings. " +
                "Useful for finding birth, marriage, and death records globally."
    )
    suspend fun searchFamilySearch(
        firstName: String,
        lastName: String,
        birthYear: String? = null,
        birthPlace: String? = null,
        deathYear: String? = null,
        deathPlace: String? = null
    ): String {
        onLog("🔎 [FAMILYSEARCH] Query: $lastName $firstName (born $birthYear in $birthPlace)")

        if (liveFamilySearchCookies.isNullOrBlank()) {
            val err = "Error: FamilySearch cookies are not set. Go to AI Settings and paste your cookies."
            onLog("⚠️ [FAMILYSEARCH] $err")
            return err
        }

        val baseUrl = "https://www.familysearch.org/service/search/hr/v2/personas"
        return try {
            val response = httpClient.get(baseUrl) {
                url {
                    parameters.append("count", "20")
                    parameters.append("offset", "0")
                    parameters.append("m.defaultFacets", "on")
                    parameters.append("m.facetNestCollectionInCategory", "on")
                    parameters.append("m.queryRequireDefault", "on")
                    
                    if (firstName.isNotBlank()) parameters.append("q.givenName", firstName)
                    if (lastName.isNotBlank()) parameters.append("q.surname", lastName)
                    
                    if (!birthYear.isNullOrBlank()) parameters.append("q.birthLikeDate", birthYear)
                    if (!birthPlace.isNullOrBlank()) parameters.append("q.birthLikePlace", birthPlace)
                    if (!deathYear.isNullOrBlank()) parameters.append("q.deathLikeDate", deathYear)
                    if (!deathPlace.isNullOrBlank()) parameters.append("q.deathLikePlace", deathPlace)
                }
                header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36")
                header("Accept", "application/json")
                header("Cookie", liveFamilySearchCookies?.trim() ?: "")
            }

            val fullUrl = response.call.request.url.toString()
            onLog("🔎 [FAMILYSEARCH REQUEST] URL: $fullUrl")
            val body = response.bodyAsText()
            onLog(
                "🔎 [FAMILYSEARCH RESPONSE] Received ${body.length} chars: ${if (body.length > 500) body.take(500) + "..." else body}"
            )

            if (body.contains("\"error\"") || body.contains("\"errors\"")) {
                 onLog("⚠️ [FAMILYSEARCH] API returned an error. Cookies might be expired.")
                 return "Error: FamilySearch API returned an error. Please check your cookies."
            }

            if (aiClient != null && aiConfig != null) {
                onLog("🤖 [TOOL SUB-AGENT] Summarizing FamilySearch results...")
                val cleanedData = try {
                    cleanFamilySearchData(body)
                } catch (e: Exception) {
                    onLog("⚠️ [FAMILYSEARCH] JSON parsing failed: ${e.message}")
                    body.take(4000)
                }

                val summaryPrompt = """
                    Extract genealogical records from this FamilySearch data for $lastName $firstName.
                    The data below has been cleaned from JSON. 
                    Include dates, locations, and relationships (parents, spouse) for each record.
                    If multiple records seem to belong to the same person, group them.
                    
                    DATA:
                    $cleanedData
                """.trimIndent()
                val summarized = aiClient.sendPrompt(summaryPrompt, aiConfig)
                onLog("🤖 [TOOL SUB-AGENT RESULT] $summarized")
                summarized
            } else {
                body.take(1500)
            }
        } catch (e: Exception) {
            onLog("❌ [FAMILYSEARCH ERROR] ${e.message}")
            "Error searching FamilySearch: ${e.message}"
        }
    }

    private fun cleanFamilySearchData(json: String): String {
        val root = try { Json.parseToJsonElement(json).jsonObject } catch(e: Exception) { return "Invalid JSON" }
        val entries = root["entries"]?.jsonArray ?: return "No records found."
        
        return buildString {
            entries.forEachIndexed { index, entry ->
                val content = entry.jsonObject["content"]?.jsonObject ?: return@forEachIndexed
                val gedcomx = content["gedcomx"]?.jsonObject ?: return@forEachIndexed
                val persons = gedcomx["persons"]?.jsonArray ?: return@forEachIndexed
                
                // Map to resolve names by ID within this record
                val personMap = mutableMapOf<String, String>()
                persons.forEach { person ->
                    val pObj = person.jsonObject
                    val id = pObj["id"]?.jsonPrimitive?.content ?: ""
                    val name = pObj["names"]?.jsonArray?.firstOrNull()?.jsonObject
                        ?.get("nameForms")?.jsonArray?.firstOrNull()?.jsonObject
                        ?.get("fullText")?.jsonPrimitive?.content ?: "Unknown"
                    if (id.isNotBlank()) personMap[id] = name
                }

                appendLine("--- Record #${index + 1} ---")
                
                persons.forEach { person ->
                    val pObj = person.jsonObject
                    val isPrincipal = pObj["principal"]?.jsonPrimitive?.booleanOrNull == true
                    if (isPrincipal) append("[PRINCIPAL] ")
                    
                    val id = pObj["id"]?.jsonPrimitive?.content ?: ""
                    val name = personMap[id] ?: "Unknown"
                    appendLine("Person: $name")
                    
                    // Gender
                    pObj["gender"]?.jsonObject["type"]?.jsonPrimitive?.content?.let {
                         appendLine("  Gender: ${it.substringAfterLast("/")}")
                    }

                    // Facts
                    pObj["facts"]?.jsonArray?.forEach { fact ->
                        val type = fact.jsonObject["type"]?.jsonPrimitive?.content?.substringAfterLast("/") ?: "Fact"
                        val date = fact.jsonObject["date"]?.jsonObject?.get("original")?.jsonPrimitive?.content ?: ""
                        val place = fact.jsonObject["place"]?.jsonObject?.get("original")?.jsonPrimitive?.content ?: ""
                        if (date.isNotBlank() || place.isNotBlank()) {
                            appendLine("  $type: $date $place")
                        }
                    }
                }
                
                // Relationships
                gedcomx["relationships"]?.jsonArray?.forEach { rel ->
                    val relType = rel.jsonObject["type"]?.jsonPrimitive?.content?.substringAfterLast("/") ?: "Relationship"
                    val p1Id = rel.jsonObject["person1"]?.jsonObject?.get("resource")?.jsonPrimitive?.content?.removePrefix("#") ?: ""
                    val p2Id = rel.jsonObject["person2"]?.jsonObject?.get("resource")?.jsonPrimitive?.content?.removePrefix("#") ?: ""
                    
                    val p1Name = personMap[p1Id] ?: "Unknown"
                    val p2Name = personMap[p2Id] ?: "Unknown"
                    
                    appendLine("Relation: $p1Name ($relType) $p2Name")
                }

                appendLine()
                if (length > 40000) return@buildString
            }
        }
    }

    /**
     * Fetches the main page of Pamyat Naroda with user's cookies to extract safety tokens. Returns
     * Pair(csrf, static_hash)
     */
    private suspend fun fetchSecurityTokens(): Pair<String?, String?> {
        if (liveCookies.isNullOrBlank()) return null to null

        onLog("🔎 [PAMYAT NARODA] Attempting to fetch security tokens from /heroes/ page...")

        return try {
            val response =
                    httpClient.get("https://pamyat-naroda.ru/heroes/") {
                        header(
                                "User-Agent",
                                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36"
                        )
                        if (!liveCookies.isNullOrBlank()) {
                            header("Cookie", liveCookies?.trim() ?: "")
                        }
                    }

            // Capture new cookies from server
            val setCookieHeaders = response.headers.getAll("Set-Cookie")
            if (!setCookieHeaders.isNullOrEmpty()) {
                updateLiveCookies(setCookieHeaders)
            }

            val html = response.bodyAsText()

            // Robust extractor: handles value/name in any order
            fun extractByRegex(name: String): String? {
                val patterns =
                        listOf(
                                Regex("""name="$name"\s+value="([^"]+)""""),
                                Regex("""value="([^"]+)"\s+name="$name"""")
                        )
                return patterns.firstNotNullOfOrNull { it.find(html)?.groupValues?.get(1) }
            }

            val csrf = extractByRegex("csrf")
            val sHash = extractByRegex("static_hash")

            if (csrf != null) {
                onLog(
                        "🔎 [PAMYAT NARODA] CSRF Token found (length ${csrf.length}): ${csrf.take(15)}..."
                )
            } else {
                onLog(
                        "⚠️ [PAMYAT NARODA] CSRF Token NOT found in HTML. Try logging in again in browser."
                )
            }

            if (sHash != null) {
                onLog("🔎 [PAMYAT NARODA] Static Hash found: ${sHash.take(8)}...")
                liveStaticHash = sHash
            }

            csrf to sHash
        } catch (e: Exception) {
            onLog("⚠️ [PAMYAT NARODA] Error fetching security tokens: ${e.message}")
            null to null
        }
    }

    /** Merges current cookies with new ones from Set-Cookie headers. */
    private fun updateLiveCookies(setCookieHeaders: List<String>) {
        val cookieMap = mutableMapOf<String, String>()

        // Parse existing cookies
        liveCookies?.split(";")?.forEach { pair ->
            val parts = pair.split("=", limit = 2)
            if (parts.size == 2) {
                cookieMap[parts[0].trim()] = parts[1].trim()
            }
        }

        // Parse and merge new cookies
        var updated = false
        setCookieHeaders.forEach { header ->
            // Set-Cookie format: "name=value; Path=/; ..."
            val firstPart = header.split(";").firstOrNull()
            if (firstPart != null) {
                val parts = firstPart.split("=", limit = 2)
                if (parts.size == 2) {
                    val name = parts[0].trim()
                    val value = parts[1].trim()
                    if (cookieMap[name] != value) {
                        cookieMap[name] = value
                        updated = true
                    }
                }
            }
        }

        if (updated) {
            liveCookies = cookieMap.entries.joinToString("; ") { "${it.key}=${it.value}" }
            onLog("🔎 [PAMYAT NARODA] Cookies updated from server response.")
        }
    }

    /**
     * Parses the Pamyat Naroda JSON and extracts key military fields to save tokens and improve
     * readability.
     */
    private fun cleanPamyatNarodaData(json: String): String {
        val root = Json.parseToJsonElement(json).jsonObject
        val dataArray = root["data"]?.jsonArray ?: return "No data found."

        fun JsonElement?.toSafeString(): String? {
            return when (this) {
                null -> null
                is JsonPrimitive -> content
                is JsonArray -> joinToString(", ") { it.toSafeString() ?: "" }
                is JsonObject -> {
                    // Try to finding 'name' or 'value' or just take the first primitive
                    this["name"]?.toSafeString()
                            ?: this["value"]?.toSafeString()
                                    ?: this.values
                                    .firstOrNull { it is JsonPrimitive }
                                    ?.toSafeString()
                }
                else -> toString()
            }
        }

        return buildString {
            dataArray.forEachIndexed { index, element ->
                val source = element.jsonObject["_source"]?.jsonObject ?: return@forEachIndexed
                appendLine("--- Record #${index + 1} ---")

                val last = source["last_name"].toSafeString() ?: ""
                val first = source["first_name"].toSafeString() ?: ""
                val middle = source["middle_name"].toSafeString() ?: ""
                appendLine("Name: $last $first $middle")

                source["date_birth"].toSafeString()?.let { appendLine("Birth Date: $it") }
                source["birth_place"].toSafeString()?.let { appendLine("Birth Place: $it") }
                source["rank"].toSafeString()?.let { appendLine("Rank: $it") }
                source["prizv"].toSafeString()?.let { appendLine("Drafted/RVK: $it") }

                // Units can be in multiple fields
                val unit = source["unit"].toSafeString()
                val division = source["division"].toSafeString()
                val vchResource = source["vch_name"].toSafeString()
                val combinedUnit =
                        listOfNotNull(unit, division, vchResource)
                                .filter { it.isNotBlank() }
                                .joinToString(", ")
                if (combinedUnit.isNotBlank()) appendLine("Unit: $combinedUnit")

                source["award_name"].toSafeString()?.let { appendLine("Award: $it") }
                source["doc_name"].toSafeString()?.let { appendLine("Document: $it") }
                source["date_death"].toSafeString()?.let { appendLine("Death/MIA Date: $it") }
                source["date_mub"].toSafeString()?.let { appendLine("Date MUB: $it") }
                source["date_disposal"].toSafeString()?.let { appendLine("Disposal Date: $it") }
                source["date_exit"].toSafeString()?.let {
                    appendLine("Demobilization/Exit Date: $it")
                }
                source["date_demobilization"].toSafeString()?.let {
                    appendLine("Demobilization Date: $it")
                }
                source["disposal_place"].toSafeString()?.let { appendLine("Disposal Place: $it") }
                source["burial_place"].toSafeString()?.let { appendLine("Burial/Death Place: $it") }
                source["family"].toSafeString()?.let { appendLine("Family/Next of Kin: $it") }
                source["additional_info"].toSafeString()?.let { appendLine("Additional Info: $it") }

                appendLine()
                if (length > 40000) return@buildString // Safety limit for cleaning
            }
        }
                .ifBlank { "No military records extracted." }
    }
}

data class AgentProposal(val promptName: String, val taskDescription: String, val results: String)
