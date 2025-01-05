package com.flowery.flowerywebsocket.lib

import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentLinkedQueue

class MessageBuffer {
    private val writeBuf = ConcurrentLinkedQueue<ByteBuffer>()
    private val readBuf = ConcurrentLinkedQueue<ByteBuffer>()

    fun write(payload: ByteBuffer) {
        writeBuf.offer(payload)
    }

    fun read(): ByteBuffer? {
        return readBuf.poll()
    }

    fun process(){
        var payload = writeBuf.poll()
        while(payload!=null) {
            readBuf.offer(payload)
            payload = writeBuf.poll()
        }
    }

    fun purge() {
        writeBuf.clear()
        readBuf.clear()
    }
}