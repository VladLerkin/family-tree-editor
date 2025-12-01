package com.family.tree.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.family.tree.core.ai.AiConfig
import com.family.tree.core.ai.AiPresets

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
 * Dialog for configuring AI settings before text import.
 */
@Composable
fun AiConfigDialog(
    initialConfig: AiConfig = AiPresets.OPENAI_GPT4O_MINI,
    onDismiss: () -> Unit,
    onConfirm: (AiConfig) -> Unit
) {
    val presets = AiPresets.getAllPresets()
    
    // Find the preset index that matches initialConfig.model
    val initialPresetIndex = presets.indexOfFirst { (_, preset) -> 
        preset.model == initialConfig.model && preset.provider == initialConfig.provider
    }.let { if (it >= 0) it else 0 }
    
    var selectedPresetIndex by remember { mutableStateOf(initialPresetIndex) }
    var provider by remember { mutableStateOf(initialConfig.provider) }
    var apiKey by remember { mutableStateOf(initialConfig.apiKey) }  // Deprecated, for backward compatibility
    var model by remember { mutableStateOf(initialConfig.model) }
    var baseUrl by remember { mutableStateOf(initialConfig.baseUrl) }
    var temperature by remember { mutableStateOf(initialConfig.temperature.toString()) }
    var maxTokens by remember { mutableStateOf(initialConfig.maxTokens.toString()) }
    var language by remember { mutableStateOf(initialConfig.language) }
    var transcriptionProvider by remember { mutableStateOf(initialConfig.transcriptionProvider) }
    var googleApiKey by remember { mutableStateOf(initialConfig.googleApiKey) }  // Deprecated
    
    // New separate API keys for each provider group
    var openaiApiKey by remember { mutableStateOf(initialConfig.openaiApiKey) }

    var googleAiApiKey by remember { mutableStateOf(initialConfig.googleAiApiKey) }
    var yandexApiKey by remember { mutableStateOf(initialConfig.yandexApiKey) }
    var yandexFolderId by remember { mutableStateOf(initialConfig.yandexFolderId) }
    
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .width(600.dp)
                .heightIn(max = 700.dp),
            shape = MaterialTheme.shapes.large,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "AI Settings for Text Import",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Text(
                    text = "Select an AI model to analyze text and extract information about people and relationships.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // Preset selector
                Text(
                    text = "Presets:",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Column(modifier = Modifier.padding(bottom = 16.dp)) {
                    presets.forEachIndexed { index, (name, preset) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedPresetIndex == index,
                                onClick = {
                                    selectedPresetIndex = index
                                    provider = preset.provider
                                    model = preset.model
                                    baseUrl = preset.baseUrl
                                    temperature = preset.temperature.toString()
                                    maxTokens = preset.maxTokens.toString()
                                }
                            )
                            Text(
                                text = name,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
                
                Divider(modifier = Modifier.padding(vertical = 16.dp))
                
                // API Keys for provider groups - copy blocked, paste only
                val clipboardManager = LocalClipboardManager.current
                
                // Show API key field depending on the selected provider
                when (provider) {
                    "OPENAI" -> {
                        DisableSelection {
                            OutlinedTextField(
                                value = openaiApiKey,
                                onValueChange = { openaiApiKey = it },
                                label = { Text("OpenAI API Key") },
                                placeholder = { Text("sk-...") },
                                supportingText = { Text("API key for OpenAI (GPT models). Get it from the OpenAI Dashboard.") },
                                visualTransformation = ApiKeyVisualTransformation(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                                    .onPreviewKeyEvent { keyEvent ->
                                        if (keyEvent.type == KeyEventType.KeyDown) {
                                            val isCopy = (keyEvent.isMetaPressed || keyEvent.isCtrlPressed) && keyEvent.key == Key.C
                                            if (isCopy) return@onPreviewKeyEvent true
                                            val isPaste = (keyEvent.isMetaPressed || keyEvent.isCtrlPressed) && keyEvent.key == Key.V
                                            if (isPaste) {
                                                clipboardManager.getText()?.text?.let { openaiApiKey = it }
                                                return@onPreviewKeyEvent true
                                            }
                                        }
                                        false
                                    },
                                singleLine = true
                            )
                        }
                    }

                    "GOOGLE" -> {
                        DisableSelection {
                            OutlinedTextField(
                                value = googleAiApiKey,
                                onValueChange = { googleAiApiKey = it },
                                label = { Text("Google AI API Key") },
                                placeholder = { Text("AIza...") },
                                supportingText = { Text("API key for Google AI (Gemini models and Speech-to-Text). Get it from Google AI Studio.") },
                                visualTransformation = ApiKeyVisualTransformation(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                                    .onPreviewKeyEvent { keyEvent ->
                                        if (keyEvent.type == KeyEventType.KeyDown) {
                                            val isCopy = (keyEvent.isMetaPressed || keyEvent.isCtrlPressed) && keyEvent.key == Key.C
                                            if (isCopy) return@onPreviewKeyEvent true
                                            val isPaste = (keyEvent.isMetaPressed || keyEvent.isCtrlPressed) && keyEvent.key == Key.V
                                            if (isPaste) {
                                                clipboardManager.getText()?.text?.let { googleAiApiKey = it }
                                                return@onPreviewKeyEvent true
                                            }
                                        }
                                        false
                                    },
                                singleLine = true
                            )
                        }
                    }
                    "YANDEX" -> {
                        DisableSelection {
                            OutlinedTextField(
                                value = yandexApiKey,
                                onValueChange = { yandexApiKey = it },
                                label = { Text("Yandex Cloud API Key") },
                                placeholder = { Text("AQVN...") },
                                supportingText = { Text("API key for Yandex Cloud. Get it from the Yandex Cloud Console.") },
                                visualTransformation = ApiKeyVisualTransformation(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                                    .onPreviewKeyEvent { keyEvent ->
                                        if (keyEvent.type == KeyEventType.KeyDown) {
                                            val isCopy = (keyEvent.isMetaPressed || keyEvent.isCtrlPressed) && keyEvent.key == Key.C
                                            if (isCopy) return@onPreviewKeyEvent true
                                            val isPaste = (keyEvent.isMetaPressed || keyEvent.isCtrlPressed) && keyEvent.key == Key.V
                                            if (isPaste) {
                                                clipboardManager.getText()?.text?.let { yandexApiKey = it }
                                                return@onPreviewKeyEvent true
                                            }
                                        }
                                        false
                                    },
                                singleLine = true
                            )
                        }
                        
                        OutlinedTextField(
                            value = yandexFolderId,
                            onValueChange = { yandexFolderId = it },
                            label = { Text("Folder ID (optional)") },
                            placeholder = { Text("default or b1g...") },
                            supportingText = { Text("Yandex Cloud Folder ID. Leave as 'default' for automatic detection when using a service account API key.") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            singleLine = true
                        )
                    }
                    "OLLAMA", "CUSTOM" -> {
                        // For Ollama and Custom, no API key is required or baseUrl is used
                        Text(
                            text = "API key is not required for local models",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }
                }
                
                // Custom URL (for Ollama and Custom)
                if (provider == "OLLAMA" || provider == "CUSTOM") {
                    OutlinedTextField(
                        value = baseUrl,
                        onValueChange = { baseUrl = it },
                        label = { Text("Base URL") },
                        placeholder = { 
                            Text(
                                when (provider) {
                                    "OLLAMA" -> "http://localhost:11434"
                                    else -> "https://your-api.com/v1"
                                }
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        singleLine = true
                    )
                }
                
                // Model
                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text("Model") },
                    placeholder = { Text("gpt-4o-mini") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    singleLine = true
                )
                
                // Language for voice transcription
                OutlinedTextField(
                    value = language,
                    onValueChange = { language = it },
                    label = { Text("Transcription Language (ISO-639-1)") },
                    placeholder = { Text("ka, ru, en, etc.") },
                    supportingText = { Text("Language code for transcription (e.g., 'ka' for Georgian, 'ru' for Russian). Leave empty for auto-detection.") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    singleLine = true
                )
                
                // Transcription provider selection
                Text(
                    text = "Speech Recognition Provider:",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                )
                
                Column(modifier = Modifier.padding(bottom = 12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = transcriptionProvider == "OPENAI_WHISPER",
                            onClick = { transcriptionProvider = "OPENAI_WHISPER" }
                        )
                        Text(
                            text = "OpenAI Whisper",
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = transcriptionProvider == "GOOGLE_SPEECH",
                            onClick = { transcriptionProvider = "GOOGLE_SPEECH" }
                        )
                        Text(
                            text = "Google Speech-to-Text (best for Georgian)",
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = transcriptionProvider == "YANDEX_SPEECHKIT",
                            onClick = { transcriptionProvider = "YANDEX_SPEECHKIT" }
                        )
                        Text(
                            text = "Yandex SpeechKit (best for Russian and CIS languages)",
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
                
                // OpenAI API Key (shown only when OpenAI Whisper is selected)
                if (transcriptionProvider == "OPENAI_WHISPER") {
                    DisableSelection {
                        OutlinedTextField(
                            value = openaiApiKey,
                            onValueChange = { openaiApiKey = it },
                            label = { Text("OpenAI API Key (Whisper)") },
                            placeholder = { Text("sk-...") },
                            supportingText = { Text("API key for OpenAI Whisper transcription. Uses the same key as for GPT models.") },
                            visualTransformation = ApiKeyVisualTransformation(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .onPreviewKeyEvent { keyEvent ->
                                    if (keyEvent.type == KeyEventType.KeyDown) {
                                        val isCopy = (keyEvent.isMetaPressed || keyEvent.isCtrlPressed) && 
                                                     keyEvent.key == Key.C
                                        if (isCopy) {
                                            return@onPreviewKeyEvent true
                                        }
                                        
                                        val isPaste = (keyEvent.isMetaPressed || keyEvent.isCtrlPressed) && 
                                                      keyEvent.key == Key.V
                                        if (isPaste) {
                                            clipboardManager.getText()?.text?.let { pastedText ->
                                                openaiApiKey = pastedText
                                            }
                                            return@onPreviewKeyEvent true
                                        }
                                    }
                                    false
                                },
                            singleLine = true
                        )
                    }
                }
                
                // Google AI API Key (shown only when Google Speech is selected)
                if (transcriptionProvider == "GOOGLE_SPEECH") {
                    DisableSelection {
                        OutlinedTextField(
                            value = googleAiApiKey,
                            onValueChange = { googleAiApiKey = it },
                            label = { Text("Google AI API Key (Speech-to-Text)") },
                            placeholder = { Text("AIza...") },
                            supportingText = { Text("API key for Google Speech-to-Text. Uses the same key as for Gemini models.") },
                            visualTransformation = ApiKeyVisualTransformation(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .onPreviewKeyEvent { keyEvent ->
                                    if (keyEvent.type == KeyEventType.KeyDown) {
                                        val isCopy = (keyEvent.isMetaPressed || keyEvent.isCtrlPressed) && 
                                                     keyEvent.key == Key.C
                                        if (isCopy) {
                                            return@onPreviewKeyEvent true
                                        }
                                        
                                        val isPaste = (keyEvent.isMetaPressed || keyEvent.isCtrlPressed) && 
                                                      keyEvent.key == Key.V
                                        if (isPaste) {
                                            clipboardManager.getText()?.text?.let { pastedText ->
                                                googleAiApiKey = pastedText
                                            }
                                            return@onPreviewKeyEvent true
                                        }
                                    }
                                    false
                                },
                            singleLine = true
                        )
                    }
                }
                
                // Yandex API Key (shown only when Yandex SpeechKit is selected)
                if (transcriptionProvider == "YANDEX_SPEECHKIT") {
                    DisableSelection {
                        OutlinedTextField(
                            value = yandexApiKey,
                            onValueChange = { yandexApiKey = it },
                            label = { Text("Yandex Cloud API Key (SpeechKit)") },
                            placeholder = { Text("AQVN...") },
                            supportingText = { Text("API key for Yandex SpeechKit. Get it from the Yandex Cloud Console.") },
                            visualTransformation = ApiKeyVisualTransformation(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .onPreviewKeyEvent { keyEvent ->
                                    if (keyEvent.type == KeyEventType.KeyDown) {
                                        val isCopy = (keyEvent.isMetaPressed || keyEvent.isCtrlPressed) && 
                                                     keyEvent.key == Key.C
                                        if (isCopy) {
                                            return@onPreviewKeyEvent true
                                        }
                                        
                                        val isPaste = (keyEvent.isMetaPressed || keyEvent.isCtrlPressed) && 
                                                      keyEvent.key == Key.V
                                        if (isPaste) {
                                            clipboardManager.getText()?.text?.let { pastedText ->
                                                yandexApiKey = pastedText
                                            }
                                            return@onPreviewKeyEvent true
                                        }
                                    }
                                    false
                                },
                            singleLine = true
                        )
                    }
                }
                
                // Advanced settings
                Text(
                    text = "Advanced Settings:",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = temperature,
                        onValueChange = { temperature = it },
                        label = { Text("Temperature") },
                        placeholder = { Text("0.7") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    
                    OutlinedTextField(
                        value = maxTokens,
                        onValueChange = { maxTokens = it },
                        label = { Text("Max Tokens") },
                        placeholder = { Text("4000") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                
                // Help text
                Text(
                    text = "OpenAI requires an API key. For Ollama, ensure the server is running (ollama serve).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 16.dp, bottom = 16.dp)
                )
                
                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val config = AiConfig(
                                provider = provider,
                                apiKey = apiKey,  // Deprecated, but kept for backward compatibility
                                model = model,
                                baseUrl = baseUrl,
                                temperature = temperature.toDoubleOrNull() ?: 0.7,
                                maxTokens = maxTokens.toIntOrNull() ?: 4000,
                                language = language,
                                transcriptionProvider = transcriptionProvider,
                                googleApiKey = googleApiKey,  // Deprecated
                                
                                // New fields for separate provider group keys
                                openaiApiKey = openaiApiKey,

                                googleAiApiKey = googleAiApiKey,
                                yandexApiKey = yandexApiKey,
                                yandexFolderId = yandexFolderId
                            )
                            onConfirm(config)
                        }
                    ) {
                        Text("Continue")
                    }
                }
            }
        }
    }
}
