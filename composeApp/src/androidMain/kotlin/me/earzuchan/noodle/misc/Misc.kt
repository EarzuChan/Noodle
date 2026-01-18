package me.earzuchan.noodle.misc

import android.app.*

class AndroidApp : Application() {
    companion object {
        lateinit var appContext: AndroidApp
            private set
    }

    override fun onCreate() {
        super.onCreate()

        appContext = this
    }
}