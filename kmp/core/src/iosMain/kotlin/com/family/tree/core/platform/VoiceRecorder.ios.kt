package com.family.tree.core.platform

actual class VoiceRecorder actual constructor(context: Any?) {
    
    private var recording = false
    
    actual fun isAvailable(): Boolean {
        // На iOS платформе голосовой ввод не реализован в текущей версии
        // Требуется интеграция с Speech framework через Objective-C/Swift
        return false
    }
    
    actual fun startRecording(
        onResult: (ByteArray) -> Unit,
        onError: (String) -> Unit
    ) {
        println("[DEBUG_LOG] VoiceRecorder (iOS): Audio recording is not available on iOS platform")
        onError("Запись аудио не поддерживается на iOS платформе в текущей версии")
    }
    
    actual fun stopRecording() {
        recording = false
        println("[DEBUG_LOG] VoiceRecorder (iOS): Stop recording called")
    }
    
    actual fun cancelRecording() {
        recording = false
        println("[DEBUG_LOG] VoiceRecorder (iOS): Cancel recording called")
    }
    
    actual fun isRecording(): Boolean {
        return recording
    }
    
    actual fun openAppSettings() {
        // No-op на iOS платформе
        println("[DEBUG_LOG] VoiceRecorder (iOS): openAppSettings called, but not supported on iOS")
    }
}
