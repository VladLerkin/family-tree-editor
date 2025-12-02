package com.family.tree.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

private const val APP_NAME = "Family Tree Editor"
private const val APP_VERSION = "v1.3.12"
private const val AUTHOR_EMAIL = "domfindus@gmail.com"

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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = APP_NAME,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

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
                        onClick = { openEmail(AUTHOR_EMAIL) },
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
                    fontSize = 14.sp,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(onClick = onDismiss) {
                        Text("OK")
                    }
                }
            }
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
