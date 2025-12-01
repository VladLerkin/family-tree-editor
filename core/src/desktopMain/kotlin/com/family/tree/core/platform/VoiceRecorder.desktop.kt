package com.family.tree.core.platform

import java.io.ByteArrayOutputStream
import java.io.File
import javax.sound.sampled.*

actual class VoiceRecorder actual constructor(context: Any?) {
    
    private var recording = false
    private var recordingThread: Thread? = null
    private var targetDataLine: TargetDataLine? = null
    private var audioOutputStream: ByteArrayOutputStream? = null
    private var resultCallback: ((ByteArray) -> Unit)? = null
    private var errorCallback: ((String) -> Unit)? = null
    
    actual fun isAvailable(): Boolean {
        // Check microphone availability on Desktop platform
        return try {
            val mixerInfos = AudioSystem.getMixerInfo()
            mixerInfos.any { info ->
                val mixer = AudioSystem.getMixer(info)
                mixer.targetLineInfo.isNotEmpty()
            }
        } catch (e: Exception) {
            println("[DEBUG_LOG] VoiceRecorder (Desktop): Error checking audio availability: ${e.message}")
            false
        }
    }
    
    actual fun startRecording(
        format: AudioFormat,
        onResult: (ByteArray) -> Unit,
        onError: (String) -> Unit
    ) {
        if (recording) {
            onError("Recording is already in progress")
            return
        }
        
        resultCallback = onResult
        errorCallback = onError
        recording = true
        
        try {
            // Configure audio format (similar to Android: 16kHz, mono, 16-bit)
            val javaxAudioFormat = javax.sound.sampled.AudioFormat(
                javax.sound.sampled.AudioFormat.Encoding.PCM_SIGNED,
                16000f,  // Sample rate
                16,      // Sample size in bits
                1,       // Channels (mono)
                2,       // Frame size
                16000f,  // Frame rate
                false    // Little endian
            )
            
            // Get line for recording
            val dataLineInfo = DataLine.Info(TargetDataLine::class.java, javaxAudioFormat)
            
            if (!AudioSystem.isLineSupported(dataLineInfo)) {
                recording = false
                val errorMsg = "Microphone is not supported on this system"
                println("[DEBUG_LOG] VoiceRecorder (Desktop): $errorMsg")
                errorCallback?.invoke(errorMsg)
                return
            }
            
            targetDataLine = AudioSystem.getLine(dataLineInfo) as TargetDataLine
            targetDataLine?.open(javaxAudioFormat)
            targetDataLine?.start()
            
            audioOutputStream = ByteArrayOutputStream()
            
            println("[DEBUG_LOG] VoiceRecorder (Desktop): Recording started")
            
            // Start recording in a separate thread
            recordingThread = Thread {
                try {
                    val buffer = ByteArray(4096)
                    while (recording && !Thread.currentThread().isInterrupted) {
                        val bytesRead = targetDataLine?.read(buffer, 0, buffer.size) ?: -1
                        if (bytesRead > 0) {
                            audioOutputStream?.write(buffer, 0, bytesRead)
                        }
                    }
                } catch (e: Exception) {
                    if (recording) {
                        println("[DEBUG_LOG] VoiceRecorder (Desktop): Recording error: ${e.message}")
                        e.printStackTrace()
                    }
                }
            }.apply { 
                isDaemon = true
                start() 
            }
            
        } catch (e: LineUnavailableException) {
            recording = false
            val errorMsg = "Microphone is not available: ${e.message}"
            println("[DEBUG_LOG] VoiceRecorder (Desktop): $errorMsg")
            e.printStackTrace()
            errorCallback?.invoke(errorMsg)
            cleanup()
        } catch (e: Exception) {
            recording = false
            val errorMsg = "Error starting recording: ${e.message}"
            println("[DEBUG_LOG] VoiceRecorder (Desktop): $errorMsg")
            e.printStackTrace()
            errorCallback?.invoke(errorMsg)
            cleanup()
        }
    }
    
    actual fun stopRecording() {
        if (!recording) {
            println("[DEBUG_LOG] VoiceRecorder (Desktop): Not recording, ignoring stop")
            return
        }
        
        try {
            println("[DEBUG_LOG] VoiceRecorder (Desktop): Stopping recording")
            recording = false
            
            // Stop recording thread
            recordingThread?.interrupt()
            recordingThread?.join(1000)
            
            // Stop line
            targetDataLine?.stop()
            targetDataLine?.close()
            
            // Get recorded data
            val audioData = audioOutputStream?.toByteArray()
            if (audioData != null && audioData.isNotEmpty()) {
                println("[DEBUG_LOG] VoiceRecorder (Desktop): Read ${audioData.size} bytes from audio recording")
                
                // Convert PCM to WAV format for compatibility
                val wavData = convertPcmToWav(audioData)
                resultCallback?.invoke(wavData)
            } else {
                println("[DEBUG_LOG] VoiceRecorder (Desktop): No audio data recorded")
                errorCallback?.invoke("Audio data was not recorded")
            }
            
            cleanup()
        } catch (e: Exception) {
            recording = false
            val errorMsg = "Error stopping recording: ${e.message}"
            println("[DEBUG_LOG] VoiceRecorder (Desktop): $errorMsg")
            e.printStackTrace()
            errorCallback?.invoke(errorMsg)
            cleanup()
        }
    }
    
    actual fun cancelRecording() {
        if (!recording) {
            println("[DEBUG_LOG] VoiceRecorder (Desktop): Not recording, ignoring cancel")
            return
        }
        
        try {
            println("[DEBUG_LOG] VoiceRecorder (Desktop): Cancelling recording (no callback)")
            recording = false
            
            // Stop recording thread
            recordingThread?.interrupt()
            recordingThread?.join(1000)
            
            // Stop line
            targetDataLine?.stop()
            targetDataLine?.close()
            
            // Clear callbacks to avoid invocation
            resultCallback = null
            errorCallback = null
            
            cleanup()
        } catch (e: Exception) {
            recording = false
            println("[DEBUG_LOG] VoiceRecorder (Desktop): Error cancelling recording: ${e.message}")
            e.printStackTrace()
            cleanup()
        }
    }
    
    actual fun isRecording(): Boolean {
        return recording
    }
    
    actual fun openAppSettings() {
        // No-op on Desktop platform
        println("[DEBUG_LOG] VoiceRecorder (Desktop): openAppSettings called, but not supported on Desktop")
    }
    
    private fun cleanup() {
        try {
            targetDataLine?.close()
            targetDataLine = null
            audioOutputStream?.close()
            audioOutputStream = null
            recordingThread = null
        } catch (e: Exception) {
            println("[DEBUG_LOG] VoiceRecorder (Desktop): Error during cleanup: ${e.message}")
        }
    }
    
    /**
     * Converts raw PCM data to WAV format with header
     */
    private fun convertPcmToWav(pcmData: ByteArray): ByteArray {
        val outputStream = ByteArrayOutputStream()
        
        // WAV header
        val javaxAudioFormat = javax.sound.sampled.AudioFormat(
            javax.sound.sampled.AudioFormat.Encoding.PCM_SIGNED,
            16000f,  // Sample rate
            16,      // Sample size in bits
            1,       // Channels (mono)
            2,       // Frame size
            16000f,  // Frame rate
            false    // Little endian
        )
        
        val audioInputStream = AudioInputStream(
            pcmData.inputStream(),
            javaxAudioFormat,
            pcmData.size.toLong() / javaxAudioFormat.frameSize
        )
        
        AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, outputStream)
        
        return outputStream.toByteArray()
    }
}
