package me.izzp.chacha

import android.app.Application
import kotlin.properties.Delegates

/**
 * Created by zzp on 2017-09-12.
 */
class App : Application() {
    companion object {
        var app: App by Delegates.notNull()
    }

    override fun onCreate() {
        super.onCreate()
        app = this
    }
}