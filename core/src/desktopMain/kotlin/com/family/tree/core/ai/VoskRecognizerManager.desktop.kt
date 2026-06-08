package com.family.tree.core.ai

import org.vosk.Model
import org.vosk.Recognizer
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual class VoskRecognizerManager actual constructor() {

    private val modelsDir = File(System.getProperty("user.home"), ".family-tree/vosk-models")

    private fun getModelUrl(language: String): String {
        return when (language.lowercase()) {
            "ru", "rus" -> "https://alphacephei.com/vosk/models/vosk-model-small-ru-0.22.zip"
            "en", "eng" -> "https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip"
            else -> "https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip"
        }
    }

    private fun getModelDirName(language: String): String {
        return when (language.lowercase()) {
            "ru", "rus" -> "vosk-model-small-ru-0.22"
            "en", "eng" -> "vosk-model-small-en-us-0.15"
            else -> "vosk-model-small-en-us-0.15"
        }
    }

    actual fun isModelDownloaded(language: String): Boolean {
        val modelPath = File(modelsDir, getModelDirName(language))
        return modelPath.exists() && modelPath.isDirectory
    }

    actual suspend fun downloadModel(language: String, onProgress: (Float) -> Unit): String = withContext(Dispatchers.IO) {
        if (!modelsDir.exists()) {
            modelsDir.mkdirs()
        }

        val dirName = getModelDirName(language)
        val finalDir = File(modelsDir, dirName)
        if (finalDir.exists()) {
            return@withContext finalDir.absolutePath
        }

        val urlString = getModelUrl(language)
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        val totalBytes = connection.contentLengthLong

        val zipFile = File(modelsDir, "$dirName.zip")
        
        connection.inputStream.use { input ->
            FileOutputStream(zipFile).use { output ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                var totalRead = 0L
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    totalRead += bytesRead
                    if (totalBytes > 0) {
                        onProgress(totalRead.toFloat() / totalBytes.toFloat())
                    }
                }
            }
        }

        // Unzip
        ZipInputStream(zipFile.inputStream()).use { zis ->
            var zipEntry = zis.nextEntry
            while (zipEntry != null) {
                val newFile = File(modelsDir, zipEntry.name)
                if (zipEntry.isDirectory) {
                    newFile.mkdirs()
                } else {
                    newFile.parentFile?.mkdirs()
                    FileOutputStream(newFile).use { fos ->
                        val buffer = ByteArray(8192)
                        var len: Int
                        while (zis.read(buffer).also { len = it } > 0) {
                            fos.write(buffer, 0, len)
                        }
                    }
                }
                zipEntry = zis.nextEntry
            }
            zis.closeEntry()
        }

        zipFile.delete()
        
        // Some zips contain a folder with the same name inside, let's just return finalDir
        // The URL provided above contains the root folder like "vosk-model-small-ru-0.22"
        return@withContext finalDir.absolutePath
    }

    private var cachedModel: Model? = null
    private var cachedModelPath: String? = null

    actual suspend fun transcribeAudio(audioData: ByteArray, language: String): String = withContext(Dispatchers.IO) {
        val dirName = getModelDirName(language)
        val finalDir = File(modelsDir, dirName)
        if (!finalDir.exists()) {
            throw Exception("Model not downloaded. Call downloadModel first.")
        }
        
        // Vosk requires 16kHz 16-bit mono PCM. 
        // We assume audioData is already in this format, or the app handles it.
        
        // Cache the model to avoid slow reloads
        if (cachedModel == null || cachedModelPath != finalDir.absolutePath) {
            cachedModel?.close()
            cachedModel = Model(finalDir.absolutePath)
            cachedModelPath = finalDir.absolutePath
        }
        
        val recognizer = Recognizer(cachedModel, 16000f)
        
        // Strip WAV header if present (44 bytes)
        val rawData = if (audioData.size > 44 && 
            audioData[0] == 'R'.code.toByte() && 
            audioData[1] == 'I'.code.toByte() && 
            audioData[2] == 'F'.code.toByte() && 
            audioData[3] == 'F'.code.toByte()) {
            audioData.copyOfRange(44, audioData.size)
        } else {
            audioData
        }
        
        recognizer.acceptWaveForm(rawData, rawData.size)
        val result = recognizer.finalResult
        
        recognizer.close()
        // Do not close model here as it is cached
        
        // Fix for encoding issues on Windows where Vosk JNI might return UTF-8 bytes 
        // incorrectly decoded as system default charset (e.g. CP1251)
        val fixedResult = try {
            // If the string contains characters typical for UTF-8-as-CP1251 mojibake
            // (like Cyrillic capital Er 'Р' (U+0420) instead of UTF-8 lead byte 0xD0)
            if (result.contains("\u0420") || result.contains("\u0421") || result.contains("\u00D0") || result.contains("\u00D1")) {
                // Try to recover by encoding back to CP1251 and decoding as UTF-8
                // We try windows-1251 first as it's the most common for Russian mojibake
                val bytes = try {
                    result.toByteArray(java.nio.charset.Charset.forName("windows-1251"))
                } catch (e: Exception) {
                    result.toByteArray(java.nio.charset.Charset.defaultCharset())
                }
                val decoded = String(bytes, Charsets.UTF_8)
                // If it looks like valid JSON now, use it
                if (decoded.contains("\"text\"")) decoded else result
            } else {
                result
            }
        } catch (e: Exception) {
            result
        }
        
        // Vosk returns JSON like { "text": "распознанный текст" }
        // We can parse it manually since we only need the text.
        val textMatch = "\"text\"\\s*:\\s*\"(.*?)\"".toRegex().find(fixedResult)
        return@withContext textMatch?.groupValues?.get(1) ?: ""
    }
}
