package com.family.tree.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

@Composable
actual fun ExitAppAction(onExit: () -> Unit) {
    LaunchedEffect(Unit) {
        onExit()
        // В веб-приложении нельзя закрыть окно браузера программно
        // Можно только показать сообщение или перезагрузить страницу
    }
}
