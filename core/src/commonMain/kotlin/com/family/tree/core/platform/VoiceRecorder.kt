package com.family.tree.core.platform

/**
 * Форматы аудио для записи
 */
enum class AudioFormat {
    /**
     * M4A/AAC формат - оптимален для OpenAI Whisper API
     * Поддерживается: Android, iOS, Desktop
     */
    M4A,
    
    /**
     * FLAC формат - оптимален для Google Speech-to-Text API
     * Поддерживается: Android, Desktop
     * На iOS будет использован M4A как fallback
     */
    FLAC,
    
    /**
     * WAV (LPCM) format - required for Yandex SpeechKit
     * Supported: Android (via AudioRecord), iOS (default), Desktop
     */
    WAV
}

/**
 * Платформо-специфичный интерфейс для записи аудио
 */
expect class VoiceRecorder(context: Any?) {
    /**
     * Проверка доступности записи аудио на платформе
     */
    fun isAvailable(): Boolean
    
    /**
     * Начать запись аудио
     * @param format формат записи (M4A или FLAC)
     * @param onResult callback с аудио данными (ByteArray) при успехе
     * @param onError callback с сообщением об ошибке
     */
    fun startRecording(
        format: AudioFormat = AudioFormat.M4A,
        onResult: (ByteArray) -> Unit,
        onError: (String) -> Unit
    )
    
    /**
     * Остановить запись и обработать результат
     */
    fun stopRecording()
    
    /**
     * Отменить запись без обработки результата
     */
    fun cancelRecording()
    
    /**
     * Проверка, идет ли сейчас запись
     */
    fun isRecording(): Boolean
    
    /**
     * Открывает настройки приложения для изменения разрешений
     * (работает только на Android, на других платформах - no-op)
     */
    fun openAppSettings()
}
