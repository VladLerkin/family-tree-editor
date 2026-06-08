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
import android.content.Context
import org.koin.core.context.GlobalContext

actual class VoskRecognizerManager actual constructor() {

    private val context: Context by lazy {
        GlobalContext.get().get<Context>()
    }

    private val modelsDir: File by lazy {
        File(context.filesDir, "vosk-models")
    }

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
        
        return@withContext finalDir.absolutePath
    }

    actual suspend fun transcribeAudio(audioData: ByteArray, language: String): String = withContext(Dispatchers.IO) {
        val dirName = getModelDirName(language)
        val finalDir = File(modelsDir, dirName)
        if (!finalDir.exists()) {
            throw Exception("Model not downloaded. Call downloadModel first.")
        }
        
        // Vosk requires 16kHz 16-bit mono PCM. 
        val model = Model(finalDir.absolutePath)
        val recognizer = Recognizer(model, 16000f)
        
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
        model.close()
        
        val textMatch = "\"text\"\\s*:\\s*\"(.*?)\"".toRegex().find(result)
        return@withContext textMatch?.groupValues?.get(1) ?: ""
    }
}
