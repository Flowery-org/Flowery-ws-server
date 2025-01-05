package com.flowery.flowerywebsocket.lib

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.flowery.flowerywebsocket.dto.Message
import org.slf4j.LoggerFactory
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap


//* Test Data:{"type":"MESSAGE","senderId":"a","receiverId":"b","payload":"Hello, how are you?"}

class WebSocketHandler: TextWebSocketHandler() {
    private val logger = LoggerFactory.getLogger(WebSocketHandler::class.java)
    private val hub = ConnectionHub()
    private val buffers = ConcurrentHashMap<String, MessageBuffer>()
    private val mapper = jacksonObjectMapper()

    override fun afterConnectionEstablished(session: WebSocketSession) {
        // Extract userId from query parameters
        val userId = session.uri?.query?.split("=")?.get(1) ?: "unknown"
        session.attributes["userId"] = userId

        hub.addSession(userId, session)
        buffers[session.id] = MessageBuffer()
        logger.info("User $userId connected with session ${session.id}")
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        val userId = session.attributes["userId"] as String
        hub.removeSession(session)
        buffers[session.id]?.purge()
        buffers.remove(session.id)
        logger.info("User $userId disconnected: $status")
    }

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        val msgData = mapper.readValue(message.payload, Message::class.java)
        val recvSession = hub.getSession(msgData.receiverId)
        if(recvSession != null && recvSession.isOpen) {
            val buf = buffers[recvSession.id]
            buf?.write(ByteBuffer.wrap(message.payload.toByteArray()))
            buf?.process()

            //* Finish write. The read message must be sent.
            var payload = buf?.read()
            while(payload != null) {
                recvSession.sendMessage(TextMessage(String(payload.array())))
                payload = buf?.read()
            }

        }

    }
}