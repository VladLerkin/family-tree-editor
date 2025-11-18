package com.family.tree.ui

import androidx.compose.runtime.Composable

actual object PlatformEnv {
    actual val isDesktop: Boolean = false
}

@Composable
actual fun rememberPlatformContext(): Any? {
    return null
}
