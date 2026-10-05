package com.example.ai.localmodel

import android.app.ActivityManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.io.InputStream
import java.util.concurrent.atomic.AtomicBoolean

interface EmbeddedModelManager {
    val status: StateFlow<EmbeddedModelStatus>
    val diagnostics: StateFlow<EmbeddedModelDiagnostics>
    val downloadProgress: StateFlow<Float>

    suspend fun inspect(): EmbeddedModelStatus
    suspend fun importModel(uri: Uri): Result<Unit>
    suspend fun importModelStream(
        inputStreamProvider: () -> InputStream?,
        fileNameHint: String? = null,
        totalSizeBytes: Long? = null
    ): Result<Unit>
    suspend fun cancelImport()
    suspend fun cancelDownload() = cancelImport()
    suspend fun install(): Result<Unit> = Result.failure(UnsupportedOperationException("Direct HTTP download is deprecated. Use importModel() to import a local .litertlm file."))
    suspend fun verify(): Result<Unit>
    suspend fun deleteInstalledModel(): Result<Unit>
    fun getInstalledModelPath(): String?
    fun isModelReady(): Boolean
}

class DefaultEmbeddedModelManager(
    private val context: Context,
    private val repository: EmbeddedModelRepository = EmbeddedModelRepository(context),
    val metadata: EmbeddedModelMetadata = EmbeddedModelMetadata(EmbeddedModelId.GEMMA_3_1B_IT),
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) : EmbeddedModelManager {

    companion object {
        private const val TAG = "EmbeddedModelManager"
        private const val MIN_REQUIRED_RAM_BYTES: Long = 2L * 1024L * 1024L * 1024L // 2GB minimum RAM baseline
        const val EXPECTED_EXTENSION = ".litertlm"
    }

    private val _status = MutableStateFlow(EmbeddedModelStatus.NOT_INSTALLED)
    override val status: StateFlow<EmbeddedModelStatus> = _status.asStateFlow()

    private val _diagnostics = MutableStateFlow(
        EmbeddedModelDiagnostics(
            modelId = metadata.id,
            status = EmbeddedModelStatus.NOT_INSTALLED,
            modelName = metadata.modelName,
            approximateSizeFormatted = "584 MB"
        )
    )
    override val diagnostics: StateFlow<EmbeddedModelDiagnostics> = _diagnostics.asStateFlow()

    private val _downloadProgress = MutableStateFlow(0f)
    override val downloadProgress: StateFlow<Float> = _downloadProgress.asStateFlow()

    private val importMutex = Mutex()
    private val isImportCancelled = AtomicBoolean(false)

    init {
        scope.launch {
            inspect()
        }
    }

    override fun isModelReady(): Boolean {
        return _status.value == EmbeddedModelStatus.READY && repository.isModelInstalled(metadata)
    }

    override fun getInstalledModelPath(): String? {
        val file = repository.getModelFile(metadata)
        return if (file.exists() && file.isFile && file.length() >= EmbeddedModelRepository.MIN_VALID_MODEL_BYTES) {
            file.absolutePath
        } else {
            null
        }
    }

    override suspend fun inspect(): EmbeddedModelStatus {
        _status.value = EmbeddedModelStatus.CHECKING
        val (compatible, reason) = evaluateDeviceCompatibility()

        if (!compatible) {
            _status.value = EmbeddedModelStatus.INCOMPATIBLE_DEVICE
            updateDiagnostics(EmbeddedModelStatus.INCOMPATIBLE_DEVICE, compatibilityReason = reason)
            return EmbeddedModelStatus.INCOMPATIBLE_DEVICE
        }

        val isInstalled = repository.isModelInstalled(metadata)
        val newStatus = if (isInstalled) {
            EmbeddedModelStatus.READY
        } else {
            EmbeddedModelStatus.NOT_INSTALLED
        }

        _status.value = newStatus
        updateDiagnostics(newStatus)
        return newStatus
    }

    override suspend fun importModel(uri: Uri): Result<Unit> {
        val fileName = getFileNameFromUri(uri)
        val fileSize = getFileSizeFromUri(uri)
        return importModelStream(
            inputStreamProvider = { context.contentResolver.openInputStream(uri) },
            fileNameHint = fileName,
            totalSizeBytes = fileSize
        )
    }

    override suspend fun importModelStream(
        inputStreamProvider: () -> InputStream?,
        fileNameHint: String?,
        totalSizeBytes: Long?
    ): Result<Unit> = importMutex.withLock {
        isImportCancelled.set(false)
        _status.value = EmbeddedModelStatus.CHECKING

        // 1. File extension validation
        if (fileNameHint != null && !fileNameHint.endsWith(EXPECTED_EXTENSION, ignoreCase = true)) {
            val status = EmbeddedModelStatus.CORRUPTED
            val errorMsg = "The selected file is not a valid LiteRT-LM model."
            _status.value = status
            updateDiagnostics(status, error = errorMsg)
            return Result.failure(IllegalArgumentException("Invalid model format. Only .litertlm files are supported."))
        }

        // 2. Compatibility check
        val (compatible, reason) = evaluateDeviceCompatibility()
        if (!compatible) {
            val status = EmbeddedModelStatus.INCOMPATIBLE_DEVICE
            _status.value = status
            updateDiagnostics(status, compatibilityReason = reason, error = reason)
            return Result.failure(IllegalStateException(reason ?: "Device is incompatible"))
        }

        // 3. Storage check
        val requiredBytes = totalSizeBytes ?: metadata.sizeBytes
        if (requiredBytes > 0 && !repository.hasSufficientStorage(requiredBytes)) {
            val status = EmbeddedModelStatus.INSUFFICIENT_STORAGE
            val msg = "Not enough storage is available to install this local AI model."
            _status.value = status
            updateDiagnostics(status, error = msg)
            return Result.failure(IllegalStateException(msg))
        }

        // 4. Import / Streaming phase
        _status.value = EmbeddedModelStatus.DOWNLOADING
        _downloadProgress.value = 0f
        updateDiagnostics(EmbeddedModelStatus.DOWNLOADING)

        val importResult = repository.importModelFromStream(
            metadata = metadata,
            inputStreamProvider = inputStreamProvider,
            totalSizeBytes = totalSizeBytes,
            onProgress = { imported, total, progress ->
                _downloadProgress.value = progress
                _diagnostics.update { current ->
                    current.copy(
                        downloadProgress = progress,
                        downloadedBytes = imported,
                        totalBytesToDownload = total
                    )
                }
            },
            isCancelled = { isImportCancelled.get() }
        )

        return importResult.fold(
            onSuccess = { file ->
                _status.value = EmbeddedModelStatus.VERIFYING
                updateDiagnostics(EmbeddedModelStatus.VERIFYING)

                val verifyResult = verify()
                if (verifyResult.isSuccess) {
                    _status.value = EmbeddedModelStatus.READY
                    _downloadProgress.value = 1f
                    updateDiagnostics(EmbeddedModelStatus.READY, localPath = file.absolutePath)
                    Result.success(Unit)
                } else {
                    _status.value = EmbeddedModelStatus.CORRUPTED
                    val errorMsg = "The selected file is not a valid LiteRT-LM model."
                    updateDiagnostics(EmbeddedModelStatus.CORRUPTED, error = errorMsg)
                    Result.failure(IllegalStateException(errorMsg))
                }
            },
            onFailure = { error ->
                val finalStatus = if (isImportCancelled.get()) {
                    EmbeddedModelStatus.CANCELLED
                } else {
                    EmbeddedModelStatus.CORRUPTED
                }
                _status.value = finalStatus
                _downloadProgress.value = 0f
                val userVisibleError = if (isImportCancelled.get()) "Import cancelled." else "The selected file is not a valid LiteRT-LM model."
                updateDiagnostics(finalStatus, error = userVisibleError)
                Result.failure(error)
            }
        )
    }

    override suspend fun cancelImport() {
        isImportCancelled.set(true)
        if (_status.value == EmbeddedModelStatus.DOWNLOADING || _status.value == EmbeddedModelStatus.CHECKING) {
            _status.value = EmbeddedModelStatus.CANCELLED
            _downloadProgress.value = 0f
            updateDiagnostics(EmbeddedModelStatus.CANCELLED)
        }
    }

    override suspend fun verify(): Result<Unit> {
        val file = repository.getModelFile(metadata)
        if (!file.exists() || file.length() < EmbeddedModelRepository.MIN_VALID_MODEL_BYTES) {
            return Result.failure(IllegalStateException("The selected file is not a valid LiteRT-LM model."))
        }
        return Result.success(Unit)
    }

    override suspend fun deleteInstalledModel(): Result<Unit> = importMutex.withLock {
        val result = repository.deleteModel(metadata)
        _status.value = EmbeddedModelStatus.NOT_INSTALLED
        _downloadProgress.value = 0f
        updateDiagnostics(EmbeddedModelStatus.NOT_INSTALLED)
        result
    }

    private fun getFileNameFromUri(uri: Uri): String? {
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            return cursor.getString(nameIndex)
                        }
                    }
                }
            } catch (_: Throwable) {}
        }
        return uri.lastPathSegment
    }

    private fun getFileSizeFromUri(uri: Uri): Long? {
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (sizeIndex != -1) {
                            val size = cursor.getLong(sizeIndex)
                            if (size > 0) return size
                        }
                    }
                }
            } catch (_: Throwable) {}
        }
        return null
    }

    private fun evaluateDeviceCompatibility(): Pair<Boolean, String?> {
        val totalRam = getTotalDeviceRam()
        if (totalRam > 0 && totalRam < MIN_REQUIRED_RAM_BYTES) {
            return Pair(false, "Device RAM (${totalRam / (1024 * 1024)} MB) is below minimum required 2 GB")
        }

        // Check supported ABIs (LiteRT-LM supports arm64-v8a, x86_64, armeabi-v7a)
        val abis = Build.SUPPORTED_ABIS ?: emptyArray()
        val hasSupportedAbi = abis.any { it.contains("arm") || it.contains("x86") }
        if (!hasSupportedAbi) {
            return Pair(false, "Unsupported CPU architecture: ${abis.joinToString()}")
        }

        return Pair(true, null)
    }

    private fun getTotalDeviceRam(): Long {
        return try {
            val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            actManager?.getMemoryInfo(memInfo)
            memInfo.totalMem
        } catch (_: Throwable) {
            0L
        }
    }

    private fun updateDiagnostics(
        status: EmbeddedModelStatus,
        compatibilityReason: String? = null,
        error: String? = null,
        localPath: String? = null
    ) {
        val installedSize = repository.getInstalledSizeBytes(metadata)
        val availableStorage = repository.getAvailableStorageBytes()
        val totalRam = getTotalDeviceRam()
        val path = localPath ?: getInstalledModelPath()

        _diagnostics.update {
            it.copy(
                status = status,
                installedSizeBytes = installedSize,
                availableStorageBytes = availableStorage,
                totalDeviceRamBytes = totalRam,
                compatibilityReason = compatibilityReason,
                lastError = error,
                localModelFilePath = path
            )
        }
    }
}
