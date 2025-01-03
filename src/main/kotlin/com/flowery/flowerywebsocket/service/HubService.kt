package com.flowery.flowerywebsocket.service

import org.apache.tomcat.util.buf.ByteChunk.BufferOverflowException
import org.springframework.messaging.simp.SimpMessageSendingOperations
import org.springframework.stereotype.Service
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

/**
 *
 * Websocket Hub Service
 */
@Service
class HubService(
    private val messagingTemplate: SimpMessageSendingOperations
) {

    /**
     * A map to handle connections.
     * TODO: Need to change the format if ~:N connection is required.
     *  - In this case, the connections will be a ConcurrentHashMap<String, MutableSet<String>>.
     */
    private val connections = Collections.synchronizedList(mutableListOf<String>())

    /**
     * Buffer size = 1MB
     */
    private val bufSize = 1024 * 1024

    private val readBuf = ConcurrentHashMap<String, ByteBuffer>()
    private val writeBuf = ConcurrentHashMap<String, ByteBuffer>()

    private fun initBuf(uid: String) {
        readBuf[uid] = ByteBuffer.allocate(bufSize)
        writeBuf[uid] = ByteBuffer.allocate(bufSize)
    }

    private fun cleanBuf(uid: String) {
        readBuf.remove(uid)
        writeBuf.remove(uid)
    }

    fun joinConnection(uid: String) {
        if (!connections.contains(uid)) {
            connections.add(uid)
        }
        initBuf(uid)
    }

    fun leaveConnection(uid: String) {
        connections.remove(uid)
        cleanBuf(uid)
    }

    fun handleMessage(senderUid: String, receiverUid: String, message: String) {
        //* Handle Sender Uid
        writeToBuf(receiverUid, message.toByteArray(StandardCharsets.UTF_8))
        processBufferAndSend(receiverUid)
    }

    private fun writeToBuf(uid: String, payload: ByteArray) {
        writeBuf[uid]?.let { buf ->
            synchronized(buf){ //* Critical Section: Lock up the buffer
                if(buf.remaining() > payload.size){
                    buf.put(payload)
                } else {
                    //* Handle Buffer Overflow
                    //* TODO: Requires better exception handler
                    throw BufferOverflowException("ERROR: Write Buffer Overflow")
                }
            }
            this.flushWriteBuf(uid)
        }
    }

    private fun readFromBuf(uid: String): ByteArray {
        return this.flushReadBuf(uid)
    }

    private fun flushWriteBuf(uid: String) {
        writeBuf[uid]?.let { srcBuf ->
            readBuf[uid]?.let { destBuf ->
                srcBuf.flip() //* Read Mode: Move the write index to 0.
                synchronized(destBuf){ //* Critical Section: Lock up the buffer
                    destBuf.put(srcBuf)
                }
            }
            srcBuf.clear()
        }
    }

    private fun flushReadBuf(uid: String): ByteArray {
        return readBuf[uid]?.let { buf ->
            synchronized(buf) {
                buf.flip()
                val dest = ByteArray(buf.remaining())
                buf.get(dest)
                buf.clear()
                dest
            }
        } ?: ByteArray(0)
    }

    private fun processBufferAndSend(uid: String) {
        val data = readFromBuf(uid)

        data.let {
            val message = String(it, StandardCharsets.UTF_8)
            sendMessage(uid, message)
        }
    }
    private fun sendMessage(uid: String, message: String) {
        messagingTemplate.convertAndSend("/topic/messages/$uid", message)
    }
}