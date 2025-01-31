package com.flowery.flowerywebsocket.lib

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.socket.WebSocketSession
import java.util.concurrent.ConcurrentHashMap

@Component
class ConnectionHub {
    private val sessions = ConcurrentHashMap<String, WebSocketSession>()
    private val connections = ConcurrentHashMap<String, String>()

    fun addSession(userId: String, session: WebSocketSession) {
        sessions[session.id] = session
        connections[userId] = session.id
        logger.info("User $userId connected with session ${session.id}")
    }

    fun removeSession(session: WebSocketSession) {
        sessions.remove(session.id)
        connections.entries.removeIf { it.value == session.id }
        logger.info("Session ${session.id} removed")
    }

    fun getSession(userId: String): WebSocketSession? {
        return connections[userId]?.let { sessions[it] }
    }

    fun isConnected(userId: String): Boolean {
        return connections.containsKey(userId)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ConnectionHub::class.java)
    }
}