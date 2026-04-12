package com.family.tree.core.di

import com.family.tree.core.platform.VoiceRecorder
import org.koin.dsl.module
import org.koin.android.ext.koin.androidContext

val platformModule = module {
    single { VoiceRecorder(androidContext()) }
}
