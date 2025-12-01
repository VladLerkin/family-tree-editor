package com.family.tree.core.ai

import com.family.tree.core.io.LoadedProject
import com.family.tree.core.platform.VoiceRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Voice input processor for importing relatives
 * Connects VoiceRecorder (audio recording) -> AiClient (transcription via Whisper) -> AiTextImporter (processing via LLM)
 */
class VoiceInputProcessor(
    private val voiceRecorder: VoiceRecorder,
    private val coroutineScope: CoroutineScope
) {
    private val settingsStorage = AiSettingsStorage()
    
    /**
     * Check if voice input is available
     */
    fun isVoiceInputAvailable(): Boolean {
        return voiceRecorder.isAvailable()
    }
    
    /**
     * Check if recording is in progress
     */
    fun isRecording(): Boolean {
        return voiceRecorder.isRecording()
    }
    
    /**
     * Start voice recording and AI processing
     * @param onSuccess callback with loaded project on success
     * @param onError callback with error message
     * @param onRecognized callback with recognized text (optional, for debug/UI)
     */
    fun startVoiceInput(
        onSuccess: (LoadedProject) -> Unit,
        onError: (String) -> Unit,
        onRecognized: ((String) -> Unit)? = null
    ) {
        if (!isVoiceInputAvailable()) {
            onError("Voice input is not available on this platform")
            return
        }
        
        if (isRecording()) {
            onError("Recording is already in progress")
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
                
                // Process audio via AI in coroutine
                coroutineScope.launch {
                    try {
                        // Load actual AI config from settings
                        val aiConfig = settingsStorage.loadConfig()
                        println("[DEBUG_LOG] VoiceInputProcessor: Loaded AI config - provider=${aiConfig.getProvider()}, model=${aiConfig.model}, apiKey=${if (aiConfig.apiKey.isBlank()) "empty" else "present"}")
                        
                        // Create clients with actual config
                        val transcriptionClient = TranscriptionClientFactory.createClient(aiConfig)
                        val aiTextImporter = AiTextImporter(aiConfig)
                        
                        // Step 1: Transcribe audio via selected provider (Whisper, Google Speech or Yandex SpeechKit)
                        val providerName = when (aiConfig.getTranscriptionProvider()) {
                            TranscriptionProvider.OPENAI_WHISPER -> "OpenAI Whisper"
                            TranscriptionProvider.GOOGLE_SPEECH -> "Google Speech-to-Text"
                            TranscriptionProvider.YANDEX_SPEECHKIT -> "Yandex SpeechKit"
                        }
                        println("[DEBUG_LOG] VoiceInputProcessor: Transcribing audio through $providerName")
                        val transcribedText = transcriptionClient.transcribeAudio(audioData, aiConfig)
                        println("[DEBUG_LOG] VoiceInputProcessor: Transcribed text: $transcribedText")
                        onRecognized?.invoke(transcribedText)
                        
                        // Step 2: Process transcribed text via AI to extract structured data
                        println("[DEBUG_LOG] VoiceInputProcessor: Processing transcribed text through AI")
                        val loadedProject = aiTextImporter.importFromText(transcribedText)
                        println("[DEBUG_LOG] VoiceInputProcessor: Successfully imported ${loadedProject.data.individuals.size} individuals")
                        onSuccess(loadedProject)
                    } catch (e: Exception) {
                        val errorMsg = "Audio processing error: ${e.message}"
                        println("[DEBUG_LOG] VoiceInputProcessor: $errorMsg")
                        e.printStackTrace()
                        onError(errorMsg)
                    }
                }
            },
            onError = { errorMessage ->
                println("[DEBUG_LOG] VoiceInputProcessor: Voice recording error: $errorMessage")
                onError("Audio recording error: $errorMessage")
            }
        )
    }
    
    /**
     * Stop recording and process result
     */
    fun stopRecording() {
        if (isRecording()) {
            println("[DEBUG_LOG] VoiceInputProcessor: Stopping recording")
            voiceRecorder.stopRecording()
        }
    }
    
    /**
     * Cancel recording without processing
     */
    fun cancelRecording() {
        if (isRecording()) {
            println("[DEBUG_LOG] VoiceInputProcessor: Cancelling recording")
            voiceRecorder.cancelRecording()
        }
    }
}
