package com.family.tree.web

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.family.tree.core.di.initKoin
import com.family.tree.core.di.platformModule
import com.family.tree.ui.App
import com.family.tree.ui.di.uiModule
import kotlinx.browser.document

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    initKoin(
        additionalModules = listOf(uiModule, platformModule)
    )
    ComposeViewport(document.body!!) {
        App()
    }
}
