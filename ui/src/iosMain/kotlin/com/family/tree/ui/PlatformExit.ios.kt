package com.family.tree.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlin.system.exitProcess

@Composable
actual fun ExitAppAction(onExit: () -> Unit) {
    LaunchedEffect(Unit) {
        onExit()
        exitProcess(0)
    }
}
