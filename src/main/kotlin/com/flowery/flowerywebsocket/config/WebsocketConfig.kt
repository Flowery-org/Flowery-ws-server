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
@EnableWebSocketMessageBroker //* Enables websocket message handling
class WebsocketConfig: WebSocketMessageBrokerConfigurer {

    //* Define Endpoint for websocket connection (/ws)
    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        //* Testing purpose -> allows all connection from any origins (CORS)
        registry.addEndpoint("/ws").setAllowedOrigins("*").withSockJS()
    }

    //* Define Prefixes for messages
    override fun configureMessageBroker(registry: MessageBrokerRegistry) {
        //* Client -> Server Endpoint
        //* Message starting with this prefix will be routed to @MessageMapping methods
        registry.setApplicationDestinationPrefixes("/app")

        //* Server -> Client Endpoint
        //* Message broker for this prefix
        registry.enableSimpleBroker("/topic")
    }
}