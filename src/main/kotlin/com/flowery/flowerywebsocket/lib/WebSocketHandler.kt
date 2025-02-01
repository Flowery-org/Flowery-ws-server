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

        // [INFO] WebSocket 연결 시작 (event: websocket_connection)
        logger.info("User $userId connected with session ${session.id}")
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        val userId = session.attributes["userId"] as String
        hub.removeSession(session)
        messageBuffer.purge(session.id)

        // [INFO] WebSocket 연결 종료 (event: websocket_disconnection)
        logger.info("User $userId disconnected: $status")
    }

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        try {
            val msgData = mapper.readValue(message.payload, Message::class.java)
            val recvSession = hub.getSession(msgData.receiverId)
            // [INFO] WebSocket 메세지 수신 (event: ws_message_received)

            if (recvSession != null && recvSession.isOpen) {
                messageBuffer.write(ByteBuffer.wrap(message.payload.toByteArray()), recvSession.id)
                messageBuffer.process(recvSession.id)

                var payload = messageBuffer.read(recvSession.id)
                while (payload != null) {
                    recvSession.sendMessage(TextMessage(String(payload.array())))
                    // [INFO] WebSocket 메세지 전송(event: ws_message_sent)
                    payload = messageBuffer.read(recvSession.id)
                }
            }
        } catch (e: Exception) {
            // [ERROR] 메세지 처리 중 오류 발생 (event: ws_message_processing_error)
            logger.error("Error handling message", e)
        }
    }
}