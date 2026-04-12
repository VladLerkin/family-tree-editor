package com.family.tree.ios

import androidx.compose.ui.window.ComposeUIViewController
import com.family.tree.core.di.initKoin as koinInit
import com.family.tree.core.di.platformModule
import com.family.tree.ui.App
import com.family.tree.ui.di.uiModule

fun MainViewController() = ComposeUIViewController { App() }

fun setupKoin() {
    KoinHelper.start()
}

object KoinHelper {
    private var isStarted = false

    fun start() {
        if (!isStarted) {
            koinInit(
                additionalModules = listOf(uiModule, platformModule)
            )
            isStarted = true
        }
    }
}
