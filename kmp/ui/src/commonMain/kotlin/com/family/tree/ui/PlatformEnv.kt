package com.family.tree.ui

import androidx.compose.runtime.Composable

expect object PlatformEnv {
    val isDesktop: Boolean
}

/**
 * Returns platform-specific context object.
 * - On Android: returns android.content.Context
 * - On other platforms: returns null
 */
@Composable
expect fun rememberPlatformContext(): Any?
