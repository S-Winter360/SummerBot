package com.example

import com.example.actions.ActionRequest
import com.example.actions.ActionResult
import com.example.actions.SecuredActionExecutor
import com.example.core.personality.BehavioralTrait
import com.example.core.personality.SummerPersonality
import com.example.core.state.SummerState
import com.example.core.state.SummerStateManager
import com.example.memory.models.SummerSettings
import com.example.security.Capability
import com.example.security.DefaultActionAuthorizationPolicy
import com.example.security.SecurityContext
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SummerArchitectureTest {

    @Test
    fun testStateManagerTransitions() {
        val manager = SummerStateManager()
        assertEquals(SummerState.Idle, manager.state.value)

        manager.transitionTo(SummerState.Listening())
        assertTrue(manager.state.value is SummerState.Listening)

        manager.transitionTo(SummerState.Thinking())
        assertTrue(manager.state.value is SummerState.Thinking)

        manager.transitionTo(SummerState.Speaking("Acknowledged."))
        assertTrue(manager.state.value is SummerState.Speaking)

        manager.resetToIdle()
        assertEquals(SummerState.Idle, manager.state.value)
        assertEquals(4, manager.transitionHistory.value.size)
    }

    @Test
    fun testSecurityPolicyDeniesInternetWhenDisabled() = runTest {
        val policy = DefaultActionAuthorizationPolicy()
        val settings = SummerSettings(internetAccessAllowed = false)
        val context = SecurityContext()

        val result = policy.evaluate(Capability.INTERNET, context, settings)
        assertFalse(result.isAllowed)
    }

    @Test
    fun testSecurityPolicyAllowsInternetWhenEnabled() = runTest {
        val policy = DefaultActionAuthorizationPolicy()
        val settings = SummerSettings(internetAccessAllowed = true)
        val context = SecurityContext()

        val result = policy.evaluate(Capability.INTERNET, context, settings)
        assertTrue(result.isAllowed)
    }

    @Test
    fun testActionExecutorBlocksUnauthorizedRequests() = runTest {
        val policy = DefaultActionAuthorizationPolicy()
        val executor = SecuredActionExecutor(policy)
        val settings = SummerSettings(voiceInteractionEnabled = false)

        val request = ActionRequest(
            capability = Capability.MICROPHONE,
            actionName = "Activate Mic",
            reasoning = "Testing microphone gate"
        )

        val outcome = executor.execute(request, SecurityContext(), settings)
        assertTrue(outcome is ActionResult.Denied)
        assertEquals(1, executor.auditLog.value.size)
        assertFalse(executor.auditLog.value.first().isAuthorized)
    }

    @Test
    fun testPersonalityCharacteristics() {
        val personality = SummerPersonality.DEFAULT
        assertEquals("Summer Winter", personality.name)
        assertEquals("Summer", personality.shortName)
        assertEquals("Personal AI Companion", personality.role)
        assertTrue(personality.traits.contains(BehavioralTrait.CALM))
        assertTrue(personality.traits.contains(BehavioralTrait.INTELLIGENT))
        assertTrue(personality.traits.contains(BehavioralTrait.WARM))
        assertTrue(personality.traits.contains(BehavioralTrait.CONCISE_BY_DEFAULT))
        assertTrue(personality.traits.contains(BehavioralTrait.CONVERSATIONAL))
        assertTrue(personality.traits.contains(BehavioralTrait.RESPECTFUL))
        assertTrue(personality.traits.contains(BehavioralTrait.MODERATELY_PROACTIVE))
        assertTrue(personality.traits.contains(BehavioralTrait.CURIOUS))
        assertTrue(personality.traits.contains(BehavioralTrait.NON_INTRUSIVE))
    }
}
