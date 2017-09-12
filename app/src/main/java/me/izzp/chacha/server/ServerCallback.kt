package me.izzp.chacha.server

import com.google.protobuf.MessageLite

/**
 * Created by zzp on 2017-09-12.
 */

typealias  ServerCallback = (cmd: Int, msg: MessageLite?) -> Unit