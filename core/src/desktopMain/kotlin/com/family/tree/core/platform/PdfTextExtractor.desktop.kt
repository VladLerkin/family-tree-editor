package com.family.tree.core.platform

actual class PdfTextExtractor actual constructor(context: Any?) {
    actual fun extractText(pdfBytes: ByteArray): String? {
        println("[DEBUG_LOG] PdfTextExtractor.desktop: PDF extraction not supported on Desktop")
        return null
    }
}
