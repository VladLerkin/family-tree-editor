package com.family.tree.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogState
import androidx.compose.ui.window.DialogWindow
import java.awt.Desktop
import java.net.URI

@Composable
actual fun AboutDialog(onDismiss: () -> Unit) {
    DialogWindow(
        onCloseRequest = onDismiss,
        state = DialogState(width = 450.dp, height = 300.dp),
        title = "About $APP_NAME",
        resizable = false
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
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
        if (Desktop.isDesktopSupported()) {
            val desktop = Desktop.getDesktop()
            if (desktop.isSupported(Desktop.Action.MAIL)) {
                desktop.mail(URI("mailto:$email"))
            }
        }
    } catch (ex: Exception) {
        System.err.println("Could not open email client: ${ex.message}")
    }
}

private fun openUrl(url: String) {
    try {
        if (Desktop.isDesktopSupported()) {
            val desktop = Desktop.getDesktop()
            if (desktop.isSupported(Desktop.Action.BROWSE)) {
                desktop.browse(URI(url))
            }
        }
    } catch (ex: Exception) {
        System.err.println("Could not open browser: ${ex.message}")
    }
}
