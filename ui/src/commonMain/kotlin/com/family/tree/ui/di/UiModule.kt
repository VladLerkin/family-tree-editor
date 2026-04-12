package com.family.tree.ui.di

import androidx.lifecycle.viewModelScope
import com.family.tree.core.ai.VoiceInputProcessor
import com.family.tree.ui.MainViewModel
import org.koin.core.module.Module
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module

val uiModule = module {
    single { 
        MainViewModel(
            searchService = get(),
            voiceInputProcessorFactory = { scope -> get<VoiceInputProcessor> { parametersOf(scope) } }
        )
    }
}
