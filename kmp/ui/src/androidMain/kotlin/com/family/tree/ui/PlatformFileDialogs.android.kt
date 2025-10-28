package com.family.tree.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast

@Composable
actual fun PlatformFileDialogs(
    showOpen: Boolean,
    onDismissOpen: () -> Unit,
    onOpenResult: (bytes: ByteArray?) -> Unit,
    showSave: Boolean,
    onDismissSave: () -> Unit,
    bytesToSave: () -> ByteArray
) {
    val context = LocalContext.current

    // OPEN — SAF ACTION_OPEN_DOCUMENT
    val openLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri: Uri? ->
            if (uri == null) {
                onOpenResult(null)
                onDismissOpen()
            } else {
                val bytes = runCatching {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                }.getOrNull()
                onOpenResult(bytes)
                onDismissOpen()
            }
        }
    )

    // SAVE — SAF CREATE_DOCUMENT
    val createLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
        onResult = { uri: Uri? ->
            if (uri != null) {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(bytesToSave())
                        out.flush()
                    }
                }
            }
            onDismissSave()
        }
    )

    if (showOpen) {
        LaunchedEffect(Unit) {
            // JSON first, allow any as fallback via user picker
            openLauncher.launch(arrayOf("application/json", "text/*", "application/*"))
        }
    }

    if (showSave) {
        LaunchedEffect(Unit) {
            // Suggest a default filename
            createLauncher.launch("project.json")
        }
    }
}
