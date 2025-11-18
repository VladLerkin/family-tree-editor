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
 * Визуальная трансформация для маскирования API ключа.
 * Показывает первые 4 символа, затем звездочки, затем последние 4 символа.
 */
class ApiKeyVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val original = text.text
        
        // Если ключ короткий или пустой, показываем как есть
        if (original.length <= 12) {
            return TransformedText(text, OffsetMapping.Identity)
        }
        
        // Показываем первые 4 символа, 6 звездочек, последние 4 символа
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
                        else -> 4 + 3 // середина звездочек
                    }
                }
                
                override fun transformedToOriginal(offset: Int): Int {
                    return when {
                        offset <= 4 -> offset
                        offset >= masked.length - 4 -> offset - masked.length + original.length
                        else -> 4 + (original.length - 8) / 2 // середина оригинального текста
                    }
                }
            }
        )
    }
}

/**
 * Диалог для настройки параметров AI перед импортом текста.
 */
@Composable
fun AiConfigDialog(
    initialConfig: AiConfig = AiPresets.OPENAI_GPT4O_MINI,
    onDismiss: () -> Unit,
    onConfirm: (AiConfig) -> Unit
) {
    var selectedPresetIndex by remember { mutableStateOf(0) }
    var provider by remember { mutableStateOf(initialConfig.provider) }
    var apiKey by remember { mutableStateOf(initialConfig.apiKey) }
    var model by remember { mutableStateOf(initialConfig.model) }
    var baseUrl by remember { mutableStateOf(initialConfig.baseUrl) }
    var temperature by remember { mutableStateOf(initialConfig.temperature.toString()) }
    var maxTokens by remember { mutableStateOf(initialConfig.maxTokens.toString()) }
    
    val presets = AiPresets.getAllPresets()
    
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
                    text = "Настройка AI для импорта текста",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Text(
                    text = "Выберите AI модель для анализа текста и извлечения информации о персонах и родственных связях.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // Preset selector
                Text(
                    text = "Предустановки:",
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
                
                // API Key - блокировка копирования, только вставка
                val clipboardManager = LocalClipboardManager.current
                DisableSelection {
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        label = { Text("API Key") },
                        placeholder = { 
                            Text(
                                when (provider) {
                                    "OPENAI" -> "sk-..."
                                    "ANTHROPIC" -> "sk-ant-..."
                                    "OLLAMA" -> "Не требуется для локальных моделей"
                                    else -> "Опционально"
                                }
                            )
                        },
                        visualTransformation = ApiKeyVisualTransformation(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .onPreviewKeyEvent { keyEvent ->
                                // Блокируем копирование (Cmd+C на macOS, Ctrl+C на Windows/Linux)
                                if (keyEvent.type == KeyEventType.KeyDown) {
                                    val isCopy = (keyEvent.isMetaPressed || keyEvent.isCtrlPressed) && 
                                                 keyEvent.key == Key.C
                                    if (isCopy) {
                                        return@onPreviewKeyEvent true // Блокируем событие
                                    }
                                    
                                    // Разрешаем вставку (Cmd+V / Ctrl+V)
                                    val isPaste = (keyEvent.isMetaPressed || keyEvent.isCtrlPressed) && 
                                                  keyEvent.key == Key.V
                                    if (isPaste) {
                                        clipboardManager.getText()?.text?.let { pastedText ->
                                            apiKey = pastedText
                                        }
                                        return@onPreviewKeyEvent true
                                    }
                                }
                                false
                            },
                        singleLine = true
                    )
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
                    label = { Text("Модель") },
                    placeholder = { Text("gpt-4o-mini") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    singleLine = true
                )
                
                // Advanced settings
                Text(
                    text = "Расширенные настройки:",
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
                    text = "Для OpenAI и Anthropic требуется API ключ. Для Ollama убедитесь, что сервер запущен (ollama serve).",
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
                        Text("Отмена")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val config = AiConfig(
                                provider = provider,
                                apiKey = apiKey,
                                model = model,
                                baseUrl = baseUrl,
                                temperature = temperature.toDoubleOrNull() ?: 0.7,
                                maxTokens = maxTokens.toIntOrNull() ?: 4000
                            )
                            onConfirm(config)
                        }
                    ) {
                        Text("Продолжить")
                    }
                }
            }
        }
    }
}
