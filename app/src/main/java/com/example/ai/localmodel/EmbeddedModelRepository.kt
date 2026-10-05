package com.example.ai.localmodel

import android.content.Context
import android.os.Environment
import android.os.StatFs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * Storage and file I/O repository for embedded local AI models.
 * Strictly maintains model artifacts in application-private storage under "embedded_models/".
 * Streams downloads to temporary files before atomically moving them to prevent corrupted/incomplete states.
 */
class EmbeddedModelRepository(
    private val context: Context,
    private val subDirName: String = "embedded_models"
) {
    companion object {
        private const val TAG = "EmbeddedModelRepo"
        // 200 MB safety headroom beyond model size
        const val STORAGE_SAFETY_MARGIN_BYTES: Long = 200L * 1024L * 1024L
    }

    private val modelsDir: File by lazy {
        val dir = File(context.filesDir, subDirName)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        dir
    }

    /**
     * Returns the target file destination for a given model metadata.
     */
    fun getModelFile(metadata: EmbeddedModelMetadata): File {
        return File(modelsDir, metadata.filename)
    }

    /**
     * Checks if a valid, non-empty model file exists on disk.
     */
    fun isModelInstalled(metadata: EmbeddedModelMetadata): Boolean {
        val file = getModelFile(metadata)
        return file.exists() && file.isFile && file.length() > 0
    }

    /**
     * Returns the installed size in bytes, or 0 if missing.
     */
    fun getInstalledSizeBytes(metadata: EmbeddedModelMetadata): Long {
        val file = getModelFile(metadata)
        return if (file.exists() && file.isFile) file.length() else 0L
    }

    /**
     * Calculates free storage space on the internal files directory volume in bytes.
     */
    fun getAvailableStorageBytes(): Long {
        return try {
            val stat = StatFs(context.filesDir.absolutePath)
            val bytes = stat.availableBlocksLong * stat.blockSizeLong
            if (bytes > 0L) bytes else context.filesDir.usableSpace
        } catch (_: Throwable) {
            try { context.filesDir.usableSpace } catch (_: Throwable) { 0L }
        }
    }

    /**
     * Verifies whether there is sufficient storage to download and install the model,
     * including safety margin.
     */
    fun hasSufficientStorage(requiredBytes: Long): Boolean {
        val available = getAvailableStorageBytes()
        return available >= (requiredBytes + STORAGE_SAFETY_MARGIN_BYTES)
    }

    /**
     * Streams model bytes from a remote URL to a temporary file, reporting progress.
     * Atomically moves to final filename only upon successful completion.
     */
    suspend fun downloadModel(
        metadata: EmbeddedModelMetadata,
        downloadUrl: String,
        onProgress: (downloadedBytes: Long, totalBytes: Long, progressFraction: Float) -> Unit,
        isCancelled: () -> Boolean
    ): Result<File> = withContext(Dispatchers.IO) {
        val targetFile = getModelFile(metadata)
        val tempFile = File(modelsDir, "${metadata.filename}.tmp")

        try {
            if (tempFile.exists()) {
                tempFile.delete()
            }

            if (!hasSufficientStorage(metadata.sizeBytes)) {
                return@withContext Result.failure(
                    IllegalStateException("Not enough storage is available to install this local AI model.")
                )
            }

            val url = URL(downloadUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 30_000
                readTimeout = 60_000
                instanceFollowRedirects = true
                requestMethod = "GET"
            }

            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                return@withContext Result.failure(
                    IllegalStateException("HTTP download failed with response code $responseCode")
                )
            }

            val totalBytes = if (connection.contentLengthLong > 0) connection.contentLengthLong else metadata.sizeBytes
            var downloadedBytes = 0L

            connection.inputStream.use { input: InputStream ->
                FileOutputStream(tempFile).use { output: FileOutputStream ->
                    val buffer = ByteArray(64 * 1024) // 64 KB buffer
                    var bytesRead: Int

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        if (isCancelled()) {
                            output.flush()
                            tempFile.delete()
                            return@withContext Result.failure(
                                IllegalStateException("Download cancelled by user.")
                            )
                        }

                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead

                        val progressFraction = if (totalBytes > 0) {
                            (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                        } else {
                            0f
                        }
                        onProgress(downloadedBytes, totalBytes, progressFraction)
                    }
                    output.flush()
                }
            }

            if (isCancelled()) {
                tempFile.delete()
                return@withContext Result.failure(IllegalStateException("Download cancelled by user."))
            }

            // Verify size
            if (tempFile.length() == 0L) {
                tempFile.delete()
                return@withContext Result.failure(IllegalStateException("Downloaded file is empty."))
            }

            // Checksum verification if available
            if (!metadata.sha256Checksum.isNullOrBlank()) {
                val computedHash = computeSha256(tempFile)
                if (!computedHash.equals(metadata.sha256Checksum, ignoreCase = true)) {
                    tempFile.delete()
                    return@withContext Result.failure(
                        IllegalStateException("Model checksum mismatch. Installation aborted.")
                    )
                }
            }

            // Atomic rename / move to destination
            if (targetFile.exists()) {
                targetFile.delete()
            }

            val success = tempFile.renameTo(targetFile)
            if (!success) {
                // Fallback copy & delete
                tempFile.copyTo(targetFile, overwrite = true)
                tempFile.delete()
            }

            Result.success(targetFile)
        } catch (t: Throwable) {
            if (tempFile.exists()) {
                tempFile.delete()
            }
            Result.failure(t)
        }
    }

    /**
     * Deletes the installed model file and any leftover temp files.
     */
    suspend fun deleteModel(metadata: EmbeddedModelMetadata): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val targetFile = getModelFile(metadata)
            val tempFile = File(modelsDir, "${metadata.filename}.tmp")

            if (tempFile.exists()) {
                tempFile.delete()
            }
            if (targetFile.exists()) {
                targetFile.delete()
            }
            Result.success(Unit)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    private fun computeSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { stream ->
            val buffer = ByteArray(64 * 1024)
            var read: Int
            while (stream.read(buffer).also { read = it } != -1) {
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
