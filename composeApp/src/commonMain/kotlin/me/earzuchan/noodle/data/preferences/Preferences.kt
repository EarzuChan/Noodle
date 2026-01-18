package me.earzuchan.noodle.data.preferences

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object AppPreferences {
    val KEY_USERNAME = stringPreferencesKey("username")
    val KEY_PASSWORD = stringPreferencesKey("password")
}