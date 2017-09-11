package me.izzp.chacha.server

import com.google.protobuf.MessageLite
import com.google.protobuf.MessageLiteOrBuilder
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
import kotlin.reflect.KClass

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
    private lateinit var processor: (cmd: Int, msg: MessageLite) -> Unit
    private val sendingQueue = ConcurrentLinkedQueue<Packet>()
    private val lock = ReentrantLock()
    private val condition: Condition by lazy {
        lock.newCondition()
    }
    private val messageMap: Map<Int, KClass<out MessageLiteOrBuilder>> = mapOf(
            0X21001 to Account.LoginResponse::class,
            0x21002 to Account.RegistResponse::class,
            0x22001 to Message.TextMessageOrBuilder::class,
            0x22002 to Message.TextMessage::class,
            0x23001 to Friends.SearchUserResp::class,
            0x23002 to Friends.AddFriendResp::class,
            0x23003 to Friends.AddFriend::class,
            0x23004 to Friends.NewFriend::class,
            0x23005 to Friends.FriendsList::class,
            0x23006 to Friends.RemoveFriend::class
    )

    fun init(ip: String, port: Int, stateChangeListener: () -> Unit, processor: (cmd: Int, msg: MessageLite) -> Unit) {
        this.ip = ip
        this.port = port
        this.stateChangeListener = stateChangeListener
        this.processor = processor
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
                var msg: MessageLite? = null
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
                    } else {
                        ui {
                            processor(cmd, msg!!)
                        }
                    }
                }
            }
        }.start()
    }

    private fun resolvePacket(cmd: Int, content: ByteArray): MessageLite? {
        val msg = messageMap[cmd]?.java?.getDeclaredMethod("parseFrom", ByteArray::class.java)?.invoke(null, content)
        return msg as MessageLite?
    }

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

    fun send(packet: Packet) {
        sendingQueue.offer(packet)
        lock.lock()
        condition.signalAll()
        lock.unlock()
    }
}