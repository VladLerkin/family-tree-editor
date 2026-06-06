package com.family.tree.core.di

import com.family.tree.core.platform.VoiceRecorder
import com.family.tree.core.ai.AndroidModelDirectoryProvider
import com.family.tree.core.ai.ModelDirectoryProvider
import org.koin.dsl.module
import org.koin.android.ext.koin.androidContext

val platformModule = module {
    single { VoiceRecorder(androidContext()) }
    single<ModelDirectoryProvider> { AndroidModelDirectoryProvider(androidContext()) }
}
