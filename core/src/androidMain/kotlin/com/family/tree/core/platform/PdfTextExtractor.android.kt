package com.family.tree.core.platform

import android.content.Context
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper

actual class PdfTextExtractor actual constructor(context: Any?) {
    
    init {
        // Initialize PDFBox resources with Android context
        try {
            val androidContext = context as? Context
            if (androidContext != null) {
                PDFBoxResourceLoader.init(androidContext)
                println("[DEBUG_LOG] PdfTextExtractor: PDFBoxResourceLoader initialized successfully")
            } else {
                println("[DEBUG_LOG] PdfTextExtractor: WARNING - No Android Context provided, PDF extraction will fail")
            }
        } catch (e: Exception) {
            println("[DEBUG_LOG] PdfTextExtractor: Failed to initialize PDFBoxResourceLoader: ${e.message}")
            e.printStackTrace()
        }
    }
    
    actual fun extractText(pdfBytes: ByteArray): String? {
        return try {
            // Load PDF document from bytes
            val document = PDDocument.load(pdfBytes)
            
            // Extract text using PDFTextStripper
            val stripper = PDFTextStripper()
            val text = stripper.getText(document)
            
            // Close document
            document.close()
            
            text
        } catch (e: Exception) {
            println("[DEBUG_LOG] PdfTextExtractor: Failed to extract text from PDF: ${e.message}")
            e.printStackTrace()
            null
        }
    }
}
