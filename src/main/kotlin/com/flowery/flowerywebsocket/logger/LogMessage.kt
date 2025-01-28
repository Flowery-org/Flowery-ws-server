package com.flowery.flowerywebsocket.logger

import java.time.Instant

data class LogMessage(
    val level: String,
    val message: String,
    val timestamp: Instant = Instant.now(),
    val throwable: Throwable? = null
)