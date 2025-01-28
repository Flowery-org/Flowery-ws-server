package com.flowery.flowerywebsocket.lib

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.flowery.flowerywebsocket.dto.Message
import com.flowery.flowerywebsocket.logger.AppLogger
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap


val fbLogger = AppLogger(appName = "flowery-ws-server")

//* Test Data:{"type":"MESSAGE","senderId":"a","receiverId":"b","payload":"Hello, how are you?"}
@Component
class WebSocketHandler(private val redisTemplate: RedisTemplate<String, String>): TextWebSocketHandler() {
    private val logger = LoggerFactory.getLogger(WebSocketHandler::class.java)
    private val hub = ConnectionHub()
    //private val buffers = ConcurrentHashMap<String, MessageBuffer>()
    private val mapper = jacksonObjectMapper()

    override fun afterConnectionEstablished(session: WebSocketSession) {
        // Extract userId from query parameters
        val userId = session.uri?.query?.split("=")?.get(1) ?: "unknown"
        session.attributes["userId"] = userId

        hub.addSession(userId, session)
        //buffers[session.id] = MessageBuffer()
        logger.info("User $userId connected with session ${session.id}")
        fbLogger.info("User $userId connected with session ${session.id}")
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        val userId = session.attributes["userId"] as String
        hub.removeSession(session)
        //buffers[session.id]?.purge()
        //buffers.remove(session.id)
        logger.info("User $userId disconnected: $status")
        fbLogger.info("User $userId disconnected: $status")
    }

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        val msgData = mapper.readValue(message.payload, Message::class.java)
        //redis에 메시지 저장
        val msgKey = "messages: ${msgData.receiverId}"
        logger.info("Storing message in Redis for key: $msgKey, value: $msgData")
        redisTemplate.opsForList().leftPush(msgKey, message.payload)
        logger.info("Message stored in Redis for key: messages:${msgData.receiverId}")

        val recvSession = hub.getSession(msgData.receiverId)
        if(recvSession != null && recvSession.isOpen) {
            val storedMessage = redisTemplate.opsForList().rightPop(msgKey)
            //val messageToSend = redisTemplate.opsForList().index(msgKey, -1) // 데이터 삭제 없이 가져오기
            storedMessage?.let {
                val messageToSend = mapper.readValue(it, Message::class.java) // JSON -> Message 객체 변환
                recvSession.sendMessage(TextMessage(mapper.writeValueAsString(messageToSend)))
                logger.info("Message sent to ${msgData.receiverId}: $messageToSend")
            }
        }

    }
}