package com.family.tree.core.platform

actual class PdfTextExtractor(context: Any? = null) {
    actual fun extractText(pdfBytes: ByteArray): String? {
        println("[DEBUG_LOG] PdfTextExtractor.wasmJs: PDF extraction not supported on Web")
        return null
    }
}
