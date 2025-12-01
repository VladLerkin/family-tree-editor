package com.family.tree.core.platform

actual object FileGateway {
    actual fun pickOpen(): ByteArray? {
        // Android SAF implementation will be provided later from an Activity/Context.
        // For now, return null as a stub to keep compilation unblocked.
        return null
    }

    actual fun pickSave(data: ByteArray): Boolean {
        // Android SAF save will be implemented later. Stub returns false for now.
        return false
    }
}
