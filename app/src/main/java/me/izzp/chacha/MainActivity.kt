package me.izzp.chacha

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.google.protobuf.MessageLite
import me.izzp.chacha.proto.Account
import me.izzp.chacha.server.Packet
import me.izzp.chacha.server.Server
import me.izzp.chacha.server.State

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun onTestLoginClick(view: View) {

        val onStateChanged = fun() {
            LogD("状态改变:" + Server.state)
            if (State.Connected == Server.state) {
                val login = Account.Login.newBuilder().setUsername("zzp").setPassword("123").build()
                Server.send(Packet(0x11001, login))
            }
        }

        val onMessageReceive = fun(cmd: Int, msg: MessageLite) {
            LogD("收到消息:%d", cmd)
            if (cmd == 0X21001) {
                java.lang.String.format("")
                val msg = msg as Account.LoginResponse
                toast(String.format("%b  %s", msg.ok, msg.error))
            }
        }

        Server.init("172.27.35.1", 2626, onStateChanged, onMessageReceive)


        Server.open()
    }
}