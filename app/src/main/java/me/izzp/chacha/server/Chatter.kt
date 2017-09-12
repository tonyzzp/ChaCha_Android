package me.izzp.chacha.server

import com.google.protobuf.MessageLite
import me.izzp.chacha.LogD
import me.izzp.chacha.proto.Account
import me.izzp.chacha.proto.Friends
import me.izzp.chacha.proto.Message
import org.greenrobot.eventbus.EventBus

/**
 * Created by zzp on 2017-09-12.
 */
object Chatter {

    private var loginCb: ServerCallback? = null
    private var friendsListCb: ServerCallback? = null
    private var searchUserCb: ServerCallback? = null
    private var addFriendCb: ServerCallback? = null

    fun onStateChanged() {

    }

    fun onMessageReceived(cmd: Int, msg: MessageLite?) {
        LogD("收到消息:0x%x", cmd)
        if (cmd == CMD_CLIENT_LOGIN_RESP) {
            loginCb?.invoke(cmd, msg)
            loginCb = null
        } else if (cmd == CMD_CLIENT_FRIENDS_LIST_RESP) {
            friendsListCb?.invoke(cmd, msg)
            friendsListCb = null
        } else if (cmd == CMD_CLIENT_QUERY_USER_RESP) {
            searchUserCb?.invoke(cmd, msg)
            searchUserCb = null
        } else if (cmd == CMD_CLIENT_ADD_FRIEND_RESP) {
            addFriendCb?.invoke(cmd, msg)
            addFriendCb = null
        } else if (cmd == CMD_SERVER_NEW_FRIEND) {
            val msg = msg as Friends.NewFriend
            EventBus.getDefault().post(msg)
        } else if (cmd == CMD_CLIENT_SEND_TEXT_MSG_RESP) {
            val msg = msg as Message.TextMessgeResponse
            EventBus.getDefault().post(msg)
        }
    }

    fun login(userName: String, passWord: String, cb: ServerCallback) {
        loginCb = cb
        val bean = Account.Login.newBuilder().setUsername(userName).setPassword(passWord).build()
        Server.send(CMD_CLIENT_LOGIN, bean)
    }

    fun friendsList(cb: ServerCallback) {
        friendsListCb = cb
        Server.send(CMD_CLIENT_FRIENDS_LIST, null)
    }

    fun searchFriend(s: String, cb: ServerCallback) {
        searchUserCb = cb
        val bean = Friends.SearchUser.newBuilder().setUserName(s).build()
        Server.send(CMD_CLIENT_QUERY_USER, bean)
    }

    fun addFriend(id: Int, reason: String, cb: ServerCallback) {
        addFriendCb = cb
        val bean = Friends.AddFriend.newBuilder().setFriendId(id).setMessage(reason).build()
        Server.send(CMD_CLIENT_ADD_FRIEND, bean)
    }

    fun sendTextMessage(friendId: Int, s: String, sequence: Long) {
        val bean = Message.TextMessage.newBuilder()
                .setSequence(sequence)
                .setContent(s)
                .setSendTime(System.currentTimeMillis())
                .setReceiver(friendId)
                .build()
        Server.send(CMD_CLIENT_SEND_TEXT_MSG, bean)
    }
}