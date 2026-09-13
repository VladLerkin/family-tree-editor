package com.family.tree.core.ai

import io.ktor.client.HttpClient
import io.ktor.client.request.prepareGet
import io.ktor.client.request.header
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
            
            if (fileWriter.exists(absolutePath)) {
                emit(DownloadStatus.Progress(1.0f))
                emit(DownloadStatus.Finished(absolutePath))
                return@flow
            }
            
            val tmpPath = "$absolutePath.tmp"
            var existingLength = 0L
            if (fileWriter.exists(tmpPath)) {
                existingLength = fileWriter.length(tmpPath)
            }
            
            httpClient.prepareGet(url) {
                if (existingLength > 0L) {
                    header(io.ktor.http.HttpHeaders.Range, "bytes=$existingLength-")
                }
                timeout {
                    requestTimeoutMillis = Long.MAX_VALUE
                    socketTimeoutMillis = Long.MAX_VALUE
                }
            }.execute { response ->
                val status = response.status.value
                if (status != 200 && status != 206 && status != 416) {
                    throw RuntimeException("HTTP Error $status: ${response.status.description}")
                }
                
                if (status == 416) {
                    // Range Not Satisfiable typically means we already downloaded it all.
                    fileWriter.rename(tmpPath, absolutePath)
                    emit(DownloadStatus.Progress(1.0f))
                    emit(DownloadStatus.Finished(absolutePath))
                    return@execute
                }
                
                val append = (status == 206)
                if (!append && existingLength > 0L) {
                    fileWriter.writeChunk(tmpPath, ByteArray(0), append = false)
                    existingLength = 0L
                } else if (!append) {
                    fileWriter.writeChunk(tmpPath, ByteArray(0), append = false)
                }
                
                val contentLengthHeader = response.headers[io.ktor.http.HttpHeaders.ContentLength]?.toLong() ?: 0L
                val totalLength = if (append) existingLength + contentLengthHeader else contentLengthHeader
                
                val channel = response.bodyAsChannel()
                var bytesCopied = existingLength
                
                val buffer = ByteArray(8192)
                while (!channel.isClosedForRead) {
                    val read = channel.readAvailable(buffer, 0, buffer.size)
                    if (read > 0) {
                        val bytes = if (read == buffer.size) buffer else buffer.copyOf(read)
                        fileWriter.writeChunk(tmpPath, bytes, append = true)
                        bytesCopied += read
                        
                        if (totalLength > 0L) {
                            emit(DownloadStatus.Progress(bytesCopied.toFloat() / totalLength))
                        }
                    } else if (read < 0) {
                        break
                    }
                }
                
                if (totalLength > 0L && bytesCopied < totalLength) {
                    throw RuntimeException("Download interrupted. Expected $totalLength bytes, got $bytesCopied bytes.")
                }
                
                fileWriter.rename(tmpPath, absolutePath)
                emit(DownloadStatus.Finished(absolutePath))
            }
        } catch (e: Exception) {
            println("[DEBUG_LOG] LocalModelManager error: ${e.message}")
            e.printStackTrace()
            emit(DownloadStatus.Error(e))
        }
    }
    fun isModelDownloaded(fileName: String): Boolean {
        val dirPath = directoryProvider.getDirectory()
        val absolutePath = "$dirPath/$fileName"
        return ModelFileWriter().exists(absolutePath)
    }

    fun deleteModel(fileName: String) {
        val dirPath = directoryProvider.getDirectory()
        val absolutePath = "$dirPath/$fileName"
        ModelFileWriter().delete(absolutePath)
    }
}

sealed class DownloadStatus {
    data class Progress(val progress: Float) : DownloadStatus()
    data class Finished(val absolutePath: String) : DownloadStatus()
    data class Error(val exception: Exception) : DownloadStatus()
}
