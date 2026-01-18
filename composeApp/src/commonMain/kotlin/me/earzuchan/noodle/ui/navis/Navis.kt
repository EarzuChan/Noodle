package me.earzuchan.noodle.ui.navis

import kotlinx.serialization.Serializable


@Serializable
sealed class AppNavis {
    @Serializable
    data object Main : AppNavis()

    @Serializable
    data object Login : AppNavis()

    @Serializable
    data object Splash : AppNavis()
}