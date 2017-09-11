package me.izzp.chacha

import me.izzp.chacha.proto.Account
import me.izzp.chacha.server.Packet
import org.junit.Test

/**
 * Created by zzp on 2017-09-08.
 */

class TestProto {
    @Test
    fun test_proto() {
        val cmd = 0x1001
        val msg = Account.Login.newBuilder().setUsername("zzp").setPassword("123").build()
        val bytes = Packet(cmd, msg).toByteArray()
        println(bytes.contentToString())
    }
}