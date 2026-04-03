package com.family.tree.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.family.tree.core.ai.AiConfig
import com.family.tree.core.ai.AiPresets
import com.family.tree.core.ai.AiSettingsStorage

class AiSettingsScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val storage = remember { AiSettingsStorage() }
        val initialConfig = remember { storage.loadConfig() }
        
        val presets = AiPresets.getAllPresets()
        val initialPresetIndex = presets.indexOfFirst { (_, preset) -> 
            preset.model == initialConfig.model && preset.provider == initialConfig.provider
        }.let { if (it >= 0) it else 0 }
        
        var selectedPresetIndex by remember { mutableStateOf(initialPresetIndex) }
        var provider by remember { mutableStateOf(initialConfig.provider) }
        @Suppress("DEPRECATION")
        var apiKey by remember { mutableStateOf(initialConfig.apiKey) }
        var model by remember { mutableStateOf(initialConfig.model) }
        var baseUrl by remember { mutableStateOf(initialConfig.baseUrl) }
        var temperature by remember { mutableStateOf(initialConfig.temperature.toString()) }
        var maxTokens by remember { mutableStateOf(initialConfig.maxTokens.toString()) }
        var language by remember { mutableStateOf(initialConfig.language) }
        var transcriptionProvider by remember { mutableStateOf(initialConfig.transcriptionProvider) }
        @Suppress("DEPRECATION")
        var googleApiKey by remember { mutableStateOf(initialConfig.googleApiKey) }
        
        var openAiKey by remember { mutableStateOf(initialConfig.openaiApiKey) }
        var googleKey by remember { mutableStateOf(initialConfig.googleAiApiKey) }
        var yandexKey by remember { mutableStateOf(initialConfig.yandexApiKey) }
        var yandexFolderId by remember { mutableStateOf(initialConfig.yandexFolderId) }
        
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("AI Settings") },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            @Suppress("DEPRECATION")
                            val config = AiConfig(
                                provider = provider,
                                apiKey = apiKey,
                                model = model,
                                baseUrl = baseUrl,
                                temperature = temperature.toDoubleOrNull() ?: 0.7,
                                maxTokens = maxTokens.toIntOrNull() ?: 4000,
                                language = language,
                                transcriptionProvider = transcriptionProvider,
                                googleApiKey = googleApiKey,
                                openaiApiKey = openAiKey,
                                googleAiApiKey = googleKey,
                                yandexApiKey = yandexKey,
                                yandexFolderId = yandexFolderId
                            )
                            storage.saveConfig(config)
                            navigator.pop()
                        }) {
                            Icon(Icons.Default.Check, contentDescription = "Save")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.TopCenter) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Select an AI model to analyze text and extract information about people and relationships.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
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
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                    
                    when (provider) {
                        "OPENAI" -> {
                            ApiKeyTextField(
                                value = openAiKey,
                                onValueChange = { openAiKey = it },
                                label = "OpenAI API Key",
                                placeholder = "sk-...",
                                supportingText = "Provided key is stored in memory and masked in logs.",
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        "GOOGLE" -> {
                            ApiKeyTextField(
                                value = googleKey,
                                onValueChange = { googleKey = it },
                                label = "Google AI API Key",
                                placeholder = "AIza...",
                                supportingText = "Provided key is stored in memory and masked in logs.",
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        "YANDEX" -> {
                            ApiKeyTextField(
                                value = yandexKey,
                                onValueChange = { yandexKey = it },
                                label = "YandexGPT API Key",
                                placeholder = "AQVN...",
                                supportingText = "Provided key is stored in memory and masked in logs.",
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = yandexFolderId,
                                onValueChange = { yandexFolderId = it },
                                label = { Text("Folder ID (optional)") },
                                placeholder = { Text("default or b1g...") },
                                supportingText = { Text("Yandex Cloud Folder ID. Leave as 'default' for automatic detection.") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                singleLine = true
                            )
                        }
                        "OLLAMA", "CUSTOM" -> {
                            Text(
                                text = "API key is not required for local models",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                        }
                    }
                    
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
                    
                    OutlinedTextField(
                        value = language,
                        onValueChange = { language = it },
                        label = { Text("Transcription Language (ISO-639-1)") },
                        placeholder = { Text("ka, ru, en, etc.") },
                        supportingText = { Text("Language code for transcription. Leave empty for auto-detection.") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        singleLine = true
                    )
                    
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
                            Text("OpenAI Whisper", modifier = Modifier.padding(start = 8.dp))
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
                            Text("Google Speech-to-Text", modifier = Modifier.padding(start = 8.dp))
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
                            Text("Yandex SpeechKit", modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                    
                    if (transcriptionProvider == "OPENAI_WHISPER") {
                        ApiKeyTextField(
                            value = openAiKey,
                            onValueChange = { openAiKey = it },
                            label = "OpenAI API Key (Whisper)",
                            placeholder = "sk-...",
                            supportingText = "API key for OpenAI Whisper transcription.",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (transcriptionProvider == "GOOGLE_SPEECH") {
                        ApiKeyTextField(
                            value = googleKey,
                            onValueChange = { googleKey = it },
                            label = "Google AI API Key (Speech-to-Text)",
                            placeholder = "AIza...",
                            supportingText = "API key for Google Speech-to-Text.",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (transcriptionProvider == "YANDEX_SPEECHKIT") {
                        ApiKeyTextField(
                            value = yandexKey,
                            onValueChange = { yandexKey = it },
                            label = "Yandex Cloud API Key (SpeechKit)",
                            placeholder = "AQVN...",
                            supportingText = "API key for Yandex SpeechKit.",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    Text(
                        text = "Advanced Settings:",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 48.dp),
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
                }
            }
        }
    }
}
