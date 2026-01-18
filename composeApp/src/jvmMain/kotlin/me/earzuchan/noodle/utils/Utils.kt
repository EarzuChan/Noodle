package me.earzuchan.noodle.utils

import java.io.File
import javax.swing.SwingUtilities

actual object NLog {
    actual fun d(tag: String, vararg messages: Any?) = printLog("D", tag, messages)

    actual fun i(tag: String, vararg messages: Any?) = printLog("I", tag, messages)

    actual fun w(tag: String, vararg messages: Any?) = printLog("W", tag, messages)

    actual fun e(tag: String, vararg messages: Any?) = if (messages.isNotEmpty() && messages.last() is Throwable) {
        val throwable = messages.last() as Throwable

        // 拼接除了最后一个 Throwable 之外的所有信息
        println("[E] $tag > ${messages.dropLast(1).joinToString(" ") { it?.toString() ?: "null" }}")

        throwable.printStackTrace()  // 输出详细堆栈信息
    } else printLog("E", tag, messages)

    private fun printLog(level: String, tag: String, messages: Array<out Any?>) {
        println("[$level] $tag > ${messages.joinToString(" ") { it?.toString() ?: "null" }}")
    }
}

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual object MPFunctions {
    private const val TAG = "DesktopPlatformFunctions"

    // Data

    actual fun getAppFilesPath(): File = File(getAppDataPath(), "files")

    private fun getAppDataPath(): File = File(System.getProperty("user.home"), "app.simugala")

    // App Helper

    actual fun setupApp() {}

    actual fun stopApp(): Unit = exitAppMethod()

    lateinit var exitAppMethod: () -> Unit
}

object DesktopUtils {
    fun <T> runOnUiThread(block: () -> T): T {
        if (SwingUtilities.isEventDispatchThread()) return block()

        var error: Throwable? = null
        var result: T? = null

        SwingUtilities.invokeAndWait {
            try {
                result = block()
            } catch (e: Throwable) {
                error = e
            }
        }

        error?.also { throw it }

        @Suppress("UNCHECKED_CAST")
        return result as T
    }
}