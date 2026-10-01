package com.example.core.event

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filterIsInstance

class SummerEventBus(replayCount: Int = 10) {
    private val _events = MutableSharedFlow<SummerEvent>(replay = replayCount, extraBufferCapacity = 64)
    val events: Flow<SummerEvent> = _events.asSharedFlow()

    suspend fun publish(event: SummerEvent) {
        _events.emit(event)
    }

    inline fun <reified T : SummerEvent> observe(): Flow<T> = events.filterIsInstance()
}
