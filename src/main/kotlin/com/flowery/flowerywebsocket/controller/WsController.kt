package com.flowery.flowerywebsocket.controller

import com.flowery.flowerywebsocket.dto.Message
import com.flowery.flowerywebsocket.service.HubService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.stereotype.Controller

@Controller
class WsController(
    @Autowired private val hubService: HubService
) {

    @MessageMapping("/join")
    fun join(uid: String) {
        hubService.joinConnection(uid)
    }

    @MessageMapping("/leave")
    fun leave(uid: String) {
        hubService.leaveConnection(uid)
    }

    @MessageMapping("/send")
    fun send(message: Message) {
        //* Handle Message Type
        hubService.handleMessage(message.sender, message.receiver, message.payload)
    }

}