package com.family.tree.core.ai

import io.ktor.client.HttpClient
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import io.ktor.utils.io.readAvailable
import io.ktor.client.plugins.timeout
import io.ktor.client.plugins.HttpTimeout
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class LocalModelManager(
    private val httpClient: HttpClient,
    private val directoryProvider: ModelDirectoryProvider
) {
    /**
     * Downloads a model file from the specified URL to the local directory.
     * Emits the download progress as a Float between 0.0 and 1.0.
     * Finally emits the absolute path to the downloaded file.
     */
    suspend fun downloadModel(url: String, fileName: String): Flow<DownloadStatus> = flow {
        val dirPath = directoryProvider.getDirectory()
        val absolutePath = "$dirPath/$fileName"
        
        try {
            val fileWriter = ModelFileWriter()
            
            // If already downloaded (e.g. exists and is not 0 bytes, but we just check exists for simplicity here)
            // Wait, for simplicity, if exists we just return finished immediately
            if (fileWriter.exists(absolutePath)) {
                emit(DownloadStatus.Progress(1.0f))
                emit(DownloadStatus.Finished(absolutePath))
                return@flow
            }
            
            httpClient.prepareGet(url) {
                timeout {
                    requestTimeoutMillis = Long.MAX_VALUE
                    socketTimeoutMillis = Long.MAX_VALUE
                }
            }.execute { response ->
                if (!response.status.isSuccess()) {
                    throw RuntimeException("HTTP Error ${response.status.value}: ${response.status.description}")
                }
                
                val channel = response.bodyAsChannel()
                val contentLength = response.headers[io.ktor.http.HttpHeaders.ContentLength]?.toLong() ?: 0L
                var bytesCopied = 0L
                
                // Truncate/create file
                fileWriter.writeChunk(absolutePath, ByteArray(0), append = false)
                
                val buffer = ByteArray(8192)
                while (!channel.isClosedForRead) {
                    val read = channel.readAvailable(buffer, 0, buffer.size)
                    if (read > 0) {
                        val bytes = if (read == buffer.size) buffer else buffer.copyOf(read)
                        fileWriter.writeChunk(absolutePath, bytes, append = true)
                        bytesCopied += read
                        
                        if (contentLength > 0L) {
                            emit(DownloadStatus.Progress(bytesCopied.toFloat() / contentLength))
                        }
                    } else if (read < 0) {
                        break
                    }
                }
                
                emit(DownloadStatus.Finished(absolutePath))
            }
        } catch (e: Exception) {
            println("[DEBUG_LOG] LocalModelManager error: ${e.message}")
            e.printStackTrace()
            emit(DownloadStatus.Error(e))
        }
    }
}

sealed class DownloadStatus {
    data class Progress(val progress: Float) : DownloadStatus()
    data class Finished(val absolutePath: String) : DownloadStatus()
    data class Error(val exception: Exception) : DownloadStatus()
}
