package me.izzp.chacha

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import java.io.InputStream

/**
 * Created by zzp on 2017-09-08.
 */


private val H: Handler by lazy {
    Handler(Looper.getMainLooper())
}

fun ui(block: () -> Unit) {
    H.post(block)
}

fun Bytes2Int(bytes: ByteArray): Int {
    val a = bytes[0].toInt() and 0xff shl 24
    val b = bytes[1].toInt() and 0xff shl 16
    val c = bytes[2].toInt() and 0xff shl 8
    val d = bytes[3].toInt() and 0xff
    return a or b or c or d
}

fun Int2Bytes(i: Int): ByteArray {
    val bytes = ByteArray(4)
    bytes[0] = (i ushr 24 and 0xff).toByte()
    bytes[1] = (i ushr 16 and 0xff).toByte()
    bytes[2] = (i ushr 8 and 0xff).toByte()
    bytes[3] = (i and 0xff).toByte()
    return bytes
}

fun ReadFull(inStream: InputStream, size: Int): ByteArray {
    val buf = ByteArray(size)
    var remain = size
    while (remain > 0) {
        val len = inStream.read(buf, size - remain, remain)
        remain -= len
    }
    return buf
}

fun LogD(format: Any?, vararg objs: Any) {
    if (format is String) {
        Log.d("ChaCha", format.format(*objs))
    } else {
        Log.d("ChaCha", format.toString())
    }
}

fun Context.toast(s: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, s, duration).show()
}