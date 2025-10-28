package com.family.tree.core.platform

/**
 * Cross-platform file access abstraction for future storage integration.
 *
 * This is a minimal expect/actual placeholder so that UI/Desktop/Android code
 * can start wiring file actions without blocking on real implementations.
 *
 * For now, both functions are synchronous and return nullable/boolean results.
 * They can be evolved to suspend versions later if needed.
 */
expect object FileGateway {
    /**
     * Opens a platform-specific file picker and returns file bytes, or null if cancelled/not implemented.
     */
    fun pickOpen(): ByteArray?

    /**
     * Opens a platform-specific save dialog and writes [data]. Returns true on success.
     */
    fun pickSave(data: ByteArray): Boolean
}
