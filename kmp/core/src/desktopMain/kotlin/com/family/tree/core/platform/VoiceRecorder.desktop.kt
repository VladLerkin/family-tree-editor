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
        // Проверяем доступность микрофона на Desktop платформе
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
        onResult: (ByteArray) -> Unit,
        onError: (String) -> Unit
    ) {
        if (recording) {
            onError("Запись уже идет")
            return
        }
        
        resultCallback = onResult
        errorCallback = onError
        recording = true
        
        try {
            // Настраиваем аудио формат (аналогично Android: 16kHz, mono, 16-bit)
            val audioFormat = AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                16000f,  // Sample rate
                16,      // Sample size in bits
                1,       // Channels (mono)
                2,       // Frame size
                16000f,  // Frame rate
                false    // Little endian
            )
            
            // Получаем линию для записи
            val dataLineInfo = DataLine.Info(TargetDataLine::class.java, audioFormat)
            
            if (!AudioSystem.isLineSupported(dataLineInfo)) {
                recording = false
                val errorMsg = "Микрофон не поддерживается на этой системе"
                println("[DEBUG_LOG] VoiceRecorder (Desktop): $errorMsg")
                errorCallback?.invoke(errorMsg)
                return
            }
            
            targetDataLine = AudioSystem.getLine(dataLineInfo) as TargetDataLine
            targetDataLine?.open(audioFormat)
            targetDataLine?.start()
            
            audioOutputStream = ByteArrayOutputStream()
            
            println("[DEBUG_LOG] VoiceRecorder (Desktop): Recording started")
            
            // Запускаем запись в отдельном потоке
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
            val errorMsg = "Микрофон недоступен: ${e.message}"
            println("[DEBUG_LOG] VoiceRecorder (Desktop): $errorMsg")
            e.printStackTrace()
            errorCallback?.invoke(errorMsg)
            cleanup()
        } catch (e: Exception) {
            recording = false
            val errorMsg = "Ошибка запуска записи: ${e.message}"
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
            
            // Останавливаем поток записи
            recordingThread?.interrupt()
            recordingThread?.join(1000)
            
            // Останавливаем линию
            targetDataLine?.stop()
            targetDataLine?.close()
            
            // Получаем записанные данные
            val audioData = audioOutputStream?.toByteArray()
            if (audioData != null && audioData.isNotEmpty()) {
                println("[DEBUG_LOG] VoiceRecorder (Desktop): Read ${audioData.size} bytes from audio recording")
                
                // Конвертируем PCM в WAV формат для совместимости
                val wavData = convertPcmToWav(audioData)
                resultCallback?.invoke(wavData)
            } else {
                println("[DEBUG_LOG] VoiceRecorder (Desktop): No audio data recorded")
                errorCallback?.invoke("Аудио данные не записаны")
            }
            
            cleanup()
        } catch (e: Exception) {
            recording = false
            val errorMsg = "Ошибка остановки записи: ${e.message}"
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
            
            // Останавливаем поток записи
            recordingThread?.interrupt()
            recordingThread?.join(1000)
            
            // Останавливаем линию
            targetDataLine?.stop()
            targetDataLine?.close()
            
            // Очищаем колбэки, чтобы они не вызывались
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
        // No-op на Desktop платформе
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
     * Конвертирует сырые PCM данные в WAV формат с заголовком
     */
    private fun convertPcmToWav(pcmData: ByteArray): ByteArray {
        val outputStream = ByteArrayOutputStream()
        
        // WAV заголовок
        val audioFormat = AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            16000f,  // Sample rate
            16,      // Sample size in bits
            1,       // Channels (mono)
            2,       // Frame size
            16000f,  // Frame rate
            false    // Little endian
        )
        
        val audioInputStream = AudioInputStream(
            pcmData.inputStream(),
            audioFormat,
            pcmData.size.toLong() / audioFormat.frameSize
        )
        
        AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, outputStream)
        
        return outputStream.toByteArray()
    }
}
