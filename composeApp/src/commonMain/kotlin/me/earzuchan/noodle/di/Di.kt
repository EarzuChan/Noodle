package me.earzuchan.noodle.di

import lib.fetchmoodle.MoodleFetcher
import me.earzuchan.noodle.data.repositories.AppPreferenceRepository
import org.koin.dsl.module

val noodleModule = module {
    // 提供App偏好项仓库：需较早初始化
    single { AppPreferenceRepository() }

    // 提供TeleFetcher
    single { MoodleFetcher() }
}