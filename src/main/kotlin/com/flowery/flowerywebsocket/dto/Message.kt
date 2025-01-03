package com.flowery.flowerywebsocket.dto

import com.flowery.flowerywebsocket.constants.MessageType

data class Message(
    val sender: String,
    val receiver: String,
    val type: MessageType,
    val payload: String,
)