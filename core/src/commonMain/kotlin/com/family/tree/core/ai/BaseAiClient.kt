package com.family.tree.core.ai

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.Json

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Base class for AI clients containing common logic.
 */
abstract class BaseAiClient : AiClient, KoinComponent {
    protected val json: Json by inject()
    private val httpClient: HttpClient by inject()

    /**
     * Executes an HTTP POST request to the AI provider.
     *
     * @param url The URL to send the request to.
     * @param body The request body string.
     * @param timeoutMillis Request timeout in milliseconds.
     * @param configureBlock Optional block to configure the request (e.g. add headers).
     * @return The response body as text.
     */
    protected suspend fun executeRequest(
        url: String,
        body: String,
        timeoutMillis: Long = 120_000,
        configureBlock: HttpRequestBuilder.() -> Unit = {}
    ): String {
        val response = httpClient.post(url) {
            timeout {
                requestTimeoutMillis = timeoutMillis
                socketTimeoutMillis = timeoutMillis
            }
            contentType(ContentType.Application.Json)
            setBody(body)
            configureBlock()
        }
        
        return response.bodyAsText()
    }
}
