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
import java.io.FileOutputStream
import java.io.FileInputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import android.media.AudioRecord
import android.media.AudioFormat as AndroidAudioFormat
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.Future
import android.os.Handler
import android.os.Looper

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
    
    // WAV recording state
    private var audioRecord: AudioRecord? = null
    private var recordingThread: Thread? = null
    private var isRecordingWav = false
    private val SAMPLE_RATE = 16000
    private val CHANNEL_CONFIG = AndroidAudioFormat.CHANNEL_IN_MONO
    private val AUDIO_FORMAT = AndroidAudioFormat.ENCODING_PCM_16BIT
    
    // Executor for background operations
    private val executor = Executors.newSingleThreadExecutor()
    // Separate executor for stop operations to avoid deadlock
    private val stopExecutor = Executors.newSingleThreadExecutor()
    private val STOP_TIMEOUT_SECONDS = 3L
    
    // Handler for posting callbacks to main thread
    private val mainHandler = Handler(Looper.getMainLooper())
    
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
            // На Android TV устройствах MediaRecorder.stop() зависает, используем AudioRecord для всех форматов
            val isAndroidTV = Build.MANUFACTURER.equals("Xiaomi", ignoreCase = true) && 
                             Build.MODEL.contains("MiTV", ignoreCase = true)
            
            if (isAndroidTV) {
                println("[DEBUG_LOG] VoiceRecorder: Detected Android TV device, using AudioRecord instead of MediaRecorder")
                startWavRecording()
                return
            }
            
            // Handle WAV format separately using AudioRecord
            if (format == AudioFormat.WAV) {
                startWavRecording()
                return
            }

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
                AudioFormat.WAV -> {
                    throw IllegalStateException("WAV format should be handled separately")
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
            
            // Post error callback to main thread
            mainHandler.post {
                errorCallback?.invoke(errorMsg)
            }
            cleanup()
        } catch (e: Exception) {
            recording = false
            val errorMsg = "Неизвестная ошибка: ${e.message}"
            println("[DEBUG_LOG] VoiceRecorder: $errorMsg")
            e.printStackTrace()
            
            // Post error callback to main thread
            mainHandler.post {
                errorCallback?.invoke(errorMsg)
            }
            cleanup()
        }
    }
    
    actual fun stopRecording() {
        if (!recording) {
            println("[DEBUG_LOG] VoiceRecorder: Not recording, ignoring stop")
            return
        }
        
        if (isRecordingWav) {
            stopWavRecording()
            return
        }
        
        val recorder = mediaRecorder
        val file = audioFile
        val onResult = resultCallback
        val onError = errorCallback
        
        println("[DEBUG_LOG] VoiceRecorder: Stopping recording")
        recording = false
        
        // Execute stop operation in background thread with timeout
        executor.execute {
            try {
                if (recorder != null) {
                    // Safely stop MediaRecorder with timeout - can hang on some Android TV devices
                    val stopFuture: Future<*> = stopExecutor.submit {
                        try {
                            println("[DEBUG_LOG] VoiceRecorder: Calling MediaRecorder.stop() in background thread")
                            recorder.stop()
                            println("[DEBUG_LOG] VoiceRecorder: MediaRecorder stopped successfully")
                        } catch (e: IllegalStateException) {
                            println("[DEBUG_LOG] VoiceRecorder: IllegalStateException during stop (device-specific issue): ${e.message}")
                            // Continue with cleanup even if stop() fails
                        } catch (e: RuntimeException) {
                            println("[DEBUG_LOG] VoiceRecorder: RuntimeException during stop: ${e.message}")
                            // Continue with cleanup
                        }
                    }
                    
                    // Wait for stop with timeout
                    try {
                        stopFuture.get(STOP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    } catch (e: java.util.concurrent.TimeoutException) {
                        println("[DEBUG_LOG] VoiceRecorder: MediaRecorder.stop() timeout after ${STOP_TIMEOUT_SECONDS}s, forcing cleanup")
                        stopFuture.cancel(true)
                    }
                    
                    // Reset before release to ensure proper cleanup (important for some Android TV devices)
                    try {
                        recorder.reset()
                        println("[DEBUG_LOG] VoiceRecorder: MediaRecorder reset successfully")
                    } catch (e: Exception) {
                        println("[DEBUG_LOG] VoiceRecorder: Exception during reset: ${e.message}")
                    }
                    
                    // Release resources
                    try {
                        recorder.release()
                        println("[DEBUG_LOG] VoiceRecorder: MediaRecorder released successfully")
                    } catch (e: Exception) {
                        println("[DEBUG_LOG] VoiceRecorder: Exception during release: ${e.message}")
                    }
                }
                
                mediaRecorder = null
                
                // Читаем аудио файл и передаем данные в callback
                if (file != null && file.exists()) {
                    val audioData = file.readBytes()
                    println("[DEBUG_LOG] VoiceRecorder: Read ${audioData.size} bytes from audio file")
                    
                    // Post callback to main thread
                    mainHandler.post {
                        onResult?.invoke(audioData)
                    }
                    
                    // Удаляем временный файл
                    file.delete()
                    audioFile = null
                } else {
                    println("[DEBUG_LOG] VoiceRecorder: Audio file not found or null")
                    
                    // Post error callback to main thread
                    mainHandler.post {
                        onError?.invoke("Аудио файл не найден")
                    }
                }
            } catch (e: Exception) {
                val errorMsg = "Ошибка остановки записи: ${e.message}"
                println("[DEBUG_LOG] VoiceRecorder: $errorMsg")
                e.printStackTrace()
                
                // Post error callback to main thread
                mainHandler.post {
                    onError?.invoke(errorMsg)
                }
                cleanup()
            }
        }
    }
    
    actual fun cancelRecording() {
        if (!recording) {
            println("[DEBUG_LOG] VoiceRecorder: Not recording, ignoring cancel")
            return
        }
        
        if (isRecordingWav) {
            isRecordingWav = false
            try {
                audioRecord?.stop()
                audioRecord?.release()
                recordingThread?.join()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            audioRecord = null
            recordingThread = null
            recording = false
            audioFile?.delete()
            audioFile = null
            resultCallback = null
            errorCallback = null
            return
        }
        
        val recorder = mediaRecorder
        val file = audioFile
        
        println("[DEBUG_LOG] VoiceRecorder: Cancelling recording (no callback)")
        recording = false
        
        // Execute cancel operation in background thread with timeout
        executor.execute {
            try {
                if (recorder != null) {
                    // Safely stop MediaRecorder with timeout - can hang on some Android TV devices
                    val stopFuture: Future<*> = stopExecutor.submit {
                        try {
                            println("[DEBUG_LOG] VoiceRecorder: Calling MediaRecorder.stop() in background thread (cancel)")
                            recorder.stop()
                            println("[DEBUG_LOG] VoiceRecorder: MediaRecorder stopped successfully (cancel)")
                        } catch (e: IllegalStateException) {
                            println("[DEBUG_LOG] VoiceRecorder: IllegalStateException during cancel stop (device-specific issue): ${e.message}")
                            // Continue with cleanup even if stop() fails
                        } catch (e: RuntimeException) {
                            println("[DEBUG_LOG] VoiceRecorder: RuntimeException during cancel stop: ${e.message}")
                            // Continue with cleanup
                        }
                    }
                    
                    // Wait for stop with timeout
                    try {
                        stopFuture.get(STOP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    } catch (e: java.util.concurrent.TimeoutException) {
                        println("[DEBUG_LOG] VoiceRecorder: MediaRecorder.stop() timeout after ${STOP_TIMEOUT_SECONDS}s during cancel, forcing cleanup")
                        stopFuture.cancel(true)
                    }
                    
                    // Reset before release to ensure proper cleanup (important for some Android TV devices)
                    try {
                        recorder.reset()
                        println("[DEBUG_LOG] VoiceRecorder: MediaRecorder reset successfully (cancel)")
                    } catch (e: Exception) {
                        println("[DEBUG_LOG] VoiceRecorder: Exception during reset (cancel): ${e.message}")
                    }
                    
                    // Release resources
                    try {
                        recorder.release()
                        println("[DEBUG_LOG] VoiceRecorder: MediaRecorder released successfully (cancel)")
                    } catch (e: Exception) {
                        println("[DEBUG_LOG] VoiceRecorder: Exception during release (cancel): ${e.message}")
                    }
                }
                
                mediaRecorder = null
                
                // Удаляем временный файл без вызова callback
                file?.delete()
                audioFile = null
                
                // Очищаем колбэки, чтобы они не вызывались
                resultCallback = null
                errorCallback = null
            } catch (e: Exception) {
                println("[DEBUG_LOG] VoiceRecorder: Error cancelling recording: ${e.message}")
                e.printStackTrace()
                cleanup()
            }
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
    private fun startWavRecording() {
        try {
            val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
            if (minBufferSize == AudioRecord.ERROR || minBufferSize == AudioRecord.ERROR_BAD_VALUE) {
                throw Exception("AudioRecord minBufferSize error: $minBufferSize")
            }
            
            println("[DEBUG_LOG] VoiceRecorder: minBufferSize = $minBufferSize")
            
            audioFile = File.createTempFile("voice_", ".wav", androidContext?.cacheDir)
            println("[DEBUG_LOG] VoiceRecorder: Created temp WAV file: ${audioFile?.absolutePath}")
            
            if (androidContext?.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                throw Exception("Permission denied")
            }
            
            // Try different audio sources for Android TV compatibility
            val audioSources = listOf(
                MediaRecorder.AudioSource.VOICE_RECOGNITION to "VOICE_RECOGNITION",
                MediaRecorder.AudioSource.MIC to "MIC",
                MediaRecorder.AudioSource.VOICE_COMMUNICATION to "VOICE_COMMUNICATION",
                MediaRecorder.AudioSource.DEFAULT to "DEFAULT"
            )
            
            var lastException: Exception? = null
            for ((source, sourceName) in audioSources) {
                try {
                    println("[DEBUG_LOG] VoiceRecorder: Trying audio source: $sourceName")
                    audioRecord = AudioRecord(source, SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, minBufferSize * 2)
                    
                    if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                        println("[DEBUG_LOG] VoiceRecorder: AudioRecord not initialized with $sourceName (state=${audioRecord?.state})")
                        audioRecord?.release()
                        audioRecord = null
                        continue
                    }
                    
                    println("[DEBUG_LOG] VoiceRecorder: AudioRecord initialized successfully with $sourceName")
                    audioRecord?.startRecording()
                    
                    // Check if recording actually works by reading a small test buffer
                    val testBuffer = ByteArray(minBufferSize)
                    Thread.sleep(100) // Give it time to start
                    val testRead = audioRecord?.read(testBuffer, 0, minBufferSize) ?: 0
                    println("[DEBUG_LOG] VoiceRecorder: Test read from $sourceName: $testRead bytes")
                    
                    if (testRead > 0) {
                        println("[DEBUG_LOG] VoiceRecorder: Successfully started recording with $sourceName")
                        isRecordingWav = true
                        
                        recordingThread = Thread {
                            writeWavDataToFile(minBufferSize)
                        }
                        recordingThread?.start()
                        return // Success!
                    } else {
                        println("[DEBUG_LOG] VoiceRecorder: $sourceName returned 0 bytes, trying next source")
                        audioRecord?.stop()
                        audioRecord?.release()
                        audioRecord = null
                    }
                } catch (e: Exception) {
                    println("[DEBUG_LOG] VoiceRecorder: Failed with $sourceName: ${e.message}")
                    lastException = e
                    audioRecord?.release()
                    audioRecord = null
                }
            }
            
            // If we get here, all sources failed
            throw Exception("Не удалось инициализировать запись аудио ни с одним источником. Возможно, на этом устройстве нет микрофона или он не поддерживается. Последняя ошибка: ${lastException?.message}")
            
        } catch (e: Exception) {
            recording = false
            isRecordingWav = false
            val errorMsg = "Ошибка запуска WAV записи: ${e.message}"
            println("[DEBUG_LOG] VoiceRecorder: $errorMsg")
            e.printStackTrace()
            
            // Post error callback to main thread
            mainHandler.post {
                errorCallback?.invoke(errorMsg)
            }
            cleanup()
        }
    }
    
    private fun writeWavDataToFile(bufferSize: Int) {
        val data = ByteArray(bufferSize)
        val file = audioFile ?: return
        
        try {
            val os = FileOutputStream(file)
            // Write placeholder header
            os.write(ByteArray(44))
            
            while (isRecordingWav) {
                val read = audioRecord?.read(data, 0, bufferSize) ?: 0
                if (read > 0) {
                    os.write(data, 0, read)
                }
            }
            os.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun stopWavRecording() {
        try {
            isRecordingWav = false
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
            
            recordingThread?.join()
            recordingThread = null
            recording = false
            
            val file = audioFile
            if (file != null && file.exists()) {
                updateWavHeader(file)
                
                val audioData = file.readBytes()
                println("[DEBUG_LOG] VoiceRecorder: Read ${audioData.size} bytes from WAV file")
                
                // Post callback to main thread
                mainHandler.post {
                    resultCallback?.invoke(audioData)
                }
                
                file.delete()
                audioFile = null
            } else {
                // Post error callback to main thread
                mainHandler.post {
                    errorCallback?.invoke("Аудио файл не найден")
                }
            }
        } catch (e: Exception) {
            val errorMsg = "Ошибка остановки WAV записи: ${e.message}"
            println("[DEBUG_LOG] VoiceRecorder: $errorMsg")
            e.printStackTrace()
            
            // Post error callback to main thread
            mainHandler.post {
                errorCallback?.invoke(errorMsg)
            }
            cleanup()
        }
    }
    
    private fun updateWavHeader(file: File) {
        try {
            val fileSize = file.length()
            val totalDataLen = fileSize - 8
            val totalAudioLen = fileSize - 44
            val byteRate = SAMPLE_RATE * 16 * 1 / 8
            
            val randomAccessFile = RandomAccessFile(file, "rw")
            randomAccessFile.seek(0)
            
            val header = ByteBuffer.allocate(44)
            header.order(ByteOrder.LITTLE_ENDIAN)
            
            header.put("RIFF".toByteArray())
            header.putInt(totalDataLen.toInt())
            header.put("WAVE".toByteArray())
            header.put("fmt ".toByteArray())
            header.putInt(16) // Subchunk1Size
            header.putShort(1) // AudioFormat (PCM)
            header.putShort(1) // NumChannels (Mono)
            header.putInt(SAMPLE_RATE)
            header.putInt(byteRate)
            header.putShort(2) // BlockAlign
            header.putShort(16) // BitsPerSample
            header.put("data".toByteArray())
            header.putInt(totalAudioLen.toInt())
            
            randomAccessFile.write(header.array())
            randomAccessFile.close()
        } catch (e: Exception) {
            println("[DEBUG_LOG] VoiceRecorder: Error updating WAV header: ${e.message}")
            e.printStackTrace()
        }
    }
}
