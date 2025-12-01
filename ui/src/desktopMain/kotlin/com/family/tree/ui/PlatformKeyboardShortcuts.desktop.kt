package com.family.tree.ui

import androidx.compose.ui.Modifier

// Temporarily no-op to avoid API mismatches; will re-enable with Compose 1.7-safe APIs
actual fun Modifier.platformKeyboardShortcuts(
    onEscape: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit
): Modifier = this