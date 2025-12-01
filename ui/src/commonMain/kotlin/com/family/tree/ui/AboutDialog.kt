package com.family.tree.ui

import androidx.compose.runtime.Composable

/**
 * Expect declaration for AboutDialog.
 * Desktop implementation shows a dialog with app info.
 * Other platforms can provide no-op implementations.
 */
@Composable
expect fun AboutDialog(onDismiss: () -> Unit)
