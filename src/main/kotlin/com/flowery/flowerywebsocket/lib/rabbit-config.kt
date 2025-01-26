package com.flowery.flowerywebsocket.config

import org.springframework.amqp.core.*
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.amqp.support.converter.MessageConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RabbitConfig {
    
    @Bean
    fun websocketExchange(): DirectExchange {
        return DirectExchange("websocket.exchange")
    }
    
    @Bean
    fun deadLetterExchange(): DirectExchange {
        return DirectExchange("websocket.dlx")
    }

    @Bean
    fun deadLetterQueue(): Queue {
        return QueueBuilder.durable("websocket.dlq")
            .build()
    }

    @Bean
    fun deadLetterBinding(): Binding {
        return BindingBuilder
            .bind(deadLetterQueue())
            .to(deadLetterExchange())
            .with("websocket.dead")
    }

    @Bean
    fun messageConverter(): MessageConverter {
        return Jackson2JsonMessageConverter()
    }

    @Bean
    fun rabbitTemplate(
        connectionFactory: ConnectionFactory,
        messageConverter: MessageConverter
    ): RabbitTemplate {
        return RabbitTemplate(connectionFactory).apply {
            this.messageConverter = messageConverter
        }
    }

    fun createQueueForSession(sessionId: String): Queue {
        return QueueBuilder.durable("${sessionId}.queue")
            .withArgument("x-dead-letter-exchange", "websocket.dlx")
            .withArgument("x-dead-letter-routing-key", "websocket.dead")
            .withArgument("x-message-ttl", 30000) // 30 seconds TTL
            .build()
    }

    fun createBindingForSession(sessionId: String, queue: Queue): Binding {
        return BindingBuilder
            .bind(queue)
            .to(websocketExchange())
            .with(sessionId)
    }

    companion object {
        const val EXCHANGE_NAME = "websocket.exchange"
        const val DEAD_LETTER_EXCHANGE = "websocket.dlx"
        const val DEAD_LETTER_QUEUE = "websocket.dlq"
        const val DEAD_LETTER_ROUTING_KEY = "websocket.dead"
    }
}