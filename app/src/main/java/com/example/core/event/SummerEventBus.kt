package com.example.core.event

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filterIsInstance

/**
 * Lightweight, coroutine-based internal event bus for Summer.
 * Not a global singleton: instantiated and owned explicitly by the core runtime.
 */
class SummerEventBus(
    replay: Int = 10,
    extraBufferCapacity: Int = 64
) {
    private val _events = MutableSharedFlow<SummerEvent>(
        replay = replay,
        extraBufferCapacity = extraBufferCapacity
    )
    val events: Flow<SummerEvent> = _events.asSharedFlow()

    /**
     * Publishes a new event to all active observers.
     */
    suspend fun publish(event: SummerEvent) {
        _events.emit(event)
    }

    /**
     * Subscribes to events of a specific type.
     */
    inline fun <reified T : SummerEvent> subscribe(): Flow<T> {
        return events.filterIsInstance<T>()
    }
}
