package com.flowery.flowerywebsocket.logger

import com.flowery.flowerywebsocket.logger.LogMessage
import org.slf4j.LoggerFactory
import java.io.PrintWriter
import java.net.Socket
import java.util.concurrent.LinkedBlockingQueue
import kotlin.concurrent.thread

class AppLogger(
    private val host: String = "localhost",
    private val port: Int = 8001,
    private val appName: String
) {
    private val queue = LinkedBlockingQueue<LogMessage>()
    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        bootstrap()
    }

    private fun bootstrap() {
        thread(start = true, isDaemon = true) {
            while (true) {
                try {
                    Socket(host, port).apply {
                        soTimeout = 5000 // Socket timeout
                    }.use { socket ->
                        val writer = PrintWriter(socket.getOutputStream(), true)
                        processMessages(writer)
                    }
                } catch (e: Exception) {
                    logger.error("Connection to Filebeat Failed...Retrying in 5 seconds.", e)
                    Thread.sleep(5000)
                }
            }
        }
    }

    private fun processMessages(writer: PrintWriter) {
        while (!Thread.currentThread().isInterrupted) {
            try {
                val loggingMessage = queue.take()
                val json = convert(loggingMessage)
                writer.println(json)
            } catch (e: Exception) {
                throw e // Propagate to outer loop for reconnection
            }
        }
    }

    private fun convert(logMessage: LogMessage): String {
        val stackTrace = logMessage.throwable?.stackTraceToString()?.replace("\"", "\\\"") ?: ""
        return """
            {
                "timestamp": "${logMessage.timestamp}",
                "level": "${logMessage.level}",
                "message": "${logMessage.message.replace("\"", "\\\"")}",
                "application": "$appName",
                "type": "log",
                ${if (stackTrace.isNotEmpty()) "\"stackTrace\": \"$stackTrace\"," else "Executed without any errors"}
            }
        """.trimIndent()
    }

    private fun log(level: String, message: String, throwable: Throwable? = null) {
        queue.offer(LogMessage(level, message, throwable = throwable))
    }

    fun info(message: String) = log(LogLevel.INFO, message)
    fun debug(message: String) = log(LogLevel.DEBUG, message)
    fun warning(message: String) = log(LogLevel.WARN, message)
    fun error(message: String, throwable: Throwable? = null) = log(LogLevel.ERROR, message, throwable)
}