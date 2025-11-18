package com.family.tree.core.platform

actual class PdfTextExtractor(context: Any? = null) {
    actual fun extractText(pdfBytes: ByteArray): String? {
        println("[DEBUG_LOG] PdfTextExtractor.ios: PDF extraction not supported on iOS")
        return null
    }
}
