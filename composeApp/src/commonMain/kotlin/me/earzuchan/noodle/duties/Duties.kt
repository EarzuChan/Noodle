package me.earzuchan.noodle.duties

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.replaceAll
import com.arkivanov.decompose.value.Value
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import lib.fetchmoodle.MoodleFetcher
import lib.fetchmoodle.MoodleResult
import me.earzuchan.noodle.data.preferences.AppPreferences
import me.earzuchan.noodle.data.repositories.AppPreferenceRepository
import me.earzuchan.noodle.ui.navis.AppNavis
import me.earzuchan.noodle.utils.MiscUtils.ioDispatcherLaunch
import me.earzuchan.noodle.utils.MiscUtils.mainDispatcherLaunch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class AppDuty(ctx: ComponentContext) : ComponentContext by ctx, KoinComponent {
    val moodleFetcher: MoodleFetcher by inject()

    val appPrefRepo: AppPreferenceRepository by inject()

    init {
        ioDispatcherLaunch {
            combine(appPrefRepo.baseSite, appPrefRepo.username, appPrefRepo.password) { site, user, pwd -> Triple(site, user, pwd) }.collectLatest { (site, user, pwd) ->
                val isValid = site.isNotEmpty() && user.isNotEmpty() && pwd.isNotEmpty() && moodleFetcher.login("https://$site", user, pwd) is MoodleResult.Success

                mainDispatcherLaunch {
                    if (isValid) navigation.replaceAll(AppNavis.Main)
                    else navigation.replaceAll(AppNavis.Login)
                }
            }
        }
    }

    private val navigation = StackNavigation<AppNavis>()

    val navStack: Value<ChildStack<*, ComponentContext>> = childStack(navigation, AppNavis.serializer(), AppNavis.Splash, "MainStack", false, ::mapDuties)

    private fun mapDuties(navi: AppNavis, subCtx: ComponentContext): ComponentContext = when (navi) {
        is AppNavis.Splash -> SplashDuty(subCtx)

        is AppNavis.Main -> MainDuty(subCtx) { navigation.replaceAll(AppNavis.Login) }

        is AppNavis.Login -> LoginDuty(subCtx) { navigation.replaceAll(AppNavis.Main) }
    }
}

class SplashDuty(ctx: ComponentContext) : ComponentContext by ctx

class MainDuty(ctx: ComponentContext, val onLogout: () -> Unit) : ComponentContext by ctx, KoinComponent {
    private val moodleFetcher: MoodleFetcher by inject()
    private val appPrefRepo: AppPreferenceRepository by inject()

    sealed interface UIState {
        object Loading : UIState
        data class Success(val data: Map<String, String>) : UIState
        data class Error(val msg: String) : UIState
    }

    private val _state = MutableStateFlow<UIState>(UIState.Loading)
    val state: StateFlow<UIState> = _state

    init {
        loadGrades()
    }

    fun loadGrades() {
        ioDispatcherLaunch {
            _state.value = UIState.Loading

            when (val result = moodleFetcher.getGrades()) {
                is MoodleResult.Success -> _state.value = UIState.Success(result.data)
                is MoodleResult.Failure -> _state.value = UIState.Error(result.exception.message ?: "成绩加载失败")
            }
        }
    }

    fun logout() {
        moodleFetcher.clearSessionData()

        ioDispatcherLaunch {
            appPrefRepo.setBaseSite(AppPreferences.DEFAULT_BASE_SITE)
            appPrefRepo.setUsername("")
            appPrefRepo.setPassword("")

            mainDispatcherLaunch {
                onLogout()
            }
        }
    }
}

class LoginDuty(ctx: ComponentContext, private val onLoginSuccess: () -> Unit) : ComponentContext by ctx, KoinComponent {
    private val moodleFetcher: MoodleFetcher by inject()
    private val appPrefRepo: AppPreferenceRepository by inject()

    // UI 状态
    val baseSite = MutableStateFlow("")
    val username = MutableStateFlow("")
    val password = MutableStateFlow("")
    val errorMessage = MutableStateFlow<String?>(null)
    val isLoading = MutableStateFlow(false)

    init {
        ioDispatcherLaunch {
            baseSite.value = appPrefRepo.baseSite.first()
            username.value = appPrefRepo.username.first()
            password.value = appPrefRepo.password.first()
        }
    }

    fun onLoginClick() {
        val baseSite = baseSite.value
        val user = username.value
        val pwd = password.value

        if (baseSite.isBlank() || user.isBlank() || pwd.isBlank()) {
            errorMessage.value = "站点、用户名、密码不能为空"
            return
        }

        ioDispatcherLaunch {
            isLoading.value = true
            errorMessage.value = null

            when (val result = moodleFetcher.login("https://$baseSite", user, pwd)) {
                is MoodleResult.Success -> {
                    appPrefRepo.setBaseSite(baseSite)
                    appPrefRepo.setUsername(user)
                    appPrefRepo.setPassword(pwd)

                    mainDispatcherLaunch { onLoginSuccess() }
                }

                is MoodleResult.Failure -> errorMessage.value = "登录失败：" + (result.exception.message ?: "未知错误")
            }

            isLoading.value = false
        }
    }
}
