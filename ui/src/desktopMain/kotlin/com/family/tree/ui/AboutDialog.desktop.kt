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
import androidx.compose.ui.window.DialogState
import java.awt.Desktop
import java.net.URI

private const val APP_NAME = "Family Tree Editor"
private const val APP_VERSION = "v1.3.10"
private const val AUTHOR_EMAIL = "domfindus@gmail.com"

@Composable
actual fun AboutDialog(onDismiss: () -> Unit) {
    Dialog(
        onCloseRequest = onDismiss,
        state = DialogState(width = 450.dp, height = 300.dp),
        title = "About $APP_NAME",
        resizable = false
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
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
