package com.family.tree.core.platform

import kotlinx.cinterop.*
import platform.AVFAudio.*
import platform.Foundation.*
import platform.UIKit.UIApplication
import platform.darwin.NSObject
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class)
actual class VoiceRecorder actual constructor(context: Any?) {
    
    private var recording = false
    private var audioRecorder: AVAudioRecorder? = null
    private var audioFile: NSURL? = null
    private var resultCallback: ((ByteArray) -> Unit)? = null
    private var errorCallback: ((String) -> Unit)? = null
    
    actual fun isAvailable(): Boolean {
        // Проверяем доступность аудио сессии на iOS
        return try {
            val audioSession = AVAudioSession.sharedInstance()
            audioSession.category() != null
            true
        } catch (e: Exception) {
            println("[DEBUG_LOG] VoiceRecorder (iOS): Error checking audio availability: ${e.message}")
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
            // Настраиваем аудио сессию
            val audioSession = AVAudioSession.sharedInstance()
            memScoped {
                val error = alloc<ObjCObjectVar<NSError?>>()
                audioSession.setCategory(
                    category = AVAudioSessionCategoryRecord,
                    error = error.ptr
                )
                
                if (error.value != null) {
                    recording = false
                    val errorMsg = "Ошибка настройки аудио сессии: ${error.value?.localizedDescription}"
                    println("[DEBUG_LOG] VoiceRecorder (iOS): $errorMsg")
                    errorCallback?.invoke(errorMsg)
                    return
                }
                
                audioSession.setActive(true, error.ptr)
                if (error.value != null) {
                    recording = false
                    val errorMsg = "Ошибка активации аудио сессии: ${error.value?.localizedDescription}"
                    println("[DEBUG_LOG] VoiceRecorder (iOS): $errorMsg")
                    errorCallback?.invoke(errorMsg)
                    return
                }
            }
            
            // Создаем временный файл для записи в формате .caf (Linear PCM)
            // iOS Simulator не поддерживает AAC кодек, используем Linear PCM
            val tempDir = NSTemporaryDirectory()
            val fileName = "voice_${NSDate().timeIntervalSince1970}.caf"
            val filePath = "$tempDir$fileName"
            audioFile = NSURL.fileURLWithPath(filePath)
            
            println("[DEBUG_LOG] VoiceRecorder (iOS): Created temp file: $filePath")
            
            // Настройки записи: Linear PCM формат (работает на симуляторе и устройствах)
            // kAudioFormatLinearPCM = 'lpcm' = 0x6C70636D = 1819304813
            val settings = mapOf<Any?, Any>(
                AVFormatIDKey to NSNumber.numberWithUnsignedInt(1819304813u),
                AVSampleRateKey to NSNumber.numberWithDouble(16000.0),
                AVNumberOfChannelsKey to NSNumber.numberWithInt(1),
                AVLinearPCMBitDepthKey to NSNumber.numberWithInt(16),
                AVLinearPCMIsBigEndianKey to NSNumber.numberWithBool(false),
                AVLinearPCMIsFloatKey to NSNumber.numberWithBool(false)
            )
            
            // Создаем рекордер
            memScoped {
                val error = alloc<ObjCObjectVar<NSError?>>()
                audioRecorder = AVAudioRecorder(
                    uRL = audioFile!!,
                    settings = settings,
                    error = error.ptr
                )
                
                if (error.value != null || audioRecorder == null) {
                    recording = false
                    val errorMsg = "Ошибка создания рекордера: ${error.value?.localizedDescription}"
                    println("[DEBUG_LOG] VoiceRecorder (iOS): $errorMsg")
                    errorCallback?.invoke(errorMsg)
                    cleanup()
                    return
                }
            }
            
            // Запускаем запись
            val success = audioRecorder?.record() ?: false
            if (success) {
                println("[DEBUG_LOG] VoiceRecorder (iOS): Recording started")
            } else {
                recording = false
                val errorMsg = "Не удалось начать запись"
                println("[DEBUG_LOG] VoiceRecorder (iOS): $errorMsg")
                errorCallback?.invoke(errorMsg)
                cleanup()
            }
            
        } catch (e: Exception) {
            recording = false
            val errorMsg = "Ошибка запуска записи: ${e.message}"
            println("[DEBUG_LOG] VoiceRecorder (iOS): $errorMsg")
            e.printStackTrace()
            errorCallback?.invoke(errorMsg)
            cleanup()
        }
    }
    
    actual fun stopRecording() {
        if (!recording) {
            println("[DEBUG_LOG] VoiceRecorder (iOS): Not recording, ignoring stop")
            return
        }
        
        try {
            println("[DEBUG_LOG] VoiceRecorder (iOS): Stopping recording")
            audioRecorder?.stop()
            recording = false
            
            // Деактивируем аудио сессию
            memScoped {
                val error = alloc<ObjCObjectVar<NSError?>>()
                AVAudioSession.sharedInstance().setActive(false, error.ptr)
            }
            
            // Читаем записанный файл
            audioFile?.let { url ->
                val fileData = NSData.dataWithContentsOfURL(url)
                if (fileData != null) {
                    val length = fileData.length.toInt()
                    val byteArray = ByteArray(length)
                    byteArray.usePinned { pinned ->
                        memcpy(pinned.addressOf(0), fileData.bytes, length.toULong())
                    }
                    
                    println("[DEBUG_LOG] VoiceRecorder (iOS): Audio file size: $length bytes")
                    resultCallback?.invoke(byteArray)
                } else {
                    val errorMsg = "Не удалось прочитать аудио файл"
                    println("[DEBUG_LOG] VoiceRecorder (iOS): $errorMsg")
                    errorCallback?.invoke(errorMsg)
                }
            } ?: run {
                val errorMsg = "Аудио файл не найден"
                println("[DEBUG_LOG] VoiceRecorder (iOS): $errorMsg")
                errorCallback?.invoke(errorMsg)
            }
            
        } catch (e: Exception) {
            val errorMsg = "Ошибка остановки записи: ${e.message}"
            println("[DEBUG_LOG] VoiceRecorder (iOS): $errorMsg")
            e.printStackTrace()
            errorCallback?.invoke(errorMsg)
        } finally {
            cleanup()
        }
    }
    
    actual fun cancelRecording() {
        if (!recording) {
            println("[DEBUG_LOG] VoiceRecorder (iOS): Not recording, ignoring cancel")
            return
        }
        
        try {
            println("[DEBUG_LOG] VoiceRecorder (iOS): Cancelling recording")
            audioRecorder?.stop()
            recording = false
            
            // Деактивируем аудио сессию
            memScoped {
                val error = alloc<ObjCObjectVar<NSError?>>()
                AVAudioSession.sharedInstance().setActive(false, error.ptr)
            }
            
            println("[DEBUG_LOG] VoiceRecorder (iOS): Recording cancelled")
            
        } catch (e: Exception) {
            println("[DEBUG_LOG] VoiceRecorder (iOS): Error during cancel: ${e.message}")
            e.printStackTrace()
        } finally {
            cleanup()
        }
    }
    
    actual fun isRecording(): Boolean {
        return recording
    }
    
    actual fun openAppSettings() {
        // Открываем настройки приложения на iOS
        try {
            val settingsUrl = NSURL.URLWithString("app-settings:")
            if (settingsUrl != null && UIApplication.sharedApplication.canOpenURL(settingsUrl)) {
                UIApplication.sharedApplication.openURL(settingsUrl)
            }
            println("[DEBUG_LOG] VoiceRecorder (iOS): Opening app settings")
        } catch (e: Exception) {
            println("[DEBUG_LOG] VoiceRecorder (iOS): Error opening settings: ${e.message}")
        }
    }
    
    private fun cleanup() {
        audioRecorder = null
        
        // Удаляем временный файл
        audioFile?.let { url ->
            try {
                memScoped {
                    val error = alloc<ObjCObjectVar<NSError?>>()
                    NSFileManager.defaultManager.removeItemAtURL(url, error.ptr)
                    if (error.value != null) {
                        println("[DEBUG_LOG] VoiceRecorder (iOS): Error deleting temp file: ${error.value?.localizedDescription}")
                    }
                }
            } catch (e: Exception) {
                println("[DEBUG_LOG] VoiceRecorder (iOS): Error during cleanup: ${e.message}")
            }
        }
        audioFile = null
    }
}
