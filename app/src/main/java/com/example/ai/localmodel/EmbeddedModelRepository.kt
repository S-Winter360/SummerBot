package com.example.ai.localmodel

import android.content.Context
import android.os.StatFs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest

/**
 * Storage and file I/O repository for embedded local AI models.
 * Strictly maintains model artifacts in application-private storage under "embedded_models/".
 * Streams imported model files to temporary ".partial" files before atomically moving them
 * to prevent corrupted or partial installations from breaking working models.
 */
class EmbeddedModelRepository(
    private val context: Context,
    private val subDirName: String = "embedded_models"
) {
    companion object {
        private const val TAG = "EmbeddedModelRepo"
        // 200 MB safety headroom beyond model size
        const val STORAGE_SAFETY_MARGIN_BYTES: Long = 200L * 1024L * 1024L
        const val MIN_VALID_MODEL_BYTES: Long = 16L
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
        return file.exists() && file.isFile && file.length() >= MIN_VALID_MODEL_BYTES
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
     * Verifies whether there is sufficient storage to import and store the model,
     * including safety margin.
     */
    fun hasSufficientStorage(requiredBytes: Long): Boolean {
        val available = getAvailableStorageBytes()
        return available >= (requiredBytes + STORAGE_SAFETY_MARGIN_BYTES)
    }

    /**
     * Streams model bytes from a stream provider into a private temporary file (.partial).
     * Validates size and integrity before atomically swapping with the target file.
     * If replacement fails, the previously installed model is retained intact.
     */
    suspend fun importModelFromStream(
        metadata: EmbeddedModelMetadata,
        inputStreamProvider: () -> InputStream?,
        totalSizeBytes: Long? = null,
        onProgress: (importedBytes: Long, totalBytes: Long, progressFraction: Float) -> Unit,
        isCancelled: () -> Boolean
    ): Result<File> = withContext(Dispatchers.IO) {
        val targetFile = getModelFile(metadata)
        val partialFile = File(modelsDir, "${metadata.filename}.partial")
        val backupFile = File(modelsDir, "${metadata.filename}.backup")

        try {
            if (partialFile.exists()) {
                partialFile.delete()
            }
            if (backupFile.exists()) {
                backupFile.delete()
            }

            val expectedSize = totalSizeBytes ?: metadata.sizeBytes
            if (expectedSize > 0 && !hasSufficientStorage(expectedSize)) {
                return@withContext Result.failure(
                    IllegalStateException("Not enough storage is available to install this local AI model.")
                )
            }

            val inputStream = inputStreamProvider() ?: return@withContext Result.failure(
                IllegalArgumentException("Unable to open stream from selected model file.")
            )

            var importedBytes = 0L

            inputStream.use { input ->
                FileOutputStream(partialFile).use { output ->
                    val buffer = ByteArray(64 * 1024) // 64 KB buffer
                    var bytesRead: Int

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        if (isCancelled()) {
                            output.flush()
                            partialFile.delete()
                            return@withContext Result.failure(
                                IllegalStateException("Import cancelled by user.")
                            )
                        }

                        output.write(buffer, 0, bytesRead)
                        importedBytes += bytesRead

                        val progressFraction = if (expectedSize > 0) {
                            (importedBytes.toFloat() / expectedSize.toFloat()).coerceIn(0f, 1f)
                        } else {
                            0f
                        }
                        onProgress(importedBytes, expectedSize, progressFraction)
                    }
                    output.flush()
                }
            }

            if (isCancelled()) {
                partialFile.delete()
                return@withContext Result.failure(IllegalStateException("Import cancelled by user."))
            }

            // Verify size
            if (partialFile.length() < MIN_VALID_MODEL_BYTES) {
                partialFile.delete()
                return@withContext Result.failure(
                    IllegalStateException("The selected file is not a valid LiteRT-LM model.")
                )
            }

            // Checksum verification if metadata specifies one
            if (!metadata.sha256Checksum.isNullOrBlank()) {
                val computedHash = computeSha256(partialFile)
                if (!computedHash.equals(metadata.sha256Checksum, ignoreCase = true)) {
                    partialFile.delete()
                    return@withContext Result.failure(
                        IllegalStateException("Model checksum mismatch. Installation aborted.")
                    )
                }
            }

            // Safe Atomic Replacement
            if (targetFile.exists()) {
                targetFile.renameTo(backupFile)
            }

            val renameSuccess = partialFile.renameTo(targetFile)
            if (renameSuccess) {
                if (backupFile.exists()) {
                    backupFile.delete()
                }
                Result.success(targetFile)
            } else {
                // Restore backup if rename failed
                if (backupFile.exists()) {
                    backupFile.renameTo(targetFile)
                }
                partialFile.delete()
                Result.failure(IllegalStateException("Failed to move imported model into target destination."))
            }
        } catch (t: Throwable) {
            if (partialFile.exists()) {
                partialFile.delete()
            }
            if (backupFile.exists() && !targetFile.exists()) {
                backupFile.renameTo(targetFile)
            }
            Result.failure(t)
        }
    }

    /**
     * Deletes the installed model file and any leftover temporary or backup files.
     */
    suspend fun deleteModel(metadata: EmbeddedModelMetadata): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val targetFile = getModelFile(metadata)
            val partialFile = File(modelsDir, "${metadata.filename}.partial")
            val backupFile = File(modelsDir, "${metadata.filename}.backup")

            if (partialFile.exists()) {
                partialFile.delete()
            }
            if (backupFile.exists()) {
                backupFile.delete()
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
