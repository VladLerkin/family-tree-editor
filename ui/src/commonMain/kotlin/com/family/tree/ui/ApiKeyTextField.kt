package com.family.tree.ui

import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.InterceptPlatformTextInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import kotlinx.coroutines.awaitCancellation

/**
 * Visual transformation for masking the API key.
 * Shows the first 4 characters, then asterisks, then the last 4 characters.
 */
class ApiKeyVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val original = text.text
        
        // If the key is short or empty, show it as is
        if (original.length <= 12) {
            return TransformedText(text, OffsetMapping.Identity)
        }
        
        // Show the first 4 characters, 6 asterisks, the last 4 characters
        val masked = buildString {
            append(original.take(4))
            append("******")
            append(original.takeLast(4))
        }
        
        return TransformedText(
            AnnotatedString(masked),
            object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int {
                    return when {
                        offset <= 4 -> offset
                        offset >= original.length - 4 -> offset - original.length + masked.length
                        else -> 4 + 3 // middle of asterisks
                    }
                }
                
                override fun transformedToOriginal(offset: Int): Int {
                    return when {
                        offset <= 4 -> offset
                        offset >= masked.length - 4 -> offset - masked.length + original.length
                        else -> 4 + (original.length - 8) / 2 // middle of original text
                    }
                }
            }
        )
    }
}

/**
 * A reusable TextField for entering API keys with masking and copy/paste protection.
 *
 * Keys are only ever pasted, so on touch platforms the soft keyboard is suppressed and a
 * paste button replaces the long-press menu (blocked by [DisableSelection]).
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ApiKeyTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    supportingText: String,
    modifier: Modifier = Modifier
) {
    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current
    
    val field = @Composable {
        DisableSelection {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            supportingText = { Text(supportingText) },
            visualTransformation = ApiKeyVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { clipboardManager.getText()?.text?.let(onValueChange) }) {
                    Icon(Icons.Default.ContentPaste, contentDescription = "Paste")
                }
            },
            modifier = modifier
                .onPreviewKeyEvent { keyEvent ->
                    // Custom copy/paste handling because selection is disabled
                    if (keyEvent.type == KeyEventType.KeyDown) {
                        val isCopy = (keyEvent.isMetaPressed || keyEvent.isCtrlPressed) && keyEvent.key == Key.C
                        if (isCopy) return@onPreviewKeyEvent true // Block copy if needed, or allow it. Logic below allowed copy/paste.
                        
                        val isPaste = (keyEvent.isMetaPressed || keyEvent.isCtrlPressed) && keyEvent.key == Key.V
                        if (isPaste) {
                            clipboardManager.getText()?.text?.let { onValueChange(it) }
                            return@onPreviewKeyEvent true
                        }
                    }
                    false
                },
            singleLine = true
        )
        }
    }

    if (PlatformEnv.isDesktop) {
        field()
    } else {
        // Swallow the IME session so tapping the field never opens the keyboard.
        InterceptPlatformTextInput(interceptor = { _, _ -> awaitCancellation() }, content = field)
    }
}
