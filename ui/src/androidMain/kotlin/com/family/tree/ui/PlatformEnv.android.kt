package com.family.tree.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

actual object PlatformEnv {
    actual val isDesktop: Boolean = false
}

@Composable
actual fun rememberPlatformContext(): Any? {
    return LocalContext.current
}
