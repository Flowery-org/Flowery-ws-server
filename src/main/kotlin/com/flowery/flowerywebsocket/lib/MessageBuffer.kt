package com.flowery.flowerywebsocket.lib

import org.slf4j.LoggerFactory
import org.springframework.amqp.core.Message
import org.springframework.amqp.core.MessageProperties
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Component
import java.nio.ByteBuffer

@Component
class MessageBuffer(
        private val rabbitTemplate: RabbitTemplate
) {
    private val exchange = "websocket.exchange"
    private val writeRoutingKey = "write"
    private val readRoutingKey = "read"

    fun write(payload: ByteBuffer, sessionId: String) {
        try {
            val messageProperties = MessageProperties().apply {
                headers["sessionId"] = sessionId
                contentType = MessageProperties.CONTENT_TYPE_BYTES
            }

            val message = Message(payload.array(), messageProperties)
            rabbitTemplate.send(exchange, writeRoutingKey, message)
            // [INFO] 메세지큐(rabbitMQ) 저장 (event: message_queued)
            logger.debug("Message written to queue for session: $sessionId")
        } catch (e: Exception) {
            // [ERROR] 메세지 저장 중 오류 발생 (event: message_queue_error)
            logger.error("Error writing message to queue", e)
        }
    }

    fun read(sessionId: String): ByteBuffer? {
        return try {
            val message = rabbitTemplate.receive("${sessionId}.read.queue")
            // [INFO] 메시지를 RabbitMQ에서 읽음 (event: message_read)
            message?.body?.let { ByteBuffer.wrap(it) }
        } catch (e: Exception) {
            // [ERROR] 메시지 읽기 중 오류 발생 (event: message_read_error)
            logger.error("Error reading message from queue", e)
            null
        }
    }

    fun process(sessionId: String) {
        try {
            var message = rabbitTemplate.receive("${sessionId}.write.queue")
            while (message != null) {
                rabbitTemplate.send(exchange, "${sessionId}.read", message)
                // [INFO] 메시지가 RabbitMQ에서 처리됨 (event: message_transferred)
                message = rabbitTemplate.receive("${sessionId}.write.queue")
            }
        } catch (e: Exception) {
            // [ERROR] 메시지 처리 중 오류 발생 (event: message_transfer_error)
            logger.error("Error processing messages", e)
        }
    }

    fun purge(sessionId: String) {
        try {
            rabbitTemplate.execute { channel ->
                channel.queuePurge("${sessionId}.write.queue")
                channel.queuePurge("${sessionId}.read.queue")
            }
            // [INFO] RabbitMQ 큐 정리 (event: rabbitQueue_purged)
            logger.info("Purged queues for session: $sessionId")
        } catch (e: Exception) {
            // [ERROR] RabbitMQ 큐 정리 중 오류 발생 (event: rabbitQueue_purge_error)
            logger.error("Error purging queues", e)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(MessageBuffer::class.java)
    }
}