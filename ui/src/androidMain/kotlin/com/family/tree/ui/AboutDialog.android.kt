package com.family.tree.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.family.tree.core.BuildConfig

/**
 * Android implementation of AboutDialog.
 * Uses Material3 AlertDialog for native Android look and feel.
 */
@Composable
actual fun AboutDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        },
        title = {
            Text(
                text = APP_NAME,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            AboutDialogContent(
                onDismiss = onDismiss,
                onOpenEmail = { openEmail(context, it) },
                onOpenUrl = { openUrl(context, it) }
            )
        }
    )
}

private fun openEmail(context: Context, email: String) {
    try {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:$email")
        }
        context.startActivity(intent)
    } catch (ex: Exception) {
        // If no email app is available, try to open in browser as fallback
        try {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("mailto:$email"))
            context.startActivity(browserIntent)
        } catch (ex2: Exception) {
            System.err.println("Could not open email client: ${ex2.message}")
        }
    }
}

private fun openUrl(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    } catch (ex: Exception) {
        System.err.println("Could not open URL: ${ex.message}")
    }
}
