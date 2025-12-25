package com.family.tree.core.platform

actual object FileGateway {
    actual fun pickOpen(): ByteArray? {
        // TODO: Реализовать для веб используя HTML5 File API
        return null
    }

    actual fun pickSave(data: ByteArray): Boolean {
        // TODO: Реализовать для веб используя download API
        return false
    }
}
