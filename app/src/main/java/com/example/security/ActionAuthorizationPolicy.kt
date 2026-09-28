package com.example.security

import com.example.memory.models.SummerSettings

/**
 * Gatekeeper policy interface that determines whether an AI action request
 * is authorized to execute.
 */
interface ActionAuthorizationPolicy {
    suspend fun evaluate(
        capability: Capability,
        context: SecurityContext,
        currentSettings: SummerSettings
    ): AuthorizationResult
}

/**
 * Default implementation enforcing least-privilege capability control
 * based on the user's active persistent settings.
 */
class DefaultActionAuthorizationPolicy : ActionAuthorizationPolicy {

    override suspend fun evaluate(
        capability: Capability,
        context: SecurityContext,
        currentSettings: SummerSettings
    ): AuthorizationResult {
        // Master switch check
        if (!currentSettings.summerEnabled) {
            return AuthorizationResult.Denied("Summer is currently disabled in system settings.")
        }

        return when (capability) {
            Capability.MICROPHONE -> {
                if (!currentSettings.voiceInteractionEnabled) {
                    AuthorizationResult.Denied("Voice interaction is disabled in settings.")
                } else if (!context.isAppInForeground) {
                    AuthorizationResult.Denied("Background microphone access is strictly disallowed by policy.")
                } else {
                    AuthorizationResult.Granted("Voice interaction permitted in foreground.")
                }
            }

            Capability.CAMERA -> {
                if (!currentSettings.cameraAccessAllowed) {
                    AuthorizationResult.Denied("Camera access is turned off in settings.")
                } else {
                    AuthorizationResult.RequiresUserConsent(
                        prompt = "Summer is requesting camera access to inspect context.",
                        rationale = "Camera visual input requires explicit user confirmation per session."
                    )
                }
            }

            Capability.INTERNET -> {
                if (!currentSettings.internetAccessAllowed) {
                    AuthorizationResult.Denied("Internet access is disabled. Summer operates exclusively in offline mode.")
                } else {
                    AuthorizationResult.Granted("Internet access enabled in settings.")
                }
            }

            Capability.SPEAKER_RECOGNITION -> {
                if (!currentSettings.speakerRecognitionEnabled) {
                    AuthorizationResult.Denied("Speaker recognition is disabled in settings.")
                } else {
                    AuthorizationResult.Granted("Speaker recognition permitted locally.")
                }
            }

            Capability.LOCAL_MEMORY_WRITE -> {
                if (!currentSettings.personalMemoryEnabled) {
                    AuthorizationResult.Denied("Personal memory retention is disabled.")
                } else {
                    AuthorizationResult.Granted("Local memory writing permitted.")
                }
            }

            Capability.LOCAL_MEMORY_READ -> {
                AuthorizationResult.Granted("Local memory read permitted.")
            }

            Capability.DEVICE_SETTINGS -> {
                AuthorizationResult.RequiresUserConsent(
                    prompt = "Summer is requesting to alter a device setting.",
                    rationale = "Modifying device configuration requires explicit user consent."
                )
            }

            Capability.SYSTEM_AUTOMATION -> {
                AuthorizationResult.Denied(
                    "Autonomous background automation is deactivated in this phase."
                )
            }
        }
    }
}
