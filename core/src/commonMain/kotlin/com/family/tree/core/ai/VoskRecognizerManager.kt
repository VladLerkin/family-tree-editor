package com.family.tree.core.ai

/**
 * Multiplatform wrapper for Vosk Recognizer and Model downloading.
 */
expect class VoskRecognizerManager() {
    
    /**
     * Downloads the Vosk model for the specified language.
     * Returns the absolute path to the extracted model directory.
     */
    suspend fun downloadModel(language: String, onProgress: (Float) -> Unit): String
    
    /**
     * Checks if the model for the given language is already downloaded.
     */
    fun isModelDownloaded(language: String): Boolean
    
    /**
     * Transcribes the given audio data.
     */
    suspend fun transcribeAudio(audioData: ByteArray, language: String): String
}
