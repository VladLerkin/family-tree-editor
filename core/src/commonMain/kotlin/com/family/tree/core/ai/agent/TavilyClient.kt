package com.family.tree.core.ai.agent

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class TavilySearchRequest(
    val api_key: String,
    val query: String,
    val search_depth: String = "basic",
    val include_images: Boolean = false,
    val include_answer: Boolean = false,
    val max_results: Int = 10
)

@Serializable
data class TavilySearchResult(
    val title: String,
    val url: String,
    val content: String,
    val score: Double
)

@Serializable
data class TavilySearchResponse(
    val answer: String? = null,
    val query: String,
    val results: List<TavilySearchResult>
)

class TavilyClient(
    private val httpClient: HttpClient,
    private val json: Json
) {
    suspend fun search(
        apiKey: String,
        query: String,
        searchDepth: String = "basic",
        onLog: (String) -> Unit = {}
    ): String {
        if (apiKey.isBlank()) {
            return "Error: Tavily API Key is empty."
        }
        return try {
            val request = TavilySearchRequest(
                api_key = apiKey,
                query = query,
                search_depth = searchDepth,
                include_answer = false
            )
            onLog("🔗 [HTTP POST] https://api.tavily.com/search  body={query=\"$query\", search_depth=\"$searchDepth\"}")

            val response = httpClient.post("https://api.tavily.com/search") {
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(request))
            }

            onLog("🔗 [HTTP STATUS] ${response.status}")

            if (response.status.isSuccess()) {
                val responseBody = response.bodyAsText()
                onLog("🔗 [HTTP RAW BODY] ${responseBody.take(500)}...")

                val parsed = json.decodeFromString<TavilySearchResponse>(responseBody)
                buildString {
                    appendLine("Search results for '$query':")
                    if (parsed.results.isEmpty()) {
                        appendLine("\n⚠️ No results returned by Tavily for this query.")
                    } else {
                        appendLine("\n[SPECIFIC SOURCE RESULTS] (${parsed.results.size} found)")
                        parsed.results.forEachIndexed { index, result ->
                            appendLine("\nSource #${index + 1}: ${result.title}")
                            appendLine("URL: ${result.url}")
                            appendLine("Snippet: ${result.content}")
                        }
                    }
                }
            } else {
                val body = response.bodyAsText()
                onLog("🔗 [HTTP ERROR BODY] $body")
                "Search failed with status: ${response.status}. Body: $body"
            }
        } catch (e: Exception) {
            val msg = "Search error [${e::class.simpleName}]: ${e.message ?: "(no message)"}"
            onLog("❌ [TAVILY EXCEPTION] $msg")
            msg
        }
    }
}
