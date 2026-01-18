package me.earzuchan.noodle

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.decompose.extensions.compose.lifecycle.LifecycleController
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import lib.fetchmoodle.MoodleFetcher
import me.earzuchan.noodle.data.repositories.AppPreferenceRepository
import me.earzuchan.noodle.di.noodleModule
import me.earzuchan.noodle.duties.AppDuty
import me.earzuchan.noodle.utils.DesktopUtils
import me.earzuchan.noodle.utils.MPFunctions
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject

fun main() = application {
    KoinApplication({ modules(noodleModule) }) {
        val lifecycle = LifecycleRegistry()

        val appDuty = DesktopUtils.runOnUiThread { AppDuty(DefaultComponentContext(lifecycle)) }

        MPFunctions.exitAppMethod = ::exitApplication

        val windowState = rememberWindowState(size = DpSize(400.dp, 800.dp))
        LifecycleController(lifecycle, windowState)

        Window(MPFunctions::stopApp, windowState, title = "Noodle") {
            NoodleApp(appDuty)
        }
    }
}