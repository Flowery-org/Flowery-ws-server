package com.flowery.flowerywebsocket.dto

import com.flowery.flowerywebsocket.constants.MessageType

data class Message (
    val type: MessageType,
    val senderId: String,
    val receiverId: String,
    val payload: String,
    val timestamp: Long = System.currentTimeMillis(),
)