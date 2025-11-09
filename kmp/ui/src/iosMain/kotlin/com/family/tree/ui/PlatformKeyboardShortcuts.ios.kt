package com.family.tree.ui

import androidx.compose.ui.Modifier

// iOS actual: keyboard shortcuts no-op for now
actual fun Modifier.platformKeyboardShortcuts(
    onEscape: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit
): Modifier = this
