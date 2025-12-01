package com.family.tree.core.platform

/**
 * Platform-specific PDF text extractor.
 * Extracts text content from PDF files for AI import processing.
 */
expect class PdfTextExtractor {
    /**
     * Extracts text from a PDF file.
     * @param pdfBytes PDF file content as ByteArray
     * @return Extracted text, or null if extraction fails
     */
    fun extractText(pdfBytes: ByteArray): String?
}
