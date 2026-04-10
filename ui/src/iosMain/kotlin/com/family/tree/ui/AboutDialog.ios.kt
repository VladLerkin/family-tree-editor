package com.family.tree.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

/**
 * iOS implementation of AboutDialog.
 * Uses Compose Dialog with Material3 components for cross-platform UI consistency.
 */
@Composable
actual fun AboutDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
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
        val url = NSURL.URLWithString("mailto:$email")
        if (url != null && UIApplication.sharedApplication.canOpenURL(url)) {
            UIApplication.sharedApplication.openURL(url)
        }
    } catch (ex: Exception) {
        println("Could not open email client: ${ex.message}")
    }
}

private fun openUrl(url: String) {
    try {
        val nsUrl = NSURL.URLWithString(url)
        if (nsUrl != null && UIApplication.sharedApplication.canOpenURL(nsUrl)) {
            UIApplication.sharedApplication.openURL(nsUrl)
        }
    } catch (ex: Exception) {
        println("Could not open browser: ${ex.message}")
    }
}
