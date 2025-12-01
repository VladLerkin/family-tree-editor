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

private const val APP_NAME = "Family Tree Editor"
private const val APP_VERSION = "v1.3.2"
private const val AUTHOR_EMAIL = "domfindus@gmail.com"

/**
 * Android implementation of AboutDialog.
 * Uses Material3 AlertDialog for native Android look and feel.
 */
@Composable
actual fun AboutDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = APP_NAME,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Version $APP_VERSION",
                    fontSize = 14.sp
                )

                Text(
                    text = "This program is free software.",
                    fontSize = 14.sp
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Author: ",
                        fontSize = 14.sp
                    )
                    TextButton(
                        onClick = { openEmail(context, AUTHOR_EMAIL) },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = AUTHOR_EMAIL,
                            fontSize = 14.sp
                        )
                    }
                }

                Text(
                    text = "Please send all comments and feedback to the email above.",
                    fontSize = 14.sp
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
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
