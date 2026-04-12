package com.family.tree.core.di

import com.family.tree.core.ai.AiSettingsStorage
import com.family.tree.core.ai.VoiceInputProcessor
import com.family.tree.core.search.QuickSearchService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module

val coreModule = module {
    single { QuickSearchService() }
    single { AiSettingsStorage() }
    
    factory { (scope: CoroutineScope) ->
        VoiceInputProcessor(
            voiceRecorder = get(),
            settingsStorage = get(),
            coroutineScope = scope
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
