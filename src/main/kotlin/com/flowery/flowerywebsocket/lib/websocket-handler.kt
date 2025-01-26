package com.flowery.flowerywebsocket.lib

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.flowery.flowerywebsocket.dto.Message
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler
import java.nio.ByteBuffer

@Component
class WebSocketHandler(
    private val hub: ConnectionHub,
    private val messageBuffer: MessageBuffer
) : TextWebSocketHandler() {
    private val logger = LoggerFactory.getLogger(WebSocketHandler::class.java)
    private val mapper = jacksonObjectMapper()

    override fun afterConnectionEstablished(session: WebSocketSession) {
        val userId = session.uri?.query?.split("=")?.get(1) ?: "unknown"
        session.attributes["userId"] = userId
        hub.addSession(userId, session)
        logger.info("User $userId connected with session ${session.id}")
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        val userId = session.attributes["userId"] as String
        hub.removeSession(session)
        messageBuffer.purge(session.id)
        logger.info("User $userId disconnected: $status")
    }

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        try {
            val msgData = mapper.readValue(message.payload, Message::class.java)
            val recvSession = hub.getSession(msgData.receiverId)

            if (recvSession != null && recvSession.isOpen) {
                messageBuffer.write(ByteBuffer.wrap(message.payload.toByteArray()), recvSession.id)
                messageBuffer.process(recvSession.id)
                
                var payload = messageBuffer.read(recvSession.id)
                while (payload != null) {
                    recvSession.sendMessage(TextMessage(String(payload.array())))
                    payload = messageBuffer.read(recvSession.id)
                }
            }
        } catch (e: Exception) {
            logger.error("Error handling message", e)
        }
    }
}