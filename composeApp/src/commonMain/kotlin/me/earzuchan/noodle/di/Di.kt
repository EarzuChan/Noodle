package me.earzuchan.noodle.di

import lib.fetchmoodle.MoodleFetcher
import lib.fetchmoodle.MoodleFetcherConfig
import me.earzuchan.noodle.data.repositories.AppPreferenceRepository
import me.earzuchan.noodle.utils.MiscUtils
import org.koin.dsl.module

val noodleModule = module {
    // 提供App偏好项仓库：需较早初始化
    single { AppPreferenceRepository() }

    /*// 提供数据库
    single { MiscUtils.buildAppDatabase() }*/

    // 提供TeleFetcher
    single { MoodleFetcher(MoodleFetcherConfig("https://moodle.hainan-biuh.edu.cn")) }
}