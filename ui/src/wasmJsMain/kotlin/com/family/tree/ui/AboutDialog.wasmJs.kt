package com.family.tree.ui

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
actual fun AboutDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.width(450.dp).height(300.dp),
            color = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.large
        ) {
            AboutDialogContent(
                onDismiss = onDismiss,
                onOpenEmail = { openEmail(it) },
                onOpenUrl = { openUrl(it) }
            )
        }
    }
}

private fun openEmail(email: String) {
    try {
        // TODO: Implement window.open when kotlinx-browser supports wasmJs
        println("Open email: $email")
    } catch (ex: Exception) {
        println("Could not open email client: ${ex.message}")
    }
}

private fun openUrl(url: String) {
    try {
        // TODO: Implement window.open when kotlinx-browser supports wasmJs
        println("Open URL: $url")
    } catch (ex: Exception) {
        println("Could not open browser: ${ex.message}")
    }
}
