package me.izzp.chacha

import me.izzp.chacha.proto.Message
import me.izzp.chacha.server.Chatter
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.concurrent.atomic.AtomicInteger

/**
 * Created by zzp on 2017-09-12.
 */
object Conversations {

    private val conversations = mutableMapOf<Int, MutableList<ChatMessage>>()
    private val sequenceId = AtomicInteger()
    private val sendingMsgs = mutableMapOf<Long, ChatMessage>()

    init {
        EventBus.getDefault().register(this)
    }

    fun getConversations(friendId: Int): MutableList<ChatMessage> {
        var list = conversations[friendId]
        if (list == null) {
            list = mutableListOf()
            conversations[friendId] = list
        }
        return list
    }

    fun sequence(): Long = System.currentTimeMillis() + sequenceId.incrementAndGet()

    fun sendTextMessage(msg: ChatMessage) {
        val list = getConversations(msg.receiver)
        list.add(msg)
        sendingMsgs[msg.sequence] = msg
        Chatter.sendTextMessage(msg.receiver, msg.content, msg.sequence)
    }

    fun retry(msg: ChatMessage) {
        msg.state = ChatMessage.State.sending
        sendingMsgs[msg.sequence] = msg
        Chatter.sendTextMessage(msg.receiver, msg.content, msg.sequence)
    }

    @Subscribe(priority = 1, threadMode = ThreadMode.MAIN)
    fun onTextMessageResponse(event: Message.TextMessgeResponse) {
        val msg = sendingMsgs[event.sequence]
        if (msg != null) {
            if (event.ok) {
                msg.state = ChatMessage.State.success
            } else {
                msg.state = ChatMessage.State.fail
                msg.error = event.error
            }
        }
    }
}