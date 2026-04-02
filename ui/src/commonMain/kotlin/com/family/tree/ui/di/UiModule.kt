package com.family.tree.ui.di

import com.family.tree.ui.MainViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val uiModule = module {
    single { MainViewModel() }
}
