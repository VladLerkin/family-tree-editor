package com.family.tree.core.platform

actual class VoiceRecorder actual constructor(context: Any?) {
    actual fun isAvailable(): Boolean {
        // TODO: Реализовать для веб используя MediaRecorder API
        return false
    }

    actual fun startRecording(
        format: AudioFormat,
        onResult: (ByteArray) -> Unit,
        onError: (String) -> Unit
    ) {
        onError("Voice recording is not supported on Web yet")
    }

    actual fun stopRecording() {
        // No-op
    }

    actual fun cancelRecording() {
        // No-op
    }

    actual fun isRecording(): Boolean {
        return false
    }

    actual fun openAppSettings() {
        // No-op
    }
}
