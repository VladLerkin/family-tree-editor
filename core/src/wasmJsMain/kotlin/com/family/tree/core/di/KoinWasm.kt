package com.family.tree.core.di

import com.family.tree.core.platform.VoiceRecorder
import com.family.tree.core.ai.WasmModelDirectoryProvider
import com.family.tree.core.ai.ModelDirectoryProvider
import org.koin.dsl.module

val platformModule = module {
    single { VoiceRecorder(null) }
    single<ModelDirectoryProvider> { WasmModelDirectoryProvider() }
}
