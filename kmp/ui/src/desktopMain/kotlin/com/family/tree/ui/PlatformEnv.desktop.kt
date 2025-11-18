package com.family.tree.ui

import androidx.compose.runtime.Composable

actual object PlatformEnv {
    actual val isDesktop: Boolean = true
}

@Composable
actual fun rememberPlatformContext(): Any? {
    return null
}
