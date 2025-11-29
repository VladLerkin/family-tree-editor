package com.family.tree.core.ai

import com.family.tree.core.io.LoadedProject
import com.family.tree.core.platform.VoiceRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Обработчик голосового ввода для импорта родственников
 * Связывает VoiceRecorder (запись аудио) → AiClient (транскрипция через Whisper) → AiTextImporter (обработка через LLM)
 */
class VoiceInputProcessor(
    private val voiceRecorder: VoiceRecorder,
    private val coroutineScope: CoroutineScope
) {
    private val settingsStorage = AiSettingsStorage()
    
    /**
     * Проверка доступности голосового ввода
     */
    fun isVoiceInputAvailable(): Boolean {
        return voiceRecorder.isAvailable()
    }
    
    /**
     * Проверка, идет ли запись
     */
    fun isRecording(): Boolean {
        return voiceRecorder.isRecording()
    }
    
    /**
     * Начать запись голоса и обработку через AI
     * @param onSuccess callback с загруженным проектом при успехе
     * @param onError callback с сообщением об ошибке
     * @param onRecognized callback с распознанным текстом (опционально, для отладки/UI)
     */
    fun startVoiceInput(
        onSuccess: (LoadedProject) -> Unit,
        onError: (String) -> Unit,
        onRecognized: ((String) -> Unit)? = null
    ) {
        if (!isVoiceInputAvailable()) {
            onError("Голосовой ввод недоступен на этой платформе")
            return
        }
        
        if (isRecording()) {
            onError("Запись уже идет")
            return
        }
        
        println("[DEBUG_LOG] VoiceInputProcessor: Starting voice input")
        
        // Load config to determine provider
        val aiConfig = settingsStorage.loadConfig()
        val provider = aiConfig.getTranscriptionProvider()
        
        // Select format based on provider
        val audioFormat = when (provider) {
            TranscriptionProvider.YANDEX_SPEECHKIT -> com.family.tree.core.platform.AudioFormat.WAV
            else -> com.family.tree.core.platform.AudioFormat.M4A
        }
        
        println("[DEBUG_LOG] VoiceInputProcessor: Using audio format $audioFormat for provider $provider")
        
        voiceRecorder.startRecording(
            format = audioFormat,
            onResult = { audioData ->
                println("[DEBUG_LOG] VoiceInputProcessor: Received audio data: ${audioData.size} bytes")
                
                // Обработать аудио через AI в корутине
                coroutineScope.launch {
                    try {
                        // Загружаем актуальную конфигурацию AI из настроек
                        val aiConfig = settingsStorage.loadConfig()
                        println("[DEBUG_LOG] VoiceInputProcessor: Loaded AI config - provider=${aiConfig.getProvider()}, model=${aiConfig.model}, apiKey=${if (aiConfig.apiKey.isBlank()) "empty" else "present"}")
                        
                        // Создаем клиенты с актуальной конфигурацией
                        val transcriptionClient = TranscriptionClientFactory.createClient(aiConfig)
                        val aiTextImporter = AiTextImporter(aiConfig)
                        
                        // Шаг 1: Транскрибировать аудио через выбранный провайдер (Whisper, Google Speech или Yandex SpeechKit)
                        val providerName = when (aiConfig.getTranscriptionProvider()) {
                            TranscriptionProvider.OPENAI_WHISPER -> "OpenAI Whisper"
                            TranscriptionProvider.GOOGLE_SPEECH -> "Google Speech-to-Text"
                            TranscriptionProvider.YANDEX_SPEECHKIT -> "Yandex SpeechKit"
                        }
                        println("[DEBUG_LOG] VoiceInputProcessor: Transcribing audio through $providerName")
                        val transcribedText = transcriptionClient.transcribeAudio(audioData, aiConfig)
                        println("[DEBUG_LOG] VoiceInputProcessor: Transcribed text: $transcribedText")
                        onRecognized?.invoke(transcribedText)
                        
                        // Шаг 2: Обработать транскрибированный текст через AI для извлечения структурированных данных
                        println("[DEBUG_LOG] VoiceInputProcessor: Processing transcribed text through AI")
                        val loadedProject = aiTextImporter.importFromText(transcribedText)
                        println("[DEBUG_LOG] VoiceInputProcessor: Successfully imported ${loadedProject.data.individuals.size} individuals")
                        onSuccess(loadedProject)
                    } catch (e: Exception) {
                        val errorMsg = "Ошибка обработки аудио: ${e.message}"
                        println("[DEBUG_LOG] VoiceInputProcessor: $errorMsg")
                        e.printStackTrace()
                        onError(errorMsg)
                    }
                }
            },
            onError = { errorMessage ->
                println("[DEBUG_LOG] VoiceInputProcessor: Voice recording error: $errorMessage")
                onError("Ошибка записи аудио: $errorMessage")
            }
        )
    }
    
    /**
     * Остановить запись и обработать результат
     */
    fun stopRecording() {
        if (isRecording()) {
            println("[DEBUG_LOG] VoiceInputProcessor: Stopping recording")
            voiceRecorder.stopRecording()
        }
    }
    
    /**
     * Отменить запись без обработки
     */
    fun cancelRecording() {
        if (isRecording()) {
            println("[DEBUG_LOG] VoiceInputProcessor: Cancelling recording")
            voiceRecorder.cancelRecording()
        }
    }
}
