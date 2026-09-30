package com.example.ai.capability

enum class AIAvailabilityStatus(val label: String) {
    CHECKING("Checking..."),
    AVAILABLE("Available"),
    DOWNLOADABLE("Downloadable"),
    DOWNLOADING("Downloading..."),
    UNAVAILABLE("Unavailable"),
    NOT_SUPPORTED("Not Supported"),
    ERROR("Error")
}
