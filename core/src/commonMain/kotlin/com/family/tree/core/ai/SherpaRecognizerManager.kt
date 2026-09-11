package com.family.tree.core.ai

/**
 * Multiplatform wrapper for Sherpa-ONNX Recognizer and Model downloading.
 */
expect class SherpaRecognizerManager() {
    
    /**
     * Downloads the Sherpa-ONNX model for the specified language.
     * Returns the absolute path to the extracted model directory.
     */
    suspend fun downloadModel(language: String, onProgress: (Float) -> Unit): String
    
    /**
     * Checks if the model for the given language is already downloaded.
     */
    fun isModelDownloaded(language: String): Boolean

    /**
     * Deletes the downloaded model for the given language.
     */
    fun deleteModel(language: String)
    
    /**
     * Transcribes the given audio data.
     */
    suspend fun transcribeAudio(audioData: ByteArray, language: String): String
}
