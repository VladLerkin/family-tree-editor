package com.family.tree.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.family.tree.core.BuildConfig

private const val APP_NAME = "Family Tree Editor"
private const val AUTHOR_EMAIL = "domfindus@gmail.com"
private const val GITHUB_URL = "https://github.com/VladLerkin/family-tree-editor"

@Composable
actual fun AboutDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.width(450.dp).height(300.dp),
            color = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
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
                    text = "Version v${BuildConfig.APP_VERSION}",
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

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "The source code is available on",
                        fontSize = 14.sp
                    )
                    TextButton(
                        onClick = { openUrl(GITHUB_URL) },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = "GitHub",
                            fontSize = 14.sp
                        )
                    }
                }

                Text(
                    text = "Please send all comments and feedback to the email above.",
                    fontSize = 14.sp,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.weight(1f))

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
