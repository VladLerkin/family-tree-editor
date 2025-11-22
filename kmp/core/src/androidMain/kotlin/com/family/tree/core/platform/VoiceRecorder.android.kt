package com.family.tree.core.platform

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.provider.Settings
import java.io.File
import java.io.IOException

/**
 * Параметры формата записи для MediaRecorder
 */
private data class RecordingFormat(
    val fileExtension: String,
    val outputFormat: Int,
    val audioEncoder: Int,
    val description: String
)

actual class VoiceRecorder actual constructor(context: Any?) {
    
    private val androidContext: Context? = context as? Context
    private var mediaRecorder: MediaRecorder? = null
    private var recording = false
    private var audioFile: File? = null
    private var resultCallback: ((ByteArray) -> Unit)? = null
    private var errorCallback: ((String) -> Unit)? = null
    
    actual fun isAvailable(): Boolean {
        return androidContext != null
    }
    
    actual fun startRecording(
        format: AudioFormat,
        onResult: (ByteArray) -> Unit,
        onError: (String) -> Unit
    ) {
        if (androidContext == null) {
            onError("Android Context не предоставлен")
            return
        }
        
        println("[DEBUG_LOG] VoiceRecorder: Device info - Manufacturer: ${Build.MANUFACTURER}, Brand: ${Build.BRAND}, Model: ${Build.MODEL}")
        
        // Проверяем разрешение RECORD_AUDIO перед запуском
        val permissionCheck = androidContext.checkSelfPermission(Manifest.permission.RECORD_AUDIO)
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            println("[DEBUG_LOG] VoiceRecorder: RECORD_AUDIO permission not granted")
            onError(buildPermissionErrorMessage())
            return
        }
        println("[DEBUG_LOG] VoiceRecorder: RECORD_AUDIO permission granted")
        
        if (recording) {
            onError("Запись уже идет")
            return
        }
        
        resultCallback = onResult
        errorCallback = onError
        recording = true
        
        try {
            // Выбираем формат записи в зависимости от провайдера транскрипции
            val recordingFormat = when (format) {
                AudioFormat.M4A -> {
                    // M4A/AAC формат - оптимален для OpenAI Whisper API
                    RecordingFormat(
                        fileExtension = ".m4a",
                        outputFormat = MediaRecorder.OutputFormat.MPEG_4,
                        audioEncoder = MediaRecorder.AudioEncoder.AAC,
                        description = "AAC/M4A format, 16kHz, 64kbps"
                    )
                }
                AudioFormat.FLAC -> {
                    // FLAC формат - оптимален для Google Speech-to-Text API
                    // Требует Android API 26+
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        RecordingFormat(
                            fileExtension = ".flac",
                            outputFormat = MediaRecorder.OutputFormat.OGG,  // OGG container для FLAC
                            audioEncoder = 17,  // MediaRecorder.AudioEncoder.FLAC (константа недоступна на старых API)
                            description = "FLAC format, 16kHz"
                        )
                    } else {
                        // Fallback на M4A для старых Android
                        println("[DEBUG_LOG] VoiceRecorder: FLAC not supported on API ${Build.VERSION.SDK_INT}, using M4A fallback")
                        RecordingFormat(
                            fileExtension = ".m4a",
                            outputFormat = MediaRecorder.OutputFormat.MPEG_4,
                            audioEncoder = MediaRecorder.AudioEncoder.AAC,
                            description = "AAC/M4A format (FLAC fallback), 16kHz, 64kbps"
                        )
                    }
                }
            }
            
            // Создаем временный файл для записи
            audioFile = File.createTempFile("voice_", recordingFormat.fileExtension, androidContext.cacheDir)
            println("[DEBUG_LOG] VoiceRecorder: Created temp file: ${audioFile?.absolutePath}")
            
            // Создаем MediaRecorder
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(androidContext)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            
            mediaRecorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(recordingFormat.outputFormat)
                setAudioEncoder(recordingFormat.audioEncoder)
                setAudioSamplingRate(16000)  // 16kHz sample rate (optimal for speech)
                
                // Bitrate только для AAC (FLAC использует lossless сжатие)
                if (format == AudioFormat.M4A) {
                    setAudioEncodingBitRate(64000)  // 64 kbps (good quality for speech)
                }
                
                setOutputFile(audioFile?.absolutePath)
                
                prepare()
                start()
                println("[DEBUG_LOG] VoiceRecorder: Recording started (${recordingFormat.description})")
            }
            
        } catch (e: IOException) {
            recording = false
            val errorMsg = "Ошибка запуска записи: ${e.message}"
            println("[DEBUG_LOG] VoiceRecorder: $errorMsg")
            e.printStackTrace()
            errorCallback?.invoke(errorMsg)
            cleanup()
        } catch (e: Exception) {
            recording = false
            val errorMsg = "Неизвестная ошибка: ${e.message}"
            println("[DEBUG_LOG] VoiceRecorder: $errorMsg")
            e.printStackTrace()
            errorCallback?.invoke(errorMsg)
            cleanup()
        }
    }
    
    actual fun stopRecording() {
        if (!recording) {
            println("[DEBUG_LOG] VoiceRecorder: Not recording, ignoring stop")
            return
        }
        
        try {
            println("[DEBUG_LOG] VoiceRecorder: Stopping recording")
            mediaRecorder?.stop()
            mediaRecorder?.release()
            mediaRecorder = null
            recording = false
            
            // Читаем аудио файл и передаем данные в callback
            val file = audioFile
            if (file != null && file.exists()) {
                val audioData = file.readBytes()
                println("[DEBUG_LOG] VoiceRecorder: Read ${audioData.size} bytes from audio file")
                resultCallback?.invoke(audioData)
                
                // Удаляем временный файл
                file.delete()
                audioFile = null
            } else {
                println("[DEBUG_LOG] VoiceRecorder: Audio file not found or null")
                errorCallback?.invoke("Аудио файл не найден")
            }
        } catch (e: Exception) {
            recording = false
            val errorMsg = "Ошибка остановки записи: ${e.message}"
            println("[DEBUG_LOG] VoiceRecorder: $errorMsg")
            e.printStackTrace()
            errorCallback?.invoke(errorMsg)
            cleanup()
        }
    }
    
    actual fun cancelRecording() {
        if (!recording) {
            println("[DEBUG_LOG] VoiceRecorder: Not recording, ignoring cancel")
            return
        }
        
        try {
            println("[DEBUG_LOG] VoiceRecorder: Cancelling recording (no callback)")
            mediaRecorder?.stop()
            mediaRecorder?.release()
            mediaRecorder = null
            recording = false
            
            // Удаляем временный файл без вызова callback
            audioFile?.delete()
            audioFile = null
            
            // Очищаем колбэки, чтобы они не вызывались
            resultCallback = null
            errorCallback = null
        } catch (e: Exception) {
            recording = false
            println("[DEBUG_LOG] VoiceRecorder: Error cancelling recording: ${e.message}")
            e.printStackTrace()
            cleanup()
        }
    }
    
    actual fun isRecording(): Boolean {
        return recording
    }
    
    private fun cleanup() {
        try {
            mediaRecorder?.release()
            mediaRecorder = null
            audioFile?.delete()
            audioFile = null
        } catch (e: Exception) {
            println("[DEBUG_LOG] VoiceRecorder: Error during cleanup: ${e.message}")
        }
    }
    
    private fun buildPermissionErrorMessage(): String {
        return """
            Недостаточно разрешений для записи аудио.
            
            Требуется разрешение: RECORD_AUDIO (запись аудио)
            
            Чтобы изменить разрешения:
            1. Откройте Настройки устройства
            2. Приложения → Family Tree
            3. Разрешения → Микрофон
            4. Включите разрешение "Микрофон"
            
            Или используйте кнопку в диалоге для быстрого перехода в настройки.
        """.trimIndent()
    }
    
    /**
     * Открывает настройки приложения на Android, где пользователь может изменить разрешения.
     */
    actual fun openAppSettings() {
        try {
            val context = androidContext ?: return
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            println("[DEBUG_LOG] VoiceRecorder: Opening app settings")
        } catch (e: Exception) {
            println("[DEBUG_LOG] VoiceRecorder: Failed to open app settings: ${e.message}")
            e.printStackTrace()
        }
    }
}
