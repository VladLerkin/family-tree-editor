package com.family.tree.core.di

import com.family.tree.core.ai.*
import com.family.tree.core.ai.AiSettingsStorage
import com.family.tree.core.ai.VoiceInputProcessor
import com.family.tree.core.ai.agent.AgentService
import com.family.tree.core.ai.agent.TavilyClient
import com.family.tree.core.export.MarkdownTreeExporter
import com.family.tree.core.search.QuickSearchService
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.Json
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.module

val coreModule = module {
    single { QuickSearchService() }
    single { AiSettingsStorage() }
    single { MarkdownTreeExporter() }

    // AI Clients
    single { OpenAiClient() }
    single { GoogleClient() }
    single { YandexClient() }
    single { OllamaClient() }
    single { CustomClient() }
    single { LocalAiClient(get()) }
    single { LocalModelManager(get(), get()) }
    single { AiClientFactory(get(), get(), get(), get(), get(), get()) }

    // Transcription Clients
    single { OpenAiWhisperClient(get(), get()) }
    single { GoogleSpeechClient(get(), get()) }
    single { YandexSpeechClient(get(), get()) }
    single { TranscriptionClientFactory(get(), get(), get()) }

    // Tavily Client
    single { TavilyClient(get(), get()) }

    single { AgentService(get(), get(), get(), get(), get()) }

    single {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
            coerceInputValues = true
            explicitNulls = false
        }
    }

    single {
        HttpClient {
            install(HttpTimeout) {
                requestTimeoutMillis = 120_000
                connectTimeoutMillis = 30_000
                socketTimeoutMillis = 120_000
            }
        }
    }

    factory { (scope: CoroutineScope) ->
        VoiceInputProcessor(
                voiceRecorder = get(),
                settingsStorage = get(),
                transcriptionClientFactory = get(),
                coroutineScope = scope
        )
    }

    factory { (config: com.family.tree.core.ai.AiConfig?) ->
        com.family.tree.core.ai.AiTextImporter(
                config ?: get<AiSettingsStorage>().loadConfig(),
                get()
        )
    }
}

fun initKoin(
        additionalModules: List<Module> = emptyList(),
        appDeclaration: KoinApplication.() -> Unit = {}
) = startKoin {
    appDeclaration()
    modules(coreModule, *additionalModules.toTypedArray())
}
