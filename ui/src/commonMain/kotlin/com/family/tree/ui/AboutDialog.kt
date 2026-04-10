package com.family.tree.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.family.tree.core.BuildConfig

const val APP_NAME = "Family Tree Editor"
const val AUTHOR_EMAIL = "domfindus@gmail.com"
const val GITHUB_URL = "https://github.com/VladLerkin/family-tree-editor"

/**
 * Expect declaration for AboutDialog.
 * Desktop implementation shows a dialog with app info.
 * Other platforms can provide no-op implementations.
 */
@Composable
expect fun AboutDialog(onDismiss: () -> Unit)

@Composable
fun AboutDialogContent(
    onDismiss: () -> Unit,
    onOpenEmail: (String) -> Unit,
    onOpenUrl: (String) -> Unit
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
                onClick = { onOpenEmail(AUTHOR_EMAIL) },
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
                onClick = { onOpenUrl(GITHUB_URL) },
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
