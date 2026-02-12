package me.earzuchan.markdo.ui.views

import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import me.earzuchan.markdo.data.models.SavedLoginAccount
import com.arkivanov.decompose.extensions.compose.stack.Children
import me.earzuchan.markdo.duties.GradesDuty
import me.earzuchan.markdo.duties.MyDuty
import me.earzuchan.markdo.duties.SettingsDuty
import me.earzuchan.markdo.resources.*
import me.earzuchan.markdo.services.MoodleService
import me.earzuchan.markdo.ui.models.DialogActionItem
import me.earzuchan.markdo.ui.widgets.MIcon
import me.earzuchan.markdo.utils.ResUtils.t

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyPage(duty: MyDuty) = Children(duty.navStack, Modifier.fillMaxSize()) { created ->
    when (val ins = created.instance) {
        is MyDuty -> OverviewPage(ins)

        is GradesDuty -> GradesPage(ins)

        is SettingsDuty -> SettingsPage(ins)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewPage(duty: MyDuty) = Scaffold(topBar = {
    TopAppBar({ Text(Res.string.my.t) })
}) { padding ->
    var showWhetherLogout by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var showSwitchAccountDialog by remember { mutableStateOf(false) }
    var pendingDeleteAccount by remember { mutableStateOf<SavedLoginAccount?>(null) }

    val userName by duty.userName.collectAsState()
    val rememberedAccounts by duty.rememberedAccounts.collectAsState()
    val activeAccountKey by duty.activeAccountKey.collectAsState()
    val switchingAccount by duty.switchingAccount.collectAsState()
    val connectionState by duty.loginConnectionState.collectAsState()
    val loginStatusText = when (connectionState) {
        is MoodleService.LoginConnectionState.Onlined -> Res.string.login_status_online.t
        is MoodleService.LoginConnectionState.Offlined -> Res.string.login_status_offline_cached.t
        is MoodleService.LoginConnectionState.Onlining -> Res.string.login_status_onlining.t
        is MoodleService.LoginConnectionState.Unknown -> Res.string.login_status_unknown.t
    }

    if (showWhetherLogout) MAlertDialog(
        Res.string.sure_logout.t,
        Res.drawable.ic_logout_24px,
        null,
        listOf(DialogActionItem(Res.string.confirm.t) { duty.logout() }, DialogActionItem(Res.string.cancel.t)),
        true,
        { showWhetherLogout = false })

    if (showAbout) MAlertDialog(Res.string.about.t, Res.drawable.ic_info_24px, Res.string.about_desc.t, listOf(DialogActionItem(Res.string.ok.t)), true, { showAbout = false })

    if (showSwitchAccountDialog) MAlertDialog(Res.string.account_switch.t, null, null, listOf(DialogActionItem(Res.string.close.t)), false, { showSwitchAccountDialog = false }, {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).verticalScroll(rememberScrollState()), Arrangement.spacedBy(8.dp)) {
            rememberedAccounts.forEach { account ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(account.username, style = MaterialTheme.typography.bodyLarge)

                            Text(account.baseSite, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        if (account.accountKey == activeAccountKey) Text(Res.string.current_account.t, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                        else Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(
                                {
                                    duty.switchAccount(account.accountKey)
                                    showSwitchAccountDialog = false
                                }, enabled = !switchingAccount
                            ) { Text(Res.string.switch_account.t) }
                            TextButton({ pendingDeleteAccount = account }, enabled = !switchingAccount) { Text(Res.string.delete_account.t) }
                        }
                    }
                }
            }
        }
    })

    pendingDeleteAccount?.let { account ->
        MAlertDialog(
            Res.string.confirm_delete_account.t, Res.drawable.ic_block_24px, "${Res.string.delete_account_desc.t}\n${account.username}@${account.baseSite}", listOf(
                DialogActionItem(Res.string.cancel.t),DialogActionItem(Res.string.confirm.t) { duty.removeRememberedAccount(account.accountKey) }
            ), false, { pendingDeleteAccount = null })
    }

    LazyColumn(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(WindowInsets.navigationBars.only(WindowInsetsSides.Top))) {
        item("name") {
            Text(userName, Modifier.padding(16.dp).fillMaxWidth(), MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        }

        item("login_status") {
            Text(loginStatusText, Modifier.padding(bottom = 16.dp).fillMaxWidth(), MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        }

        item("grades") {
            ListItem({ Text(Res.string.grades.t) }, Modifier.clickable { duty.navGrades() }, leadingContent = { MIcon(Res.drawable.ic_list_24px) })
        }

        item("trans_man") {
            ListItem({ Text(Res.string.trans_man.t) }, Modifier.clickable { }, leadingContent = { MIcon(Res.drawable.ic_translate_24px) })
        }

        item("settings") {
            ListItem({ Text(Res.string.settings.t) }, Modifier.clickable { duty.navSettings() }, leadingContent = { MIcon(Res.drawable.ic_settings_24px) })
        }

        item("about") {
            ListItem({ Text(Res.string.about.t) }, Modifier.clickable { showAbout = true }, leadingContent = { MIcon(Res.drawable.ic_info_24px) })
        }

        if (rememberedAccounts.size > 1) item("account_switch") {
            ListItem({ Text(Res.string.account_switch.t) }, Modifier.clickable { showSwitchAccountDialog = true }, leadingContent = { MIcon(Res.drawable.ic_switch_account_24px) })
        }

        if (connectionState is MoodleService.LoginConnectionState.Offlined) item("relogin") {
            ListItem(
                { Text(Res.string.relogin.t) },
                Modifier.clickable { duty.retryLogin() },
                leadingContent = { MIcon(Res.drawable.ic_refresh_24px) }
            )
        }

        item("logout") {
            ListItem({ Text(Res.string.logout.t) }, Modifier.clickable { showWhetherLogout = true }, leadingContent = { MIcon(Res.drawable.ic_logout_24px) })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradesPage(duty: GradesDuty) = Scaffold(topBar = {
    TopAppBar({ Text(Res.string.grades.t) }, navigationIcon = { IconButton({ duty.navBack() }) { MIcon(Res.drawable.ic_arrow_back_24px) } })
}) { padding ->
    val state by duty.state.collectAsState()

    Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(WindowInsets.navigationBars.only(WindowInsetsSides.Top))) {
        when (val s = state) {
            is GradesDuty.UIState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))

            is GradesDuty.UIState.Error -> Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(s.msg, color = MaterialTheme.colorScheme.error)
                Button({ duty.loadGrades() }) { Text(Res.string.retry.t) }
            }

            is GradesDuty.UIState.Success -> LazyColumn(Modifier.fillMaxSize()) {
                items(s.data) {
                    ListItem({ Text(it.name) }, trailingContent = { Text(it.grade, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary) })
                }
            }
        }
    }
}

@Suppress("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPage(duty: SettingsDuty) = Scaffold(topBar = {
    TopAppBar({ Text(Res.string.settings.t) }, navigationIcon = { IconButton({ duty.navBack() }) { MIcon(Res.drawable.ic_arrow_back_24px) } })
}) { padding -> }
