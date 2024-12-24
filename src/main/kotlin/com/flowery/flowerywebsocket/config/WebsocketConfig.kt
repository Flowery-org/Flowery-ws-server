package com.flowery.flowerywebsocket.config

import org.springframework.context.annotation.Configuration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer

/**
 * Use STOMP for web socket <- need for serialization.
 * */

@Configuration
@EnableWebSocketMessageBroker
class WebsocketConfig: WebSocketMessageBrokerConfigurer {

    //* Define Endpoint for websocket connection
    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        registry.addEndpoint("/ws").setAllowedOrigins("*").withSockJS()
    }

    //* Define Prefixes for messages
    override fun configureMessageBroker(registry: MessageBrokerRegistry) {
        //* Client -> Server Endpoint
        registry.setApplicationDestinationPrefixes("/app")

        //* Server -> Client Endpoint
        registry.enableSimpleBroker("/topic")
    }
}