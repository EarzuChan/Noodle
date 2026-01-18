package me.earzuchan.noodle

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.stack.Children
import me.earzuchan.noodle.di.noodleModule
import me.earzuchan.noodle.ui.NoodleTheme
import me.earzuchan.noodle.duties.AppDuty
import me.earzuchan.noodle.duties.LoginDuty
import me.earzuchan.noodle.duties.MainDuty
import me.earzuchan.noodle.duties.SplashDuty
import me.earzuchan.noodle.ui.navis.AppNavis
import me.earzuchan.noodle.ui.screens.LoginScreen
import me.earzuchan.noodle.ui.screens.MainScreen
import me.earzuchan.noodle.ui.screens.SplashScreen
import me.earzuchan.noodle.utils.MPFunctions
import org.koin.compose.KoinApplication

@Composable
fun NoodleApp(appDuty: AppDuty)  {
    val TAG = "NoodleApp"

    DisposableEffect(Unit) {
        MPFunctions.setupApp()

        onDispose { MPFunctions.stopApp() }
    }

    NoodleTheme {
        Surface {
            Children(appDuty.navStack, Modifier.fillMaxSize()) {
                when (val ins = it.instance) {
                    is MainDuty -> MainScreen(ins)

                    is LoginDuty -> LoginScreen(ins)

                    is SplashDuty -> SplashScreen()
                }
            }
        }
    }
}