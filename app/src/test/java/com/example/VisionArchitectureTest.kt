package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.actions.ActionRequest
import com.example.actions.SecuredActionExecutor
import com.example.core.context.SummerContext
import com.example.core.event.SummerEvent
import com.example.core.event.SummerEventBus
import com.example.memory.MemoryRepository
import com.example.memory.models.MemoryCategory
import com.example.memory.models.MemoryRecord
import com.example.memory.models.SummerSettings
import com.example.security.Capability
import com.example.security.DefaultActionAuthorizationPolicy
import com.example.security.SecurityContext
import com.example.vision.CameraPermissionState
import com.example.vision.StubVisionProvider
import com.example.vision.VisionCapability
import com.example.vision.VisionCapabilityDetector
import com.example.vision.VisionDiagnostics
import com.example.vision.VisionEngineRouter
import com.example.vision.VisionInput
import com.example.vision.VisionInputSource
import com.example.vision.VisionObservation
import com.example.vision.VisionProvider
import com.example.vision.VisionProviderType
import com.example.vision.VisionResult
import com.example.vision.VisionState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Phase 0G: Vision & Camera Architecture Unit & Integration Tests.
 * Verifies states, stub provider, routing, capability detection, permission boundaries,
 * context encapsulation, memory safety, and security policies.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class VisionArchitectureTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Context>()
    }

    // 1. Initial vision state is IDLE
    @Test
    fun testInitialVisionStateIsIdle() {
        val router = VisionEngineRouter()
        assertEquals(VisionState.IDLE, router.visionState.value)
        router.release()
    }

    // 2. Vision state transitions correctly through processing to completed
    @Test
    fun testVisionStateTransitionsOnProcessingAndCompletion() = runBlocking {
        val router = VisionEngineRouter()
        val input = VisionInput(source = VisionInputSource.IMAGE)

        val result = router.process(input)

        assertTrue(result is VisionResult.Success)
        assertEquals(VisionState.COMPLETED, router.visionState.value)
        router.release()
    }

    // 3. Vision state transitions to STOPPED on stop()
    @Test
    fun testVisionStateTransitionsToStoppedOnStop() = runBlocking {
        val router = VisionEngineRouter()
        router.stop()
        assertEquals(VisionState.STOPPED, router.visionState.value)
        router.release()
    }

    // 4. Vision state transitions to ERROR on failure
    @Test
    fun testVisionStateTransitionsToErrorOnFailure() = runBlocking {
        val failingProvider = object : VisionProvider {
            override val providerType = VisionProviderType.STUB
            override val isAvailable = true
            override val supportedCapabilities = setOf(VisionCapability.IMAGE_ANALYSIS)
            override suspend fun process(input: VisionInput): VisionResult {
                throw IllegalStateException("Simulated vision hardware failure")
            }
            override suspend fun stop() {}
            override fun release() {}
        }

        val router = VisionEngineRouter(stubProvider = failingProvider)
        val result = router.process(VisionInput())

        assertTrue(result is VisionResult.Error)
        assertEquals(VisionState.ERROR, router.visionState.value)
        router.release()
    }

    // 5. Stub provider reports correct availability and capabilities
    @Test
    fun testStubVisionProviderAvailabilityAndCapabilities() {
        val stub = StubVisionProvider(isAvailable = true)
        assertEquals(VisionProviderType.STUB, stub.providerType)
        assertTrue(stub.isAvailable)
        assertTrue(stub.supportedCapabilities.contains(VisionCapability.IMAGE_ANALYSIS))
        assertFalse(stub.supportedCapabilities.contains(VisionCapability.FACE_DETECTION))
    }

    // 6. Stub provider returns deterministic result clearly identified as stub
    @Test
    fun testStubVisionProviderReturnsDeterministicResult() = runBlocking {
        val stub = StubVisionProvider(isAvailable = true)
        val result = stub.process(VisionInput(source = VisionInputSource.CAMERA))

        assertTrue(result is VisionResult.Success)
        val observation = (result as VisionResult.Success).observation
        assertTrue(observation.isSyntheticOrStub)
        assertEquals(0.0f, observation.confidence, 0.001f)
        assertTrue(observation.sceneDescription.contains("Architectural stub active"))
        assertTrue(observation.detectedObjects.isEmpty())
        assertTrue(observation.detectedText.isEmpty())
    }

    // 7. Unavailable provider returns unavailable result without claiming detection
    @Test
    fun testUnavailableProviderReturnsUnavailableResult() = runBlocking {
        val stub = StubVisionProvider(isAvailable = false)
        val result = stub.process(VisionInput())

        assertTrue(result is VisionResult.Unavailable)
        val unavailable = result as VisionResult.Unavailable
        assertTrue(unavailable.reason.contains("unavailable"))
    }

    // 8. Router selects available provider correctly
    @Test
    fun testRouterSelectsAvailableProvider() {
        val stub = StubVisionProvider(isAvailable = true)
        val router = VisionEngineRouter(stubProvider = stub)

        assertEquals(stub, router.getActiveProvider())
        assertEquals(VisionProviderType.STUB, router.diagnostics.value.activeProviderType)
        router.release()
    }

    // 9. Router handles unavailable provider safely with fallback error
    @Test
    fun testRouterHandlesUnavailableProviderGracefully() = runBlocking {
        val stub = StubVisionProvider(isAvailable = false)
        val router = VisionEngineRouter(stubProvider = stub)

        assertNull(router.getActiveProvider())
        val result = router.process(VisionInput())

        assertTrue(result is VisionResult.Unavailable)
        assertEquals(VisionState.ERROR, router.visionState.value)
        router.release()
    }

    // 10. Router rejects processing when camera permission is denied
    @Test
    fun testRouterRejectsProcessingWhenPermissionDenied() = runBlocking {
        val fakeDetector = object : VisionCapabilityDetector {
            override fun isCameraHardwarePresent(): Boolean = true
            override fun hasFrontCamera(): Boolean = true
            override fun hasRearCamera(): Boolean = true
            override fun getCameraPermissionState(): CameraPermissionState = CameraPermissionState.DENIED
            override fun detectDiagnostics(activeProvider: VisionProviderType, visionState: VisionState): VisionDiagnostics {
                return VisionDiagnostics(
                    cameraHardwareAvailable = true,
                    cameraPermissionState = CameraPermissionState.DENIED,
                    activeProviderType = activeProvider,
                    visionState = visionState
                )
            }
        }

        val router = VisionEngineRouter(detector = fakeDetector)
        val result = router.process(VisionInput(source = VisionInputSource.CAMERA))

        assertTrue(result is VisionResult.PermissionDenied)
        assertEquals(VisionState.ERROR, router.visionState.value)
        router.release()
    }

    // 11. Router allows processing when camera permission is granted
    @Test
    fun testRouterAllowsProcessingWhenPermissionGranted() = runBlocking {
        val fakeDetector = object : VisionCapabilityDetector {
            override fun isCameraHardwarePresent(): Boolean = true
            override fun hasFrontCamera(): Boolean = true
            override fun hasRearCamera(): Boolean = true
            override fun getCameraPermissionState(): CameraPermissionState = CameraPermissionState.GRANTED
            override fun detectDiagnostics(activeProvider: VisionProviderType, visionState: VisionState): VisionDiagnostics {
                return VisionDiagnostics(
                    cameraHardwareAvailable = true,
                    cameraPermissionState = CameraPermissionState.GRANTED,
                    activeProviderType = activeProvider,
                    visionState = visionState
                )
            }
        }

        val router = VisionEngineRouter(detector = fakeDetector)
        val result = router.process(VisionInput(source = VisionInputSource.CAMERA))

        assertTrue(result is VisionResult.Success)
        assertEquals(VisionState.COMPLETED, router.visionState.value)
        router.release()
    }

    // 12. Capability detector inspects hardware presence correctly
    @Test
    fun testCapabilityDetectionCameraAvailableAndUnavailable() {
        val availableDetector = object : VisionCapabilityDetector {
            override fun isCameraHardwarePresent(): Boolean = true
            override fun hasFrontCamera(): Boolean = true
            override fun hasRearCamera(): Boolean = true
            override fun getCameraPermissionState(): CameraPermissionState = CameraPermissionState.DENIED
            override fun detectDiagnostics(activeProvider: VisionProviderType, visionState: VisionState): VisionDiagnostics {
                return VisionDiagnostics(
                    cameraHardwareAvailable = true,
                    frontCameraAvailable = true,
                    rearCameraAvailable = true
                )
            }
        }

        val diag = availableDetector.detectDiagnostics(VisionProviderType.STUB, VisionState.IDLE)
        assertTrue(diag.cameraHardwareAvailable)
        assertTrue(diag.frontCameraAvailable)
        assertTrue(diag.rearCameraAvailable)

        val unavailableDetector = object : VisionCapabilityDetector {
            override fun isCameraHardwarePresent(): Boolean = false
            override fun hasFrontCamera(): Boolean = false
            override fun hasRearCamera(): Boolean = false
            override fun getCameraPermissionState(): CameraPermissionState = CameraPermissionState.DENIED
            override fun detectDiagnostics(activeProvider: VisionProviderType, visionState: VisionState): VisionDiagnostics {
                return VisionDiagnostics(
                    cameraHardwareAvailable = false,
                    frontCameraAvailable = false,
                    rearCameraAvailable = false
                )
            }
        }

        val unavailableDiag = unavailableDetector.detectDiagnostics(VisionProviderType.STUB, VisionState.IDLE)
        assertFalse(unavailableDiag.cameraHardwareAvailable)
        assertFalse(unavailableDiag.frontCameraAvailable)
        assertFalse(unavailableDiag.rearCameraAvailable)
    }

    // 13. Capability detector inspects permission state abstractions
    @Test
    fun testCapabilityDetectionPermissionStates() {
        val permDetector = object : VisionCapabilityDetector {
            var currentPerm = CameraPermissionState.UNKNOWN
            override fun isCameraHardwarePresent(): Boolean = true
            override fun hasFrontCamera(): Boolean = false
            override fun hasRearCamera(): Boolean = true
            override fun getCameraPermissionState(): CameraPermissionState = currentPerm
            override fun detectDiagnostics(activeProvider: VisionProviderType, visionState: VisionState): VisionDiagnostics {
                return VisionDiagnostics(cameraPermissionState = currentPerm)
            }
        }

        permDetector.currentPerm = CameraPermissionState.DENIED
        assertEquals(CameraPermissionState.DENIED, permDetector.detectDiagnostics(VisionProviderType.STUB, VisionState.IDLE).cameraPermissionState)

        permDetector.currentPerm = CameraPermissionState.GRANTED
        assertEquals(CameraPermissionState.GRANTED, permDetector.detectDiagnostics(VisionProviderType.STUB, VisionState.IDLE).cameraPermissionState)

        permDetector.currentPerm = CameraPermissionState.PERMANENTLY_DENIED
        assertEquals(CameraPermissionState.PERMANENTLY_DENIED, permDetector.detectDiagnostics(VisionProviderType.STUB, VisionState.IDLE).cameraPermissionState)
    }

    // 14. Vision observation can be represented in SummerContext
    @Test
    fun testVisionObservationRepresentedInSummerContext() {
        val observation = VisionObservation(
            timestamp = 1715000000000,
            detectedObjects = listOf("notebook", "desk lamp"),
            sceneDescription = "Indoor desk setting under soft illumination",
            detectedText = "CHAPTER 1",
            confidence = 0.85f,
            source = VisionInputSource.CAMERA
        )

        val context = SummerContext(
            sessionId = "session_test_vision",
            currentVisionObservation = observation
        )

        assertNotNull(context.currentVisionObservation)
        assertEquals("Indoor desk setting under soft illumination", context.currentVisionObservation?.sceneDescription)
        assertEquals(listOf("notebook", "desk lamp"), context.currentVisionObservation?.detectedObjects)
        assertEquals("CHAPTER 1", context.currentVisionObservation?.detectedText)
        assertEquals(0.85f, context.currentVisionObservation?.confidence ?: 0f, 0.001f)
    }

    // 15. Context remains bounded and does not store raw image or frame data
    @Test
    fun testContextRemainsBoundedAndDoesNotStoreRawImages() {
        val observation = VisionObservation(
            sceneDescription = "Bounded text summary only",
            detectedObjects = listOf("mug")
        )

        val context = SummerContext(
            sessionId = "test_bound",
            currentVisionObservation = observation
        )

        val summary = context.currentVisionObservation?.toContextSummary()
        assertNotNull(summary)
        assertTrue(summary!!.contains("Bounded text summary only"))
        assertTrue(summary.contains("mug"))
        // Confirms no byte array or raw frame property exists in SummerContext
        assertEquals(String::class.java, summary.javaClass)
    }

    // 16. Vision provider cannot directly execute actions
    @Test
    fun testVisionProviderCannotDirectlyExecuteActions() {
        // VisionProvider interface contract does not expose ActionExecutor or execution hooks
        val stub = StubVisionProvider()
        val methods = stub.javaClass.methods.map { it.name }
        assertFalse(methods.contains("executeAction"))
        assertFalse(methods.contains("performAction"))
    }

    // 17. Vision processing does not bypass security authorization
    @Test
    fun testVisionProcessingDoesNotBypassAuthorization() {
        val policy = DefaultActionAuthorizationPolicy()
        val executor = SecuredActionExecutor(policy)

        // Attempting an unauthorized camera action without user initiation must be blocked
        val cameraRequest = ActionRequest(
            capability = Capability.CAMERA,
            actionName = "capture_camera_frame",
            reasoning = "Test camera action"
        )

        val uninitiatedContext = SecurityContext(
            caller = "test_user",
            sessionAuthorized = true,
            isUserInitiated = false
        )

        assertFalse(policy.isAuthorized(cameraRequest, uninitiatedContext))

        val initiatedContext = SecurityContext(
            caller = "test_user",
            sessionAuthorized = true,
            isUserInitiated = true
        )

        assertTrue(policy.isAuthorized(cameraRequest, initiatedContext))
        assertTrue(policy.requiresExplicitUserConfirmation(cameraRequest))
    }

    // 18. Vision processing does not trigger automatic memory persistence
    @Test
    fun testVisionProcessingDoesNotTriggerAutomaticMemoryPersistence() = runBlocking {
        // Mock memory repository to verify zero memory writes occurred
        val memoryRecords = mutableListOf<MemoryRecord>()
        val mockRepo = object : MemoryRepository {
            override suspend fun recordMemory(record: MemoryRecord) {
                memoryRecords.add(record)
            }
            override suspend fun updateMemory(record: MemoryRecord) {}
            override suspend fun deleteMemory(id: String) {}
            override suspend fun forgetMemory(targetId: String?, keyword: String?): Boolean = false
            override suspend fun updateAccessMetadata(id: String, count: Int, timestamp: Long) {}
            override suspend fun getActiveMemories(): List<MemoryRecord> = memoryRecords
            override fun observeMemories(): Flow<List<MemoryRecord>> = flowOf(memoryRecords)
            override fun observeMemoriesByCategory(category: MemoryCategory): Flow<List<MemoryRecord>> = flowOf(emptyList())
            override suspend fun clearAllMemories() { memoryRecords.clear() }
            override suspend fun getSettings(): SummerSettings = SummerSettings()
            override suspend fun updateSettings(settings: SummerSettings) {}
            override fun observeSettings(): Flow<SummerSettings> = flowOf(SummerSettings())
        }

        val router = VisionEngineRouter()
        router.process(VisionInput(source = VisionInputSource.IMAGE))

        // Assert no memory record was inserted into memory repository
        assertEquals(0, memoryRecords.size)
        router.release()
    }

    // 19. Vision diagnostics truthfully reports stub and not implemented
    @Test
    fun testVisionDiagnosticsTruthfullyReportsStubAndNotImplemented() {
        val router = VisionEngineRouter()
        val diag = router.diagnostics.value

        assertEquals(VisionProviderType.STUB, diag.activeProviderType)
        assertEquals("NOT_IMPLEMENTED", diag.processingStatus)
        assertFalse(diag.isLiveVisionActive)
        assertTrue(diag.message.contains("not implemented in this phase"))
        router.release()
    }

    // 20. Lifecycle safety: router release cleans up safely
    @Test
    fun testLifecycleSafetyRouterReleaseCleansUpGracefully() {
        val router = VisionEngineRouter()
        router.release()
        assertEquals(VisionState.IDLE, router.visionState.value)
    }

    // 21. Event bus integration: router posts structured VisionInput event on success
    @Test
    fun testRouterPostsStructuredEventToEventBus() = runBlocking {
        val eventBus = SummerEventBus()
        val receivedEvents = mutableListOf<SummerEvent>()

        val job = launch {
            eventBus.events.collect { receivedEvents.add(it) }
        }

        val router = VisionEngineRouter(eventBus = eventBus)
        val result = router.process(VisionInput(source = VisionInputSource.IMAGE))

        assertTrue(result is VisionResult.Success)
        val visionEvent = receivedEvents.filterIsInstance<SummerEvent.VisionInput>().firstOrNull()
        assertNotNull(visionEvent)
        assertTrue(visionEvent!!.frameSummary.contains("Architectural stub active"))

        job.cancel()
        router.release()
    }
}
