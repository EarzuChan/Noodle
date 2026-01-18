package me.earzuchan.noodle.utils

import android.util.Log
import me.earzuchan.noodle.misc.AndroidApp
import java.io.File
import kotlin.system.exitProcess

actual object NLog {
    actual fun d(tag: String, vararg messages: Any?) {
        Log.d(tag, messages.toStr())
    }

    actual fun i(tag: String, vararg messages: Any?) {
        Log.i(tag, messages.toStr())
    }

    actual fun w(tag: String, vararg messages: Any?) {
        Log.w(tag, messages.toStr())
    }

    actual fun e(tag: String, vararg messages: Any?) {
        if (messages.isNotEmpty() && messages.last() is Throwable) {
            val throwable = messages.last() as Throwable

            // 拼接除了最后一个 Throwable 之外的所有信息
            val msg = messages.dropLast(1).joinToString(" ") { it?.toString() ?: "null" }

            Log.e(tag, msg, throwable)
        } else Log.e(tag, messages.toStr())
    }

    private fun Array<out Any?>.toStr() = joinToString(" ") { it?.toString() ?: "null" }
}

actual object MPFunctions {
    private const val TAG = "AndroidPlatformFunctions"

    // Data

    actual fun getAppFilesPath(): File = AndroidApp.appContext.filesDir // 在 /data/data/包名/files/ 下

    // App Helper

    actual fun setupApp() {}

    actual fun stopApp(): Unit = exitProcess(0)
}