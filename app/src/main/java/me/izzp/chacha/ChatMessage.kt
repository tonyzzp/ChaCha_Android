package me.izzp.chacha

import java.util.*

/**
 * Created by zzp on 2017-09-12.
 */
class ChatMessage {

    enum class State {
        new, sending, fail, success
    }

    var sequence = 0L
    var state = State.new
    var error = ""
    var sender = 0
    var receiver = 0
    var content = ""
    var sendTime = Date()
}