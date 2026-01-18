package me.earzuchan.noodle.data.preferences

import androidx.datastore.preferences.core.stringPreferencesKey

object AppPreferences {
    val KEY_BASE_SITE = stringPreferencesKey("base_site")
    val KEY_USERNAME = stringPreferencesKey("username")
    val KEY_PASSWORD = stringPreferencesKey("password")

    const val DEFAULT_BASE_SITE = "moodle.hainan-biuh.edu.cn"
}