package me.earzuchan.markdo.ui.views

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.stack.Children
import me.earzuchan.markdo.duties.AllCoursesDuty
import me.earzuchan.markdo.duties.CourseDetailDuty
import me.earzuchan.markdo.duties.CourseDuty
import me.earzuchan.markdo.duties.GradesDuty
import me.earzuchan.markdo.duties.MyDuty
import me.earzuchan.markdo.resources.Res
import me.earzuchan.markdo.resources.ic_logout_24px
import me.earzuchan.markdo.resources.ic_settings_24px
import me.earzuchan.markdo.ui.models.DialogActionItem
import me.earzuchan.markdo.ui.widgets.MIcon
import me.earzuchan.markdo.utils.ComposeUtils.only
import me.earzuchan.markdo.utils.ResUtils.vector
import org.jetbrains.compose.resources.DrawableResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradesPage(duty: GradesDuty) = Scaffold(topBar = {
    TopAppBar({ Text("成绩") })
}) { padding ->
    val state by duty.state.collectAsState()

    Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(WindowInsets.navigationBars.only(WindowInsetsSides.Top))) {
        when (val s = state) {
            is GradesDuty.UIState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))

            is GradesDuty.UIState.Error -> Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(s.msg, color = MaterialTheme.colorScheme.error)
                Button({ duty.loadGrades() }) { Text("重试") }
            }

            is GradesDuty.UIState.Success -> LazyColumn(Modifier.fillMaxSize()) {
                items(s.data) {
                    ListItem({ Text(it.name) }, trailingContent = { Text(it.grade, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary) })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoursePage(duty: CourseDuty) = Children(duty.navStack, Modifier.fillMaxSize()) { created ->
    when (val ins = created.instance) {
        is AllCoursesDuty -> AllCoursesPage(ins)

        is CourseDetailDuty -> CourseDetailPage(ins)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllCoursesPage(duty: AllCoursesDuty) = Scaffold(topBar = {
    TopAppBar({ Text("课程") })
}) { padding ->
    val state by duty.state.collectAsState()

    Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(WindowInsets.navigationBars.only(WindowInsetsSides.Top))) {
        when (val s = state) {
            is AllCoursesDuty.UIState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))

            is AllCoursesDuty.UIState.Error -> Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(s.msg, color = MaterialTheme.colorScheme.error)
                Button({ duty.loadCourses() }) { Text("重试") }
            }

            is AllCoursesDuty.UIState.Success -> LazyColumn(Modifier.fillMaxSize()) {
                items(s.data) {
                    ListItem(
                        { Text(it.name) }, Modifier.clickable { duty.onClickCourse(it.id) },
                        trailingContent = { Text(it.category, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyPage(duty: MyDuty) = Scaffold(topBar = {
    TopAppBar({ Text("我的") })
}) { padding ->
    var showWhetherLogout by remember { mutableStateOf(false) }

    if (showWhetherLogout) TeleAlertDialog("确定退出登录？", Res.drawable.ic_logout_24px, null, listOf(DialogActionItem("确定") { duty.logout() }, DialogActionItem("取消")), true, { showWhetherLogout = false })

    LazyColumn(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(WindowInsets.navigationBars.only(WindowInsetsSides.Top))) {
        item("name") {
            Text(duty.userName, Modifier.padding(16.dp).fillMaxWidth(), MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        }

        item("hello") {
            Text("你好", Modifier.padding(bottom = 16.dp).fillMaxWidth(), MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        }

        item("setting") {
            ListItem({ Text("设置") }, Modifier.clickable {}, leadingContent = { MIcon(Res.drawable.ic_settings_24px) })
        }

        item("logout") {
            ListItem({ Text("退出登录") }, Modifier.clickable { showWhetherLogout = true }, leadingContent = { MIcon(Res.drawable.ic_logout_24px) })
        }
    }
}

@Composable
fun <T> TeleOptionsDialog(
    title: String, options: List<Pair<String, T>>, selectedValue: T,
    onDismiss: () -> Unit, onConfirm: (T) -> Unit
) {
    var tempSelectedValue by remember { mutableStateOf(selectedValue) }

    TeleAlertDialog(
        title, actionsInColumn = false, onDismissRequest = onDismiss,
        actions = listOf(DialogActionItem("取消"), DialogActionItem("确定") { onConfirm(tempSelectedValue) })
    ) {
        Column(Modifier.selectableGroup()) {
            options.forEach { (optionTitle, optionValue) ->
                Row(
                    Modifier.fillMaxWidth().height(56.dp).selectable(
                        (tempSelectedValue == optionValue), role = Role.RadioButton
                    ) { tempSelectedValue = optionValue }.padding(horizontal = 24.dp),
                    Arrangement.SpaceBetween, Alignment.CenterVertically
                ) {
                    Text(optionTitle, style = MaterialTheme.typography.bodyLarge)
                    RadioButton((tempSelectedValue == optionValue), null)
                }
            }
        }
    }
}

@Composable
fun TeleAlertDialog(
    title: String, icon: DrawableResource? = null, description: String? = null, actions: List<DialogActionItem>,
    actionsInColumn: Boolean = true, onDismissRequest: () -> Unit = {}, content: (@Composable () -> Unit)? = null
) = TeleDialogBase(onDismissRequest) {
    Column(Modifier.width(IntrinsicSize.Min)) {
        val tFdp = 24.dp
        val eSdp = PaddingValues(tFdp, 20.dp)

        val midUpper = icon != null
        val hasContent = content != null

        Column(
            Modifier.padding(tFdp, tFdp, tFdp, if (hasContent) tFdp else 0.dp).fillMaxWidth(),
            Arrangement.spacedBy(16.dp), if (midUpper) Alignment.CenterHorizontally else Alignment.Start
        ) {
            if (midUpper) Icon(icon.vector, title, tint = MaterialTheme.colorScheme.secondary)

            Text(title, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
            description?.let {
                Text(
                    it, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (hasContent) content()

        @Composable
        fun listActions() = actions.forEach { actionItem ->
            TextButton({
                actionItem.action()
                onDismissRequest()
            }, Modifier.only(actionsInColumn) { fillMaxWidth() }.height(40.dp)) {
                Text(actionItem.text, color = MaterialTheme.colorScheme.primary)
            }
        }

        if (actionsInColumn) Column(
            Modifier.padding(eSdp).fillMaxWidth(),
            Arrangement.spacedBy(8.dp), Alignment.CenterHorizontally
        ) {
            listActions()
        } else Row(
            Modifier.padding(eSdp).fillMaxWidth(),
            Arrangement.spacedBy(8.dp, Alignment.End), Alignment.CenterVertically
        ) {
            listActions()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeleDialogBase(onDismissRequest: () -> Unit = {}, content: @Composable () -> Unit) = BasicAlertDialog(onDismissRequest) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        tonalElevation = AlertDialogDefaults.TonalElevation,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        content()
    }
}