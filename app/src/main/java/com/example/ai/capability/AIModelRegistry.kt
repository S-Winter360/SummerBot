package com.example.ai.capability

import com.example.ai.AIEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class AIModelRegistry {

    private val providers = mutableMapOf<AIProviderType, AIEngine>()
    private val _metadataMap = MutableStateFlow<Map<AIProviderType, AIModelMetadata>>(emptyMap())
    val allMetadata: StateFlow<Map<AIProviderType, AIModelMetadata>> = _metadataMap.asStateFlow()

    fun register(
        providerType: AIProviderType,
        engine: AIEngine,
        metadata: AIModelMetadata
    ) {
        synchronized(providers) {
            providers[providerType] = engine
        }
        _metadataMap.update { current ->
            current + (providerType to metadata)
        }
    }

    fun updateAvailability(
        providerType: AIProviderType,
        status: AIAvailabilityStatus
    ) {
        val engine = synchronized(providers) { providers[providerType] }
        when (engine) {
            is OnDeviceGenAIProvider -> engine.updateAvailability(status)
            is OnDeviceGeminiNanoAIEngine -> engine.updateAvailability(status)
        }
        _metadataMap.update { current ->
            val existing = current[providerType]
            if (existing != null) {
                current + (providerType to existing.copy(
                    availabilityStatus = status,
                    lastCheckedTimestamp = System.currentTimeMillis()
                ))
            } else {
                current
            }
        }
    }

    fun getEngine(providerType: AIProviderType): AIEngine? {
        return synchronized(providers) {
            providers[providerType]
        }
    }

    fun getMetadata(providerType: AIProviderType): AIModelMetadata? {
        return _metadataMap.value[providerType]
    }

    fun getAvailableModels(): List<AIModelMetadata> {
        return _metadataMap.value.values.filter { it.availabilityStatus == AIAvailabilityStatus.AVAILABLE }
    }

    fun getProvidersForCapability(capability: AICapability): List<AIProviderType> {
        return _metadataMap.value.entries
            .filter { capability in it.value.supportedCapabilities }
            .map { it.key }
    }

    fun getPreferredModelForCapability(capability: AICapability): AIModelMetadata? {
        val priorityOrder = listOf(
            AIProviderType.ON_DEVICE_GENAI,
            AIProviderType.EMBEDDED_LOCAL_MODEL,
            AIProviderType.DETERMINISTIC_LOCAL
        )

        for (provider in priorityOrder) {
            val meta = _metadataMap.value[provider]
            if (meta != null && meta.availabilityStatus == AIAvailabilityStatus.AVAILABLE && capability in meta.supportedCapabilities) {
                return meta
            }
        }

        return _metadataMap.value[AIProviderType.DETERMINISTIC_LOCAL]
    }
}
