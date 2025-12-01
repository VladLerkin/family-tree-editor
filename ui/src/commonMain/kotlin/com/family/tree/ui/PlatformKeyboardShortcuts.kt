package com.family.tree.ui

import androidx.compose.ui.Modifier

/**
 * Cross-platform keyboard shortcuts hook.
 * - desktopMain provides a real implementation (Esc, +/-)
 * - androidMain is a no-op for now
 */
expect fun Modifier.platformKeyboardShortcuts(
    onEscape: () -> Unit = {},
    onZoomIn: () -> Unit = {},
    onZoomOut: () -> Unit = {}
): Modifier
