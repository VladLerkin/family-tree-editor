package com.family.tree.core.di

import com.family.tree.core.platform.VoiceRecorder
import org.koin.dsl.module

val platformModule = module {
    single { VoiceRecorder(null) }
}
