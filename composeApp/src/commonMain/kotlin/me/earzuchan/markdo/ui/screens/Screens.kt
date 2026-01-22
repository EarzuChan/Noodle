package me.earzuchan.markdo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.FaultyDecomposeApi
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import me.earzuchan.markdo.duties.CourseDuty
import me.earzuchan.markdo.duties.GradesDuty
import me.earzuchan.markdo.duties.LoginDuty
import me.earzuchan.markdo.duties.MainDuty
import me.earzuchan.markdo.duties.MyDuty
import me.earzuchan.markdo.resources.Res
import me.earzuchan.markdo.resources.ic_courses_24px
import me.earzuchan.markdo.resources.ic_list_24px
import me.earzuchan.markdo.resources.ic_user_24px
import me.earzuchan.markdo.ui.views.CoursePage
import me.earzuchan.markdo.ui.views.GradesPage
import me.earzuchan.markdo.ui.views.MyPage
import me.earzuchan.markdo.ui.widgets.MIcon

@OptIn(ExperimentalMaterial3Api::class, FaultyDecomposeApi::class)
@Composable
fun MainScreen(duty: MainDuty) {
    val stack by duty.navStack.subscribeAsState()
    val activeInstance = stack.active.instance

    Scaffold(bottomBar = {
        NavigationBar {
            NavigationBarItem(activeInstance is GradesDuty, { duty.naviGrades() }, { MIcon(Res.drawable.ic_list_24px) }, label = { Text("成绩") })

            NavigationBarItem(activeInstance is CourseDuty, { duty.naviCourse() }, { MIcon(Res.drawable.ic_courses_24px) }, label = { Text("课程") })

            NavigationBarItem(activeInstance is MyDuty, { duty.naviMy() }, { MIcon(Res.drawable.ic_user_24px) }, label = { Text("我的") })
        }
    }) {
        Children(duty.navStack, Modifier.fillMaxSize().padding(bottom = it.calculateBottomPadding()).consumeWindowInsets(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))) { created ->
            when (val ins = created.instance) {
                is GradesDuty -> GradesPage(ins)

                is CourseDuty -> CoursePage(ins)

                is MyDuty -> MyPage(ins)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(duty: LoginDuty) = Scaffold(topBar = { TopAppBar({ Text("登录") }) }) { padding ->
    val baseSite by duty.baseSite.collectAsState()
    val username by duty.username.collectAsState()
    val password by duty.password.collectAsState()

    val error by duty.errorMessage.collectAsState()
    val disableButton by duty.disableButton.collectAsState()

    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        item { OutlinedTextField(baseSite, { duty.baseSite.value = it }, Modifier.fillMaxWidth(), label = { Text("站点域名") }) }

        item { OutlinedTextField(username, { duty.username.value = it }, Modifier.fillMaxWidth(), label = { Text("用户名") }) }

        item { OutlinedTextField(password, { duty.password.value = it }, Modifier.fillMaxWidth(), label = { Text("密码") }) }

        error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }

        item { Button({ duty.onLoginClick() }, Modifier.fillMaxWidth(), !disableButton) { Text(if (disableButton) "正在登录" else "登录") } }
    }
}

@Composable
fun SplashScreen() = Box(Modifier.fillMaxSize().systemBarsPadding()) {
    Text("正在\n加载", Modifier.align(Alignment.Center), style = MaterialTheme.typography.displayLarge)
}