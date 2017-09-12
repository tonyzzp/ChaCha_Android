package me.izzp.chacha.server

import com.google.protobuf.MessageLite
import me.izzp.chacha.Bytes2Int
import me.izzp.chacha.LogD
import me.izzp.chacha.ReadFull
import me.izzp.chacha.proto.Account
import me.izzp.chacha.proto.Friends
import me.izzp.chacha.proto.Message
import me.izzp.chacha.ui
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock

/**
 * Created by zzp on 2017-09-11.
 */


object Server {

    var state = State.Closed
        private set
    private var socket: Socket? = null
    private var inStream: InputStream? = null
    private var outStream: OutputStream? = null
    private var ip = ""
    private var port = 0
    private lateinit var stateChangeListener: () -> Unit
    private lateinit var processor: (cmd: Int, msg: MessageLite?) -> Unit
    private val sendingQueue = ConcurrentLinkedQueue<Packet>()
    private val lock = ReentrantLock()
    private val condition: Condition by lazy {
        lock.newCondition()
    }

    private val messageMap = mapOf(
            0x21001 to { bytes: ByteArray -> Account.LoginResponse.parseFrom(bytes) },
            0x21002 to { bytes: ByteArray -> Account.RegistResponse.parseFrom(bytes) },
            CMD_CLIENT_SEND_TEXT_MSG_RESP to { bytes: ByteArray -> Message.TextMessgeResponse.parseFrom(bytes) },
            0x22002 to { bytes: ByteArray -> Message.TextMessage.parseFrom(bytes) },
            0x23001 to { bytes: ByteArray -> Friends.SearchUserResp.parseFrom(bytes) },
            0x23002 to { bytes: ByteArray -> Friends.AddFriendResp.parseFrom(bytes) },
            0x23003 to { bytes: ByteArray -> Friends.AddFriend.parseFrom(bytes) },
            0x23004 to { bytes: ByteArray -> Friends.NewFriend.parseFrom(bytes) },
            0x23005 to { bytes: ByteArray -> Friends.FriendsList.parseFrom(bytes) },
            0x23006 to { bytes: ByteArray -> Friends.RemoveFriend.parseFrom(bytes) }
    )

    fun init(ip: String, port: Int) {
        this.ip = ip
        this.port = port
        this.stateChangeListener = Chatter::onStateChanged
        this.processor = Chatter::onMessageReceived
    }

    fun open() {
        if (state != State.Closed) {
            return
        }
        state = State.Connecting
        Thread {
            try {
                socket = Socket(ip, port)
                inStream = socket!!.getInputStream()
                outStream = socket!!.getOutputStream()
                onConnected()
            } catch (e: Exception) {
                e.printStackTrace()
                onFailed()
            }
        }.start()
    }

    fun close() {
        state = State.Closed
        socket?.close()
    }

    private fun onConnected() {
        state = State.Connected
        ui {
            stateChangeListener.invoke()
        }
        Thread {
            while (state == State.Connected) {
                val p = sendingQueue.poll()
                if (p != null) {
                    val bytes = p.toByteArray()
                    try {
                        outStream?.write(bytes)
                    } catch (e: Exception) {
                        onClosed()
                    }
                } else {
                    lock.lock()
                    condition.await()
                    lock.unlock()
                }
            }
        }.start()
        Thread {
            while (state == State.Connected) {
                val stream = inStream ?: return@Thread
                var msg: MessageLite?
                var cmd = 0
                var content: ByteArray? = null
                var ioerror = false
                try {
                    val len = Bytes2Int(ReadFull(stream, 4))
                    cmd = Bytes2Int(ReadFull(stream, 4))
                    content = ReadFull(stream, len)
                } catch (e: Exception) {
                    e.printStackTrace()
                    ioerror = true
                }
                if (ioerror) {
                    onClosed()
                } else {
                    msg = resolvePacket(cmd, content!!)
                    if (msg == null) {
                        LogD(String.format("有包未解析%x", cmd))
                    }
                    ui {
                        processor(cmd, msg)
                    }
                }
            }
        }.start()
    }

    private fun resolvePacket(cmd: Int, content: ByteArray): MessageLite? = messageMap[cmd]?.invoke(content)

    private fun onClosed() {
        if (state == State.Closed) {
            return
        }
        state = State.Closed
        sendingQueue.clear()
        lock.lock()
        condition.signalAll()
        lock.unlock()
        ui {
            stateChangeListener.invoke()
        }
    }

    private fun onFailed() {
        state = State.Closed
        sendingQueue.clear()
        ui {
            stateChangeListener.invoke()
        }
    }

    fun send(cmd: Int, msg: MessageLite?) {
        val p = Packet(cmd, msg)
        sendingQueue.offer(p)
        lock.lock()
        condition.signalAll()
        lock.unlock()
    }
}