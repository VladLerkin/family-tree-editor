package com.family.tree.core.di

import com.family.tree.core.io.RelRepository
import com.family.tree.core.search.QuickSearchService
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.module

val coreModule = module {
    // We register QuickSearchService without passing project yet, 
    // but QuickSearchService currently takes project in constructor!
    // So we might need to change it or pass project to it statically?
    // Let's hold off on specific services until we refactor them.
}

fun initKoin(platformModules: List<Module> = emptyList()) = startKoin {
    modules(
        coreModule,
        *platformModules.toTypedArray()
    )
}
