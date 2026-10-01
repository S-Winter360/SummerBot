package com.example

import com.example.actions.ActionRequest
import com.example.actions.ActionResult
import com.example.actions.SecuredActionExecutor
import com.example.ai.OfflineLocalAIEngine
import com.example.core.decision.DefaultSummerDecisionEngine
import com.example.core.event.SummerEventBus
import com.example.core.interaction.InteractionState
import com.example.core.orchestrator.SummerOrchestrator
import com.example.core.personality.SummerPersonality
import com.example.core.response.ResponseType
import com.example.core.session.SummerSessionManager
import com.example.core.state.SummerState
import com.example.core.state.SummerStateManager
import com.example.memory.MemoryRepository
import com.example.memory.models.MemoryCategory
import com.example.memory.models.MemoryRecord
import com.example.memory.models.SummerSettings
import com.example.network.NetworkInformationProvider
import com.example.network.NetworkState
import com.example.security.Capability
import com.example.security.DefaultActionAuthorizationPolicy
import com.example.security.SecurityContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SummerOrchestratorTest {

    private class FakeMemoryRepository : MemoryRepository {
        val memories = mutableListOf<MemoryRecord>()
        var currentSettings = SummerSettings()

        private val _settingsFlow = MutableStateFlow(currentSettings)
        private val _memoriesFlow = MutableStateFlow<List<MemoryRecord>>(emptyList())

        override fun observeSettings(): Flow<SummerSettings> = _settingsFlow
        override suspend fun getSettings(): SummerSettings = currentSettings
        override suspend fun updateSettings(settings: SummerSettings) {
            currentSettings = settings
            _settingsFlow.value = settings
        }

        override suspend fun recordMemory(record: MemoryRecord) {
            memories.add(record)
            _memoriesFlow.value = memories.toList()
        }

        override fun observeMemories(): Flow<List<MemoryRecord>> = _memoriesFlow
        override fun observeMemoriesByCategory(category: MemoryCategory): Flow<List<MemoryRecord>> =
            _memoriesFlow.map { list -> list.filter { it.category == category } }

        override suspend fun deleteMemory(id: String) {
            memories.removeAll { it.id == id }
            _memoriesFlow.value = memories.toList()
        }

        override suspend fun updateMemory(record: MemoryRecord) {
            val index = memories.indexOfFirst { it.id == record.id }
            if (index >= 0) {
                memories[index] = record
            } else {
                memories.add(record)
            }
            _memoriesFlow.value = memories.toList()
        }

        override suspend fun getActiveMemories(): List<MemoryRecord> = memories.filter { it.isActive }

        override suspend fun forgetMemory(targetId: String?, keyword: String?): Boolean {
            if (targetId != null) {
                val removed = memories.removeAll { it.id == targetId }
                _memoriesFlow.value = memories.toList()
                return removed
            }
            if (keyword != null) {
                val removed = memories.removeAll { it.content.contains(keyword, ignoreCase = true) }
                _memoriesFlow.value = memories.toList()
                return removed
            }
            return false
        }

        override suspend fun clearAllMemories() {
            memories.clear()
            _memoriesFlow.value = emptyList()
        }

        override suspend fun updateAccessMetadata(id: String, count: Int, timestamp: Long) {
            val index = memories.indexOfFirst { it.id == id }
            if (index >= 0) {
                memories[index] = memories[index].copy(accessCount = count, lastAccessedAt = timestamp)
                _memoriesFlow.value = memories.toList()
            }
        }
    }

    private class FakeNetworkProvider : NetworkInformationProvider {
        private val _state = MutableStateFlow(NetworkState.CONNECTED_WIFI)
        override val networkState: Flow<NetworkState> = _state
        override val isOnline: Flow<Boolean> = _state.map { it != NetworkState.OFFLINE }
        override fun getCurrentState(): NetworkState = _state.value
    }

    private fun createOrchestrator(
        memoryRepo: FakeMemoryRepository = FakeMemoryRepository()
    ): Pair<SummerOrchestrator, FakeMemoryRepository> {
        val stateManager = SummerStateManager()
        val sessionManager = SummerSessionManager()
        val eventBus = SummerEventBus()
        val personality = SummerPersonality.DEFAULT
        val aiEngine = OfflineLocalAIEngine(personality)
        val decisionEngine = DefaultSummerDecisionEngine()
        val policy = DefaultActionAuthorizationPolicy()
        val actionExecutor = SecuredActionExecutor(policy)
        val networkProvider = FakeNetworkProvider()

        val orchestrator = SummerOrchestrator(
            stateManager = stateManager,
            sessionManager = sessionManager,
            eventBus = eventBus,
            aiEngine = aiEngine,
            decisionEngine = decisionEngine,
            authorizationPolicy = policy,
            actionExecutor = actionExecutor,
            memoryRepository = memoryRepo,
            networkProvider = networkProvider
        )

        return Pair(orchestrator, memoryRepo)
    }

    @Test
    fun testGreetingInteraction() = runTest {
        val (orchestrator, _) = createOrchestrator()

        val result = orchestrator.handleUserInput("Hello Summer")
        assertEquals(InteractionState.COMPLETED, result.state)
        assertNotNull(result.response)
        assertTrue(result.response!!.text.contains("Summer"))
        assertEquals(SummerState.Idle, orchestrator.orchestratorState.value)
    }

    @Test
    fun testIdentityInteraction() = runTest {
        val (orchestrator, _) = createOrchestrator()

        val result = orchestrator.handleUserInput("Who are you?")
        assertEquals(InteractionState.COMPLETED, result.state)
        assertNotNull(result.response)
        assertTrue(result.response!!.text.contains("Summer Winter"))
    }

    @Test
    fun testCapabilitiesInteraction() = runTest {
        val (orchestrator, _) = createOrchestrator()

        val result = orchestrator.handleUserInput("What can you do?")
        assertEquals(InteractionState.COMPLETED, result.state)
        assertNotNull(result.response)
        assertTrue(result.response!!.text.contains("offline", ignoreCase = true))
    }

    @Test
    fun testTimeInteraction() = runTest {
        val (orchestrator, _) = createOrchestrator()

        val result = orchestrator.handleUserInput("What time is it?")
        assertEquals(InteractionState.COMPLETED, result.state)
        assertNotNull(result.response)
        assertTrue(result.response!!.text.contains("time", ignoreCase = true))
    }

    @Test
    fun testCapabilityActionExecutionNetwork() = runTest {
        val (orchestrator, _) = createOrchestrator()

        val result = orchestrator.handleUserInput("test network")
        assertEquals(InteractionState.COMPLETED, result.state)
        assertNotNull(result.response)
        assertTrue(result.response!!.text.contains("Execution outcome"))
    }

    @Test
    fun testCapabilityActionExecutionMic() = runTest {
        val (orchestrator, _) = createOrchestrator()

        val result = orchestrator.handleUserInput("test mic")
        assertEquals(InteractionState.COMPLETED, result.state)
        assertNotNull(result.response)
        assertTrue(result.response!!.text.contains("Execution outcome"))
    }

    @Test
    fun testMemoryRecordingOnFact() = runTest {
        val (orchestrator, memoryRepo) = createOrchestrator()

        val result = orchestrator.handleUserInput("remember I like dark mode")
        assertEquals(InteractionState.COMPLETED, result.state)
        assertTrue(memoryRepo.memories.any { it.content.contains("I like dark mode") })
    }

    @Test
    fun testUnknownQueryFallback() = runTest {
        val (orchestrator, _) = createOrchestrator()

        val result = orchestrator.handleUserInput("xyzzy gibberish query 12345")
        assertEquals(InteractionState.COMPLETED, result.state)
        assertNotNull(result.response)
        assertEquals(ResponseType.TEXT, result.response!!.type)
        assertTrue(result.response!!.text.isNotBlank())
    }
}
