package com.family.tree.core.platform

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
     * @param onResult callback с аудио данными (ByteArray) при успехе
     * @param onError callback с сообщением об ошибке
     */
    fun startRecording(
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
