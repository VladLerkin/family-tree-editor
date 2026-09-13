package com.family.tree.core.ai

import com.k2fsa.sherpa.onnx.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

actual class SherpaRecognizerManager actual constructor() {

    private val modelsDir = File(System.getProperty("user.home"), ".family-tree/sherpa-models")
    
    init {
        val userDir = System.getProperty("user.dir")
        val possiblePaths = listOf(
            File(userDir, "../libs/sherpa-onnx"), // when running via gradle :app-desktop:run
            File(userDir, "libs/sherpa-onnx"), // when running from root
            File(System.getProperty("compose.application.resources.dir") ?: "", "libs/sherpa-onnx")
        )
        val basePath = possiblePaths.firstOrNull { it.exists() }?.absolutePath ?: File("libs/sherpa-onnx").absolutePath
        val onnxLib = File(basePath, "libonnxruntime.dylib")
        val sherpaCapi = File(basePath, "libsherpa-onnx-c-api.dylib")
        val sherpaJni = File(basePath, "libsherpa-onnx-jni.dylib")
        
        // Tell Sherpa-ONNX JVM wrapper where to find the native library
        System.setProperty("sherpa_onnx.native.path", basePath)
        
        try {
            if (onnxLib.exists()) System.load(onnxLib.absolutePath)
            if (sherpaCapi.exists()) System.load(sherpaCapi.absolutePath)
            if (sherpaJni.exists()) System.load(sherpaJni.absolutePath)
        } catch (e: UnsatisfiedLinkError) {
            e.printStackTrace()
            // Fallback to java.library.path
            try {
                System.loadLibrary("sherpa-onnx-jni")
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    private fun getModelUrl(language: String): String {
        return when (language.lowercase()) {
            "ru" -> "https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/sherpa-onnx-nemo-fast-conformer-ctc-be-de-en-es-fr-hr-it-pl-ru-uk-20k.tar.bz2"
            "en", "eng" -> "https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/sherpa-onnx-zipformer-en-2023-06-26.tar.bz2"
            else -> "https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/sherpa-onnx-zipformer-en-2023-06-26.tar.bz2"
        }
    }

    private fun getModelDirName(language: String): String {
        return when (language.lowercase()) {
            "ru", "rus" -> "sherpa-onnx-nemo-fast-conformer-ctc-be-de-en-es-fr-hr-it-pl-ru-uk-20k"
            "en", "eng" -> "sherpa-onnx-zipformer-en-2023-06-26"
            else -> "sherpa-onnx-zipformer-en-2023-06-26"
        }
    }

    actual fun isModelDownloaded(language: String): Boolean {
        val modelPath = File(modelsDir, getModelDirName(language))
        val successFile = File(modelPath, "_SUCCESS")
        return modelPath.exists() && modelPath.isDirectory && successFile.exists()
    }

    actual fun deleteModel(language: String) {
        val finalDir = File(modelsDir, getModelDirName(language))
        if (finalDir.exists()) {
            finalDir.deleteRecursively()
        }
    }

    actual suspend fun downloadModel(language: String, onProgress: (Float) -> Unit): String = withContext(Dispatchers.IO) {
        if (!modelsDir.exists()) {
            modelsDir.mkdirs()
        }

        val dirName = getModelDirName(language)
        val finalDir = File(modelsDir, dirName)
        val successFile = File(finalDir, "_SUCCESS")
        if (finalDir.exists() && successFile.exists()) {
            return@withContext finalDir.absolutePath
        } else if (finalDir.exists()) {
            finalDir.deleteRecursively()
        }

        val tarFile = File(modelsDir, "$dirName.tar.bz2")
        val tmpFile = File(modelsDir, "$dirName.tar.bz2.tmp")
        var downloadedBytes = 0L
        if (tmpFile.exists()) {
            downloadedBytes = tmpFile.length()
        }

        val urlString = getModelUrl(language)
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.instanceFollowRedirects = true
        if (downloadedBytes > 0) {
            connection.setRequestProperty("Range", "bytes=$downloadedBytes-")
        }
        
        var actualConnection = connection
        var redirect = false
        var status = connection.responseCode
        if (status != HttpURLConnection.HTTP_OK && status != HttpURLConnection.HTTP_PARTIAL) {
            if (status == HttpURLConnection.HTTP_MOVED_TEMP
                || status == HttpURLConnection.HTTP_MOVED_PERM
                || status == HttpURLConnection.HTTP_SEE_OTHER)
            redirect = true
        }
        
        if (redirect) {
            val newUrl = connection.getHeaderField("Location")
            actualConnection = URL(newUrl).openConnection() as HttpURLConnection
            if (downloadedBytes > 0) {
                actualConnection.setRequestProperty("Range", "bytes=$downloadedBytes-")
            }
        }

        var totalBytes = actualConnection.contentLengthLong
        if (downloadedBytes > 0 && actualConnection.responseCode != HttpURLConnection.HTTP_PARTIAL) {
            downloadedBytes = 0L
            tmpFile.delete()
        } else if (downloadedBytes > 0) {
            if (totalBytes >= 0) totalBytes += downloadedBytes
        }
        
        actualConnection.inputStream.use { input ->
            val append = downloadedBytes > 0
            FileOutputStream(tmpFile, append).use { output ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                var totalRead = downloadedBytes
                while (true) {
                    bytesRead = input.read(buffer)
                    if (bytesRead == -1) break
                    output.write(buffer, 0, bytesRead)
                    totalRead += bytesRead
                    if (totalBytes > 0) {
                        onProgress(totalRead.toFloat() / totalBytes.toFloat())
                    }
                }
                if (totalBytes > 0 && totalRead != totalBytes) {
                    throw Exception("Download incomplete: expected $totalBytes bytes but got $totalRead bytes")
                }
            }
        }
        tmpFile.renameTo(tarFile)

        onProgress(2.0f) // Signal extraction phase

        try {
            val process = Runtime.getRuntime().exec(arrayOf("tar", "-xf", tarFile.absolutePath), null, modelsDir)
            val exitCode = process.waitFor()
            if (exitCode != 0) {
                val errorMsg = process.errorStream.bufferedReader().use { it.readText() }
                throw Exception("Native tar extraction failed with code $exitCode: $errorMsg")
            }
        } catch (e: Exception) {
            // Fallback to slow Java extraction if native tar fails or is not available
            val tarFileSize = tarFile.length()
            val bufferedIn = tarFile.inputStream().buffered(65536)
            val bzIn = BZip2CompressorInputStream(bufferedIn)
            val tarIn = TarArchiveInputStream(bzIn)
            var entry = tarIn.nextTarEntry
            while (entry != null) {
                val newFile = File(modelsDir, entry.name)
                if (entry.isDirectory) {
                    newFile.mkdirs()
                } else {
                    newFile.parentFile?.mkdirs()
                    newFile.outputStream().buffered(65536).use { fos ->
                        tarIn.copyTo(fos)
                    }
                }
                entry = tarIn.nextTarEntry
            }
            tarIn.close()
        }
        
        tarFile.delete()
        File(finalDir, "_SUCCESS").createNewFile()
        return@withContext finalDir.absolutePath
    }

    private var cachedRecognizer: OfflineRecognizer? = null
    private var cachedModelPath: String? = null

    actual suspend fun transcribeAudio(audioData: ByteArray, language: String): String = withContext(Dispatchers.IO) {
        val dirName = getModelDirName(language)
        val finalDir = File(modelsDir, dirName)
        if (!finalDir.exists()) {
            throw Exception("Model not downloaded. Call downloadModel first.")
        }
        
        if (cachedRecognizer == null || cachedModelPath != finalDir.absolutePath) {
            cachedRecognizer?.release()
            
            val encoder = finalDir.listFiles()?.find { (it.name.startsWith("encoder-") || it.name == "encoder.onnx") && it.name.endsWith(".onnx") }?.absolutePath ?: ""
            val decoder = finalDir.listFiles()?.find { (it.name.startsWith("decoder-") || it.name == "decoder.onnx") && it.name.endsWith(".onnx") }?.absolutePath ?: ""
            val joiner = finalDir.listFiles()?.find { (it.name.startsWith("joiner-") || it.name == "joiner.onnx") && it.name.endsWith(".onnx") }?.absolutePath ?: ""
            val model = finalDir.listFiles()?.find { it.name == "model.onnx" }?.absolutePath ?: ""
            val tokens = finalDir.listFiles()?.find { it.name == "tokens.txt" }?.absolutePath ?: ""
            
            val modelConfigBuilder = OfflineModelConfig.builder()
                .setTokens(tokens)
                .setNumThreads(4)
                .setDebug(false)
                
            if (model.isNotEmpty()) {
                val nemoConfig = OfflineNemoEncDecCtcModelConfig.builder()
                    .setModel(model)
                    .build()
                modelConfigBuilder.setNemo(nemoConfig)
                modelConfigBuilder.setModelType("nemo_ctc")
            } else {
                val transducerConfig = OfflineTransducerModelConfig.builder()
                    .setEncoder(encoder)
                    .setDecoder(decoder)
                    .setJoiner(joiner)
                    .build()
                modelConfigBuilder.setTransducer(transducerConfig)
                modelConfigBuilder.setModelType("zipformer")
            }
            
            val modelConfig = modelConfigBuilder.build()
                
            val featConfig = FeatureConfig.builder()
                .setSampleRate(16000)
                .setFeatureDim(80)
                .build()
                
            val config = OfflineRecognizerConfig.builder()
                .setFeatureConfig(featConfig)
                .setOfflineModelConfig(modelConfig)
                .build()
            cachedRecognizer = OfflineRecognizer(config)
            cachedModelPath = finalDir.absolutePath
        }
        
        val recognizer = cachedRecognizer!!
        
        // Strip WAV header if present (44 bytes)
        val startIndex = if (audioData.size > 44 && 
            audioData[0] == 'R'.code.toByte() && 
            audioData[1] == 'I'.code.toByte() && 
            audioData[2] == 'F'.code.toByte() && 
            audioData[3] == 'F'.code.toByte()) {
            44
        } else {
            0
        }
        
        val shortCount = (audioData.size - startIndex) / 2
        val floatData = FloatArray(shortCount)
        val byteBuffer = java.nio.ByteBuffer.wrap(audioData, startIndex, audioData.size - startIndex).order(java.nio.ByteOrder.LITTLE_ENDIAN)
        val shortBuffer = byteBuffer.asShortBuffer()
        for (i in 0 until shortCount) {
            floatData[i] = shortBuffer.get(i).toFloat() / 32768.0f
        }
        
        val stream = recognizer.createStream()
        stream.acceptWaveform(floatData, 16000)
        recognizer.decode(stream)
        val result = recognizer.getResult(stream)
        stream.release()
        
        return@withContext result.text
    }
}
