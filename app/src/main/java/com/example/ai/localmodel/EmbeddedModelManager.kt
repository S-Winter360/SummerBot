package com.example.ai.localmodel

import android.app.ActivityManager
import android.content.Context
import android.os.Build
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
import java.util.concurrent.atomic.AtomicBoolean

interface EmbeddedModelManager {
    val status: StateFlow<EmbeddedModelStatus>
    val diagnostics: StateFlow<EmbeddedModelDiagnostics>
    val downloadProgress: StateFlow<Float>

    suspend fun inspect(): EmbeddedModelStatus
    suspend fun install(): Result<Unit>
    suspend fun cancelDownload()
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

    private val installMutex = Mutex()
    private val isDownloadCancelled = AtomicBoolean(false)

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
        return if (file.exists() && file.isFile && file.length() > 0) {
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

    override suspend fun install(): Result<Unit> = installMutex.withLock {
        isDownloadCancelled.set(false)
        _status.value = EmbeddedModelStatus.CHECKING

        // 1. Compatibility check
        val (compatible, reason) = evaluateDeviceCompatibility()
        if (!compatible) {
            val status = EmbeddedModelStatus.INCOMPATIBLE_DEVICE
            _status.value = status
            updateDiagnostics(status, compatibilityReason = reason, error = reason)
            return Result.failure(IllegalStateException(reason ?: "Device is incompatible"))
        }

        // 2. Storage check
        if (!repository.hasSufficientStorage(metadata.sizeBytes)) {
            val status = EmbeddedModelStatus.INSUFFICIENT_STORAGE
            val msg = "Not enough storage is available to install this local AI model."
            _status.value = status
            updateDiagnostics(status, error = msg)
            return Result.failure(IllegalStateException(msg))
        }

        // 3. Download phase
        _status.value = EmbeddedModelStatus.DOWNLOADING
        _downloadProgress.value = 0f
        updateDiagnostics(EmbeddedModelStatus.DOWNLOADING)

        val downloadResult = repository.downloadModel(
            metadata = metadata,
            downloadUrl = metadata.id.remoteDownloadUrl,
            onProgress = { downloaded, total, progress ->
                _downloadProgress.value = progress
                _diagnostics.update { current ->
                    current.copy(
                        downloadProgress = progress,
                        downloadedBytes = downloaded,
                        totalBytesToDownload = total
                    )
                }
            },
            isCancelled = { isDownloadCancelled.get() }
        )

        return downloadResult.fold(
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
                    updateDiagnostics(EmbeddedModelStatus.CORRUPTED, error = "Integrity check failed")
                    Result.failure(IllegalStateException("Model integrity check failed after download"))
                }
            },
            onFailure = { error ->
                val finalStatus = if (isDownloadCancelled.get()) {
                    EmbeddedModelStatus.CANCELLED
                } else {
                    EmbeddedModelStatus.ERROR
                }
                _status.value = finalStatus
                _downloadProgress.value = 0f
                updateDiagnostics(finalStatus, error = error.message)
                Result.failure(error)
            }
        )
    }

    override suspend fun cancelDownload() {
        isDownloadCancelled.set(true)
        if (_status.value == EmbeddedModelStatus.DOWNLOADING) {
            _status.value = EmbeddedModelStatus.CANCELLED
            _downloadProgress.value = 0f
            updateDiagnostics(EmbeddedModelStatus.CANCELLED)
        }
    }

    override suspend fun verify(): Result<Unit> {
        val file = repository.getModelFile(metadata)
        if (!file.exists() || file.length() == 0L) {
            return Result.failure(IllegalStateException("Model file is missing or empty."))
        }
        return Result.success(Unit)
    }

    override suspend fun deleteInstalledModel(): Result<Unit> = installMutex.withLock {
        val result = repository.deleteModel(metadata)
        _status.value = EmbeddedModelStatus.NOT_INSTALLED
        _downloadProgress.value = 0f
        updateDiagnostics(EmbeddedModelStatus.NOT_INSTALLED)
        result
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
