package com.family.tree.core.platform

/**
 * iOS implementation of FileGateway.
 * TODO: Implement file picker using UIKit APIs.
 */
actual object FileGateway {
    actual fun pickOpen(): ByteArray? {
        println("[DEBUG_LOG] FileGateway.pickOpen: iOS implementation not yet available")
        return null
    }

    actual fun pickSave(data: ByteArray): Boolean {
        println("[DEBUG_LOG] FileGateway.pickSave: iOS implementation not yet available")
        return false
    }
}
