package com.flowery.flowerywebsocket.lib

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.flowery.flowerywebsocket.dto.Message
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.core.Queue
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap

@Configuration
class RabbitConfig {
    @Bean
    fun messageQueue() = Queue("message_queue", true)

    @Bean
    fun offlineMessageQueue() = Queue("offline_message_queue", true)
}

class WebSocketHandler(
        private val rabbitTemplate: RabbitTemplate
) : TextWebSocketHandler() {
    private val logger = LoggerFactory.getLogger(WebSocketHandler::class.java)
    private val hub = ConnectionHub()
    private val messageBuffers = ConcurrentHashMap<String, MessageBuffer>()
    private val mapper = jacksonObjectMapper()

    override fun afterConnectionEstablished(session: WebSocketSession) {
        val userId = session.uri?.query?.split("=")?.get(1) ?: "unknown"
        session.attributes["userId"] = userId
        hub.addSession(userId, session)
        messageBuffers[userId] = MessageBuffer()

        // 오프라인 동안의 메시지 처리
        processOfflineMessages(userId)

        logger.info("User $userId connected with session ${session.id}")
    }

    private fun processOfflineMessages(userId: String) {
        val buffer = messageBuffers[userId] ?: return

        // RabbitMQ의 offline_message_queue에서 메시지 가져오기
        var message = rabbitTemplate.receive("offline_message_queue")
        while (message != null) {
            val messageData = mapper.readValue(message.body, Message::class.java)
            if (messageData.receiverId == userId) {
                // 메시지를 버퍼에 추가
                buffer.write(ByteBuffer.wrap(message.body))
                buffer.process()

                // 버퍼에서 메시지 읽어서 전송
                var payload = buffer.read()
                while (payload != null) {
                    val session = hub.getSession(userId)
                    session?.sendMessage(TextMessage(String(payload.array())))
                    payload = buffer.read()
                }
            }
            message = rabbitTemplate.receive("offline_message_queue")
        }
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        val userId = session.attributes["userId"] as String
        hub.removeSession(session)
        messageBuffers[userId]?.purge()
        messageBuffers.remove(userId)
        logger.info("User $userId disconnected: $status")
    }

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        val msgData = mapper.readValue(message.payload, Message::class.java)

        val recvSession = hub.getSession(msgData.receiverId)
        if (recvSession != null && recvSession.isOpen) {
            // 수신자가 온라인인 경우
            val buffer = messageBuffers[msgData.receiverId]
            buffer?.write(ByteBuffer.wrap(message.payload.toByteArray()))
            buffer?.process()

            // 버퍼에서 읽어서 전송
            var payload = buffer?.read()
            while (payload != null) {
                recvSession.sendMessage(TextMessage(String(payload.array())))
                payload = buffer.read()
            }
        } else {
            // 수신자가 오프라인인 경우 RabbitMQ에 저장
            rabbitTemplate.convertAndSend(
                    "offline_message_queue",
                    message.payload
            )
            logger.info("Stored offline message for user ${msgData.receiverId}")
        }
    }
}
