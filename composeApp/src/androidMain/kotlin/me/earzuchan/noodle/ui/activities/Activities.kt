package me.earzuchan.noodle.ui.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.arkivanov.decompose.defaultComponentContext
import lib.fetchmoodle.MoodleFetcher
import me.earzuchan.noodle.NoodleApp
import me.earzuchan.noodle.data.repositories.AppPreferenceRepository
import me.earzuchan.noodle.di.noodleModule
import me.earzuchan.noodle.duties.AppDuty
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject

class InitAppActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        enableEdgeToEdge()

        setContent {
            KoinApplication({ modules(noodleModule) }) {
                val appDuty = AppDuty(defaultComponentContext())
                NoodleApp(appDuty)
            }
        }
    }
}