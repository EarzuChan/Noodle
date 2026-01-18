package me.earzuchan.noodle.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.earzuchan.noodle.duties.LoginDuty
import me.earzuchan.noodle.duties.MainDuty

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(duty: MainDuty) = Scaffold(topBar = { TopAppBar({ Text("目前只能看成绩") }) }) { padding ->
    val state by duty.state.collectAsState()

    Box(Modifier.fillMaxSize().padding(padding)) {
        when (val s = state) {
            is MainDuty.UIState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))

            is MainDuty.UIState.Error -> Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(s.msg, color = MaterialTheme.colorScheme.error)
                Button({ duty.loadGrades() }) { Text("重试") }
            }

            is MainDuty.UIState.Success -> LazyColumn(Modifier.fillMaxSize()) {
                items(s.data.toList()) { (sj, grade) ->
                    ListItem({ Text(sj) }, trailingContent = { Text(grade, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary) })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(duty: LoginDuty) = Scaffold(topBar = { TopAppBar({ Text("登录") }) }) { padding ->
    val username by duty.username.collectAsState()
    val password by duty.password.collectAsState()
    val error by duty.errorMessage.collectAsState()
    val loading by duty.isLoading.collectAsState()

    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        item { OutlinedTextField(username, { duty.username.value = it }, Modifier.fillMaxWidth(), label = { Text("用户名") }) }

        item { OutlinedTextField(password, { duty.password.value = it }, Modifier.fillMaxWidth(), label = { Text("密码") }) }

        error?.let { item { Text(it, Modifier.padding(top = 8.dp), MaterialTheme.colorScheme.error) } }

        item { Button({ duty.onLoginClick() }, Modifier.fillMaxWidth(), !loading) { Text(if (loading) "正在登录" else "登录") } }
    }
}

@Composable
fun SplashScreen() = Box(Modifier.fillMaxSize()) {
    Text("正在\n加载", Modifier.align(Alignment.Center), style = MaterialTheme.typography.displayLarge)
}