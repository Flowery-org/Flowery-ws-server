package com.flowery.flowerywebsocket.config

import org.springframework.amqp.core.*
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.amqp.support.converter.MessageConverter
import org.springframework.context.annotation.Bean

@Bean
fun messageConverter(): MessageConverter {
    return Jackson2JsonMessageConverter()
}