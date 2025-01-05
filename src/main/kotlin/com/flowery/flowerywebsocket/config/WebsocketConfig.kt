package com.flowery.flowerywebsocket.config

import com.flowery.flowerywebsocket.lib.WebSocketHandler
import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry

@Configuration
@EnableWebSocket //* Enables websocket message handling
class WebsocketConfig: WebSocketConfigurer {

    //* Define Endpoint for websocket connection (/ws)
    override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
        //* Testing purpose -> allows all connection from any origins (CORS)
        registry.addHandler(WebSocketHandler(), "/ws").setAllowedOrigins("*")
    }

}