package me.earzuchan.noodle.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.earzuchan.noodle.data.APP_PREFERENCES_NAME
import okio.Path.Companion.toOkioPath
import org.jetbrains.compose.resources.*
import java.io.File

expect object NLog {
    fun d(tag: String, vararg messages: Any?)
    fun i(tag: String, vararg messages: Any?)
    fun w(tag: String, vararg messages: Any?)
    fun e(tag: String, vararg messages: Any?)
}

expect object MPFunctions {
    /*fun getAppDatabaseBuilder(): RoomDatabase.Builder<AppDatabase>*/

    fun getAppFilesPath(): File

    // App Helper
    fun setupApp()

    fun stopApp()
}

object MiscUtils {
    private const val TAG = "MiscUtils"

    // 每次新建
    /*fun buildAppDatabase(): AppDatabase = MPFunctions.getAppDatabaseBuilder()
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()*/

    fun buildAppPreferences(): DataStore<Preferences> = PreferenceDataStoreFactory.createWithPath(
        produceFile = { File(MPFunctions.getAppFilesPath(), APP_PREFERENCES_NAME).toOkioPath() }
    )

    // 预制菜
    val defaultDispatcherScope = CoroutineScope(Dispatchers.Default)
    val ioDispatcherScope = CoroutineScope(Dispatchers.IO)
    val mainDispatcherScope = CoroutineScope(Dispatchers.Main)

    // 使用默认的协程上下文来启动任务
    fun defaultDispatcherLaunch(task: suspend CoroutineScope.() -> Unit) = defaultDispatcherScope.launch(block = task)

    // 使用IO协程上下文来启动任务
    fun ioDispatcherLaunch(task: suspend CoroutineScope.() -> Unit) = ioDispatcherScope.launch(block = task)

    fun mainDispatcherLaunch(task: suspend CoroutineScope.() -> Unit) = mainDispatcherScope.launch(block = task)
}

object ComposeUtils {
    @Composable
    inline fun Modifier.only(
        condition: Boolean,
        elseBlock: @Composable Modifier.() -> Modifier = { this },
        ifBlock: @Composable Modifier.() -> Modifier
    ): Modifier = if (condition) ifBlock() else elseBlock()

    inline fun Color.opacity(opacity: Float): Color {
        val newAlpha = alpha * opacity
        return this.copy(newAlpha)
    }

    inline val Int.dpPx: Float
        @Composable
        get() = this.dp.px

    inline val Dp.px: Float
        @Composable
        get() = LocalDensity.current.run { this@px.toPx() }
}

object ResUtils {
    val DrawableResource.vector
        @Composable
        get() = vectorResource(this)

    val DrawableResource.image
        @Composable
        get() = imageResource(this)


    @Composable
    fun StringResource.text(vararg format: String): String = stringResource(this, *format)

    val StringResource.text
        @Composable
        get() = this.text()
}