package me.izzp.chacha.server

import com.google.protobuf.MessageLite
import me.izzp.chacha.Int2Bytes

/**
 * Created by zzp on 2017-09-11.
 */

class Packet(private val cmd: Int, private val bean: MessageLite) {
    fun toByteArray(): ByteArray {
        val bytes = Int2Bytes(cmd)
        val content = bean.toByteArray()
        val len = Int2Bytes(content.size)
        val result = ByteArray(4 + 4 + content.size)
        System.arraycopy(len, 0, result, 0, 4)
        System.arraycopy(bytes, 0, result, 4, 4)
        System.arraycopy(content, 0, result, 8, content.size)
        return result
    }
}