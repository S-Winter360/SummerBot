package com.example

import com.example.actions.ActionRequest
import com.example.core.event.EventPriority
import com.example.core.event.SummerEvent
import com.example.core.event.SummerEventBus
import com.example.core.interaction.InteractionState
import com.example.core.interaction.SummerInteraction
import com.example.core.personality.SummerPersonality
import com.example.core.session.SummerSessionManager
import com.example.core.state.SummerState
import com.example.core.state.SummerStateManager
import com.example.security.Capability
import com.example.security.DefaultActionAuthorizationPolicy
import com.example.security.SecurityContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SummerArchitectureTest {

    @Test
    fun testStateManagerTransitionsAndHistory() {
        val manager = SummerStateManager()
        assertEquals(SummerState.Idle, manager.state.value)
        assertEquals("Idle", manager.state.value.displayName)

        manager.transitionTo(SummerState.Listening())
        assertTrue(manager.state.value is SummerState.Listening)
        assertEquals("Listening", manager.state.value.displayName)

        manager.transitionTo(SummerState.Thinking())
        assertTrue(manager.state.value is SummerState.Thinking)
        assertEquals("Thinking", manager.state.value.displayName)

        manager.transitionTo(SummerState.Speaking("Acknowledged."))
        assertTrue(manager.state.value is SummerState.Speaking)
        assertEquals("Speaking", manager.state.value.displayName)

        manager.transitionTo(SummerState.Executing("Test Task"))
        assertTrue(manager.state.value is SummerState.Executing)
        assertEquals("Executing", manager.state.value.displayName)

        manager.transitionTo(SummerState.Observing("Sensor inputs"))
        assertTrue(manager.state.value is SummerState.Observing)
        assertEquals("Observing", manager.state.value.displayName)

        manager.transitionTo(SummerState.Learning("Context pattern"))
        assertTrue(manager.state.value is SummerState.Learning)
        assertEquals("Learning", manager.state.value.displayName)

        manager.transitionTo(SummerState.Error("Network failure"))
        assertTrue(manager.state.value is SummerState.Error)
        assertEquals("Error", manager.state.value.displayName)

        manager.resetToIdle()
        assertEquals(SummerState.Idle, manager.state.value)
        assertEquals(8, manager.transitionHistory.value.size)
    }

    @Test
    fun testSessionManagerLifecycle() {
        val sessionManager = SummerSessionManager()
        val initialSession = sessionManager.getActiveSession()
        assertEquals(0, initialSession.interactions.size)

        val interaction = SummerInteraction(
            sessionId = initialSession.id,
            userInput = "Hello",
            state = InteractionState.COMPLETED
        )
        sessionManager.recordInteraction(interaction)
        assertEquals(1, sessionManager.getActiveSession().interactions.size)

        val newSession = sessionManager.startNewSession()
        assertTrue(newSession.id != initialSession.id)
        assertEquals(1, sessionManager.getSessionHistory().size)
    }

    @Test
    fun testEventBusPublishAndObserve() = runTest {
        val bus = SummerEventBus()
        val event = SummerEvent.UserTextInput(
            text = "Testing bus",
            priority = EventPriority.NORMAL
        )
        bus.publish(event)
        val observed = bus.observe<SummerEvent.UserTextInput>().first()
        assertEquals("Testing bus", observed.text)
    }

    @Test
    fun testSecurityPolicySessionAuthorization() {
        val policy = DefaultActionAuthorizationPolicy()
        val request = ActionRequest(
            capability = Capability.INTERNET,
            actionName = "Check Connectivity",
            reasoning = "Network verification"
        )

        val authorizedContext = SecurityContext(
            caller = "TEST_CALLER",
            sessionAuthorized = true
        )
        assertTrue(policy.isAuthorized(request, authorizedContext))

        val unauthorizedContext = SecurityContext(
            caller = "TEST_CALLER",
            sessionAuthorized = false
        )
        assertFalse(policy.isAuthorized(request, unauthorizedContext))
    }

    @Test
    fun testCapabilityConfirmationRequirements() {
        val policy = DefaultActionAuthorizationPolicy()

        val netRequest = ActionRequest(Capability.INTERNET, "Net", "reason")
        assertFalse(policy.requiresExplicitUserConfirmation(netRequest))

        val camRequest = ActionRequest(Capability.CAMERA, "Cam", "reason")
        assertTrue(policy.requiresExplicitUserConfirmation(camRequest))

        val sysRequest = ActionRequest(Capability.SYSTEM_SETTINGS, "Settings", "reason")
        assertTrue(policy.requiresExplicitUserConfirmation(sysRequest))
    }

    @Test
    fun testPersonalityCharacteristics() {
        val personality = SummerPersonality.DEFAULT
        assertEquals("Summer Winter", personality.fullName)
        assertEquals("Summer", personality.shortName)
        assertTrue(personality.archetype.contains("observant", ignoreCase = true))
        assertTrue(personality.toneDirective.contains("calm", ignoreCase = true))
    }
}
