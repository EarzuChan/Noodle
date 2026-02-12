package me.earzuchan.markdo.services

import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import lib.fetchmoodle.LoginFailure
import lib.fetchmoodle.MoodleCourseInfo
import lib.fetchmoodle.MoodleFetcher
import lib.fetchmoodle.MoodleLoginFailureException
import lib.fetchmoodle.MoodleRecentItem
import lib.fetchmoodle.MoodleResult
import lib.fetchmoodle.MoodleTimelineEvent
import lib.fetchmoodle.MoodleUserProfile
import me.earzuchan.markdo.data.models.LoginDraft
import me.earzuchan.markdo.data.models.SavedLoginAccount
import me.earzuchan.markdo.data.repositories.AccountRepository
import me.earzuchan.markdo.data.repositories.DataCacheRepository
import me.earzuchan.markdo.utils.MarkDoLog
import me.earzuchan.markdo.utils.MiscUtils.ioDispatcherLaunch
import me.earzuchan.markdo.utils.MiscUtils.ioDispatcherScope

class MoodleService(
    private val moodleFetcher: MoodleFetcher,
    private val accountRepo: AccountRepository,
    private val cacheRepo: DataCacheRepository
) {
    sealed class AuthState {
        data object Initial : AuthState()
        data object Busy : AuthState()
        data object Authed : AuthState()
        data class Unauthed(val reason: String) : AuthState()
    }

    sealed interface BootstrapRoute {
        data object Main : BootstrapRoute
        data object SplashAndLogin : BootstrapRoute
        data object Login : BootstrapRoute
    }

    sealed interface LoginConnectionState {
        data object Unknown : LoginConnectionState
        data object Onlined : LoginConnectionState
        data object Offlined : LoginConnectionState
        data object Onlining : LoginConnectionState
    }

    val authState = MutableStateFlow<AuthState>(AuthState.Initial)

    val userProfile = MutableStateFlow<MoodleUserProfile?>(null)

    sealed interface TimelineState {
        data object Loading : TimelineState
        data class Success(val data: List<MoodleTimelineEvent>) : TimelineState
        data class Error(val msg: String) : TimelineState
    }

    val timelineState = MutableStateFlow<TimelineState>(TimelineState.Loading)

    sealed interface RecentItemsState {
        data object Loading : RecentItemsState
        data class Success(val data: List<MoodleRecentItem>) : RecentItemsState
        data class Error(val msg: String) : RecentItemsState
    }

    val recentItemsState = MutableStateFlow<RecentItemsState>(RecentItemsState.Loading)

    sealed interface CoursesState {
        data object Loading : CoursesState
        data class Success(val data: List<MoodleCourseInfo>) : CoursesState
        data class Error(val msg: String) : CoursesState
    }

    val coursesState = MutableStateFlow<CoursesState>(CoursesState.Loading)

    val rememberedAccounts = MutableStateFlow<List<SavedLoginAccount>>(emptyList())
    val activeAccountKey = MutableStateFlow<String?>(null)
    val loginConnectionState = MutableStateFlow<LoginConnectionState>(LoginConnectionState.Unknown)

    private var currentAccountKey: String? = null
    private var retryLoginJob: Job? = null

    companion object {
        private const val TAG = "MoodleService"
        private const val RETRY_LOGIN_INTERVAL_MS = 30_000L

        const val AUTH_MSG_NO_LOGIN_INFO = "缺少可用登录账号"
        const val AUTH_MSG_INVALID_CREDENTIALS = "账号或密码错误"
        const val AUTH_MSG_NETWORK = "网络异常，暂时无法登录"
        const val AUTH_MSG_UNKNOWN = "登录失败：未知原因"
        const val AUTH_MSG_USER_LOGOUT = "用户主动退出登录"
    }

    suspend fun bootstrap(): BootstrapRoute {
        refreshRememberedAccountState()

        val hasRememberedAccount = rememberedAccounts.value.isNotEmpty()

        if (!hasRememberedAccount) {
            if (cacheRepo.hasAnyCache()) cacheRepo.clearAllCaches()
            onLogoutted()
            currentAccountKey = null
            activeAccountKey.value = null
            markConnectionUnknown()
            return BootstrapRoute.Login
        }

        val activeAccount = accountRepo.getActiveAccount() ?: run {
            if (cacheRepo.hasAnyCache()) cacheRepo.clearAllCaches()
            onLogoutted()
            currentAccountKey = null
            activeAccountKey.value = null
            markConnectionUnknown()
            return BootstrapRoute.Login
        }

        currentAccountKey = activeAccount.accountKey
        activeAccountKey.value = activeAccount.accountKey

        val hasCacheForActive = cacheRepo.hasAnyCacheForAccount(activeAccount.accountKey)
        return if (hasCacheForActive) {
            loadFromLocalCache(activeAccount.accountKey)
            settleStatesForOfflineView()
            markOfflineCached()
            BootstrapRoute.Main
        } else {
            onLogoutted()
            markConnectionUnknown()
            BootstrapRoute.SplashAndLogin
        }
    }

    suspend fun autoLogin(allowOfflineFallback: Boolean) {
        if (authState.value is AuthState.Busy) return

        refreshRememberedAccountState()

        val activeAccount = accountRepo.getActiveAccount()
        if (activeAccount == null) {
            moodleFetcher.clearSessionData()
            currentAccountKey = null
            activeAccountKey.value = null
            onLogoutted()
            markConnectionUnknown()
            authState.value = AuthState.Unauthed(AUTH_MSG_NO_LOGIN_INFO)
            return
        }

        authState.value = AuthState.Busy
        currentAccountKey = activeAccount.accountKey

        val accountKey = activeAccount.accountKey
        val hasLocalCache = cacheRepo.hasAnyCacheForAccount(accountKey)
        if (allowOfflineFallback && hasLocalCache) markOnlining()

        when (val result = moodleFetcher.login(activeAccount.baseSite.toBaseUrl(), activeAccount.username, activeAccount.password)) {
            is MoodleResult.Success -> {
                onLoginned(accountKey, refreshRemote = true)
                markOnline()
                authState.value = AuthState.Authed
            }

            is MoodleResult.Failure -> {
                when (val reason = resolveLoginFailure(result.exception)) {
                    is LoginFailureReason.InvalidCredentials -> {
                        moodleFetcher.clearSessionData()
                        cacheRepo.clearAccountCaches(accountKey)
                        accountRepo.clearActiveAccount()
                        currentAccountKey = null
                        activeAccountKey.value = null
                        refreshRememberedAccountState()
                        onLogoutted()
                        markConnectionUnknown()
                        authState.value = AuthState.Unauthed(AUTH_MSG_INVALID_CREDENTIALS)
                    }

                    is LoginFailureReason.Network -> {
                        if (allowOfflineFallback && hasLocalCache) {
                            MarkDoLog.w(TAG, "自动登录网络失败，回落到缓存模式")
                            onLoginned(accountKey, refreshRemote = false)
                            markOfflineCached()
                            authState.value = AuthState.Authed
                        } else {
                            onLogoutted()
                            markConnectionUnknown()
                            authState.value = AuthState.Unauthed(AUTH_MSG_NETWORK)
                        }
                    }

                    is LoginFailureReason.Unknown -> {
                        onLogoutted()
                        markConnectionUnknown()
                        authState.value = AuthState.Unauthed(reason.message ?: AUTH_MSG_UNKNOWN)
                    }
                }
            }
        }
    }

    suspend fun manualLogin(site: String, user: String, pwd: String) {
        if (authState.value is AuthState.Busy) return

        authState.value = AuthState.Busy

        val normalizedSite = AccountRepository.normalizeSite(site)
        val normalizedUser = user.trim()

        if (normalizedSite.isBlank() || normalizedUser.isBlank() || pwd.isBlank()) {
            authState.value = AuthState.Unauthed(AUTH_MSG_NO_LOGIN_INFO)
            return
        }

        when (val result = moodleFetcher.login(normalizedSite.toBaseUrl(), normalizedUser, pwd)) {
            is MoodleResult.Success -> {
                val account = accountRepo.saveSuccessfulLogin(normalizedSite, normalizedUser, pwd)
                currentAccountKey = account.accountKey
                refreshRememberedAccountState()
                onLoginned(account.accountKey, refreshRemote = true)
                markOnline()
                authState.value = AuthState.Authed
            }

            is MoodleResult.Failure -> {
                authState.value = when (val reason = resolveLoginFailure(result.exception)) {
                    is LoginFailureReason.InvalidCredentials -> AuthState.Unauthed(AUTH_MSG_INVALID_CREDENTIALS)
                    is LoginFailureReason.Network -> AuthState.Unauthed(AUTH_MSG_NETWORK)
                    is LoginFailureReason.Unknown -> AuthState.Unauthed(reason.message ?: AUTH_MSG_UNKNOWN)
                }
                markConnectionUnknown()
            }
        }
    }

    suspend fun logout() {
        if (authState.value is AuthState.Busy) return

        authState.value = AuthState.Busy

        moodleFetcher.clearSessionData()
        accountRepo.clearActiveAccount()
        currentAccountKey = null
        activeAccountKey.value = null
        refreshRememberedAccountState()

        onLogoutted()
        markConnectionUnknown()
        authState.value = AuthState.Unauthed(AUTH_MSG_USER_LOGOUT)
    }

    suspend fun getRememberedAccounts(): List<SavedLoginAccount> {
        refreshRememberedAccountState()
        return rememberedAccounts.value
    }

    suspend fun getPreferredLoginDraft(): LoginDraft = accountRepo.getPreferredLoginDraft()

    suspend fun getLoginDraftByAccountKey(accountKey: String): LoginDraft? = accountRepo.getLoginDraftByAccountKey(accountKey)

    suspend fun switchAccount(accountKey: String) {
        if (authState.value is AuthState.Busy) return

        val account = accountRepo.getAccountByKey(accountKey) ?: return

        accountRepo.setActiveAccount(account.accountKey)
        moodleFetcher.clearSessionData()
        currentAccountKey = account.accountKey
        refreshRememberedAccountState()

        val hasLocalCache = cacheRepo.hasAnyCacheForAccount(account.accountKey)
        if (hasLocalCache) {
            clearUserProfile()
            clearTimeline()
            clearRecentItems()
            clearCourses()
            loadFromLocalCache(account.accountKey)
            settleStatesForOfflineView()
            markOfflineCached()
            authState.value = AuthState.Authed
        } else {
            onLogoutted()
            markConnectionUnknown()
        }

        autoLogin(allowOfflineFallback = hasLocalCache)
    }

    suspend fun removeRememberedAccount(accountKey: String): Boolean {
        val removed = accountRepo.removeRememberedAccount(accountKey)
        if (!removed) return false

        cacheRepo.clearAccountCaches(accountKey)

        refreshRememberedAccountState()

        return true
    }

    suspend fun retryLoginNow() {
        if (authState.value !is AuthState.Authed) return

        val hasLocalCache = resolveCurrentAccountKey()?.let { cacheRepo.hasAnyCacheForAccount(it) } ?: false
        autoLogin(allowOfflineFallback = hasLocalCache)
    }

    private fun onLoginned(accountKey: String, refreshRemote: Boolean) {
        ioDispatcherLaunch {
            currentAccountKey = accountKey
            loadFromLocalCache(accountKey)

            if (refreshRemote) refreshAllRemote()
            else settleStatesForOfflineView()
        }
    }

    private suspend fun refreshAllRemote() = coroutineScope {
        launch { refreshUserProfile() }
        launch { refreshTimeline() }
        launch { refreshRecentItems() }
        launch { refreshCourses() }
    }

    private suspend fun loadFromLocalCache(accountKey: String) {
        cacheRepo.readCachedUserProfile(accountKey)?.let { userProfile.value = it }

        val timeline = cacheRepo.readCachedTimeline(accountKey)
        if (timeline.isNotEmpty()) timelineState.value = TimelineState.Success(timeline)

        val recentItems = cacheRepo.readCachedRecentItems(accountKey)
        if (recentItems.isNotEmpty()) recentItemsState.value = RecentItemsState.Success(recentItems)

        val courses = cacheRepo.readCachedCourses(accountKey)
        if (courses.isNotEmpty()) coursesState.value = CoursesState.Success(courses)
    }

    private fun settleStatesForOfflineView() {
        if (timelineState.value is TimelineState.Loading) timelineState.value = TimelineState.Success(emptyList())
        if (recentItemsState.value is RecentItemsState.Loading) recentItemsState.value = RecentItemsState.Success(emptyList())
        if (coursesState.value is CoursesState.Loading) coursesState.value = CoursesState.Success(emptyList())
    }

    private fun onLogoutted() {
        clearUserProfile()
        clearTimeline()
        clearRecentItems()
        clearCourses()
    }

    suspend fun refreshUserProfile(): Boolean {
        val accountKey = resolveCurrentAccountKey() ?: return false

        return when (val result = moodleFetcher.getUserProfile()) {
            is MoodleResult.Success -> {
                cacheRepo.replaceUserProfile(accountKey, result.data)
                userProfile.value = result.data
                true
            }

            is MoodleResult.Failure -> {
                if (userProfile.value == null) {
                    cacheRepo.readCachedUserProfile(accountKey)?.let {
                        userProfile.value = it
                        return true
                    }
                }

                false
            }
        }
    }

    suspend fun refreshTimeline() {
        val accountKey = resolveCurrentAccountKey() ?: return

        when (val result = moodleFetcher.getTimeline()) {
            is MoodleResult.Success -> {
                cacheRepo.replaceTimeline(accountKey, result.data)
                timelineState.value = TimelineState.Success(result.data)
            }

            is MoodleResult.Failure -> {
                if (timelineState.value !is TimelineState.Success) {
                    val localData = cacheRepo.readCachedTimeline(accountKey)
                    timelineState.value = if (localData.isNotEmpty()) TimelineState.Success(localData) else TimelineState.Error(result.exception.message ?: "未知")
                }
            }
        }
    }

    suspend fun refreshRecentItems() {
        val accountKey = resolveCurrentAccountKey() ?: return

        when (val result = moodleFetcher.getRecentItems()) {
            is MoodleResult.Success -> {
                cacheRepo.replaceRecentItems(accountKey, result.data)
                recentItemsState.value = RecentItemsState.Success(result.data)
            }

            is MoodleResult.Failure -> {
                if (recentItemsState.value !is RecentItemsState.Success) {
                    val localData = cacheRepo.readCachedRecentItems(accountKey)
                    recentItemsState.value = if (localData.isNotEmpty()) RecentItemsState.Success(localData) else RecentItemsState.Error(result.exception.message ?: "未知")
                }
            }
        }
    }

    suspend fun refreshCourses() {
        val accountKey = resolveCurrentAccountKey() ?: return

        when (val result = moodleFetcher.getCourses()) {
            is MoodleResult.Success -> {
                cacheRepo.replaceCourses(accountKey, result.data)
                coursesState.value = CoursesState.Success(result.data)
            }

            is MoodleResult.Failure -> {
                if (coursesState.value !is CoursesState.Success) {
                    val localData = cacheRepo.readCachedCourses(accountKey)
                    coursesState.value = if (localData.isNotEmpty()) CoursesState.Success(localData) else CoursesState.Error(result.exception.message ?: "未知")
                }
            }
        }
    }

    fun clearUserProfile() {
        userProfile.value = null
    }

    fun clearTimeline() {
        timelineState.value = TimelineState.Loading
    }

    fun clearRecentItems() {
        recentItemsState.value = RecentItemsState.Loading
    }

    fun clearCourses() {
        coursesState.value = CoursesState.Loading
    }

    private suspend fun resolveCurrentAccountKey(): String? {
        currentAccountKey?.let { return it }

        val activeAccountKey = accountRepo.getActiveAccount()?.accountKey
        currentAccountKey = activeAccountKey
        return activeAccountKey
    }

    private fun resolveLoginFailure(exception: Exception): LoginFailureReason {
        val loginFailureException = exception as? MoodleLoginFailureException
        val failure = loginFailureException?.failure

        return when (failure) {
            is LoginFailure.InvalidCredentials -> LoginFailureReason.InvalidCredentials
            is LoginFailure.Network -> LoginFailureReason.Network
            is LoginFailure.Unknown -> LoginFailureReason.Unknown(failure.message)
            null -> LoginFailureReason.Unknown(exception.message)
        }
    }

    private fun String.toBaseUrl(): String {
        val normalized = AccountRepository.normalizeSite(this)
        return "https://$normalized"
    }

    private sealed interface LoginFailureReason {
        data object InvalidCredentials : LoginFailureReason
        data object Network : LoginFailureReason
        data class Unknown(val message: String?) : LoginFailureReason
    }

    private fun markOnline() {
        loginConnectionState.value = LoginConnectionState.Onlined
        stopRetryLoginLoop()
    }

    private fun markOfflineCached() {
        loginConnectionState.value = LoginConnectionState.Offlined
        startRetryLoginLoop()
    }

    private fun markOnlining() {
        loginConnectionState.value = LoginConnectionState.Onlining
    }

    private fun markConnectionUnknown() {
        loginConnectionState.value = LoginConnectionState.Unknown
        stopRetryLoginLoop()
    }

    private fun startRetryLoginLoop() {
        if (retryLoginJob?.isActive == true) return

        retryLoginJob = ioDispatcherScope.launch {
            while (isActive) {
                delay(RETRY_LOGIN_INTERVAL_MS)

                if (loginConnectionState.value != LoginConnectionState.Offlined) continue
                if (authState.value !is AuthState.Authed) continue

                retryLoginInBackground()
            }
        }
    }

    private fun stopRetryLoginLoop() {
        retryLoginJob?.cancel()
        retryLoginJob = null
    }

    private suspend fun retryLoginInBackground() {
        val activeAccount = accountRepo.getActiveAccount() ?: run {
            markConnectionUnknown()
            return
        }

        markOnlining()

        when (val result = moodleFetcher.login(activeAccount.baseSite.toBaseUrl(), activeAccount.username, activeAccount.password)) {
            is MoodleResult.Success -> {
                currentAccountKey = activeAccount.accountKey
                onLoginned(activeAccount.accountKey, refreshRemote = true)
                authState.value = AuthState.Authed
                markOnline()
            }

            is MoodleResult.Failure -> {
                when (val reason = resolveLoginFailure(result.exception)) {
                    is LoginFailureReason.InvalidCredentials -> {
                        moodleFetcher.clearSessionData()
                        cacheRepo.clearAccountCaches(activeAccount.accountKey)
                        accountRepo.clearActiveAccount()
                        currentAccountKey = null
                        activeAccountKey.value = null
                        refreshRememberedAccountState()
                        onLogoutted()
                        markConnectionUnknown()
                        authState.value = AuthState.Unauthed(AUTH_MSG_INVALID_CREDENTIALS)
                    }

                    is LoginFailureReason.Network -> markOfflineCached()

                    is LoginFailureReason.Unknown -> {
                        MarkDoLog.w(TAG, "后台重登录失败：${reason.message}")
                        markOfflineCached()
                    }
                }
            }
        }
    }

    private suspend fun refreshRememberedAccountState() {
        val accounts = accountRepo.getRememberedAccounts()
        rememberedAccounts.value = accounts
        activeAccountKey.value = accountRepo.getActiveAccount()?.accountKey
    }
}
