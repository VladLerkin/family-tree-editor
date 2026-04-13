package com.family.tree.core.di

import com.family.tree.core.ai.VoiceInputProcessor
import com.family.tree.core.ai.AiSettingsStorage
import com.family.tree.core.search.QuickSearchService
import com.family.tree.core.ai.agent.AgentService
import com.family.tree.core.export.MarkdownTreeExporter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import kotlinx.serialization.json.Json

val coreModule = module {
    single { QuickSearchService() }
    single { AiSettingsStorage() }
    single { MarkdownTreeExporter() }
    single { AgentService(get(), get(), get(), get()) }
    
    single { 
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
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
            coroutineScope = scope
        )
    }
    
    factory { (config: com.family.tree.core.ai.AiConfig?) -> 
        com.family.tree.core.ai.AiTextImporter(
            config ?: get<AiSettingsStorage>().loadConfig()
        ) 
    }
}

fun initKoin(
    additionalModules: List<Module> = emptyList(),
    appDeclaration: KoinApplication.() -> Unit = {}
) = startKoin {
    appDeclaration()
    modules(
        coreModule,
        *additionalModules.toTypedArray()
    )
}
