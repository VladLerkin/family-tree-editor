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
    suspend fun search(apiKey: String, query: String, searchDepth: String = "basic"): String {
        if (apiKey.isBlank()) {
            return "Error: Tavily API Key is empty."
        }
        return try {
            val request = TavilySearchRequest(api_key = apiKey, query = query, search_depth = searchDepth)
            val response = httpClient.post("https://api.tavily.com/search") {
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(request))
            }
            if (response.status.isSuccess()) {
                val responseBody = response.bodyAsText()
                val parsed = json.decodeFromString<TavilySearchResponse>(responseBody)
                buildString {
                    appendLine("Search results for '$query':")
                    parsed.answer?.let { 
                        appendLine("\n[AI SUMMARY - ATTENTION: This is an aggregated summary and may contain errors. Please verify facts using the specific source results below.]")
                        appendLine("Summary: $it") 
                    }
                    appendLine("\n[SPECIFIC SOURCE RESULTS]")
                    parsed.results.forEachIndexed { index, result ->
                        appendLine("\nSource #${index + 1}: ${result.title}")
                        appendLine("URL: ${result.url}")
                        appendLine("Snippet: ${result.content}")
                    }
                }
            } else {
                "Search failed with status: ${response.status}"
            }
        } catch (e: Exception) {
            "Search error: ${e.message}"
        }
    }
}
