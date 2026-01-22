package me.earzuchan.markdo.ui.views

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import lib.fetchmoodle.CourseModule
import lib.fetchmoodle.CourseModuleAvailability
import lib.fetchmoodle.SectionLike
import me.earzuchan.markdo.duties.CourseDetailDuty
import me.earzuchan.markdo.resources.*
import me.earzuchan.markdo.ui.widgets.MIcon
import me.earzuchan.markdo.utils.ResUtils.vector
import org.jetbrains.compose.resources.DrawableResource


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseDetailPage(duty: CourseDetailDuty) {
    val state by duty.state.collectAsState()

    Scaffold(topBar = {
        TopAppBar({
            Text(
                when (val s = state) {
                    is CourseDetailDuty.UIState.Success -> s.data.name
                    else -> "课程详情"
                }
            )
        }, navigationIcon = { IconButton({ duty.naviBack() }) { MIcon(Res.drawable.ic_arrow_back_24px) } })
    }) { padding ->
        val state by duty.state.collectAsState()

        Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(WindowInsets.navigationBars.only(WindowInsetsSides.Top))) {
            when (val s = state) {
                is CourseDetailDuty.UIState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))

                is CourseDetailDuty.UIState.Error -> Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(s.msg, color = MaterialTheme.colorScheme.error)
                    Button({ duty.loadCourse() }) { Text("重试") }
                }

                is CourseDetailDuty.UIState.Success -> {
                    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        s.data.sections.forEach { section -> item(section.id) { SectionView(section, duty) } }
                    }
                }
            }
        }
    }
}

@Composable
fun SectionView(section: SectionLike, duty: CourseDetailDuty): Unit = Column(
    Modifier
        .fillMaxWidth()
        .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.large)
        .padding(vertical = 16.dp)
) {
    // 1. 头部永远显示
    SectionHeader(section)

    // 2. 遍历模块，将“上一个模块”的信息传给分发器
    section.modules.forEachIndexed { index, module ->
        val prevModule = if (index > 0) section.modules[index - 1] else null

        ModuleItemDispatcher(module, prevModule, index == 0, duty)
    }
}

@Composable
fun ModuleItemDispatcher(module: CourseModule, prevModule: CourseModule?, isFirst: Boolean, duty: CourseDetailDuty) {
    // 判断当前模块和上一个模块是否都是 ListItem 类型
    val currentIsFull = isFullWidth(module)
    val prevIsFull = prevModule?.let { isFullWidth(it) } ?: false

    // 间距逻辑：
    // 1. 如果是第一个模块，且 Header 存在，必须有 16dp Gap
    // 2. 如果当前或前一个是 Inset 类型，必须有 16dp Gap
    // 3. 只有连续两个 FullWidth 之间是 0 Gap
    val needTopGap = isFirst || !currentIsFull || !prevIsFull

    Column(Modifier.fillMaxWidth()) {
        if (needTopGap) Spacer(Modifier.height(16.dp))

        // 统一处理左右内边距：如果是 FullWidth 类型则为 0，否则为 16.dp
        val horizontalPadding = if (currentIsFull) 0.dp else 16.dp

        Box(Modifier.fillMaxWidth().padding(horizontal = horizontalPadding)) {
            when (module) {
                is CourseModule.SubSection -> SectionView(module, duty) // 递归
                is CourseModule.Label -> LabelView(module)
                is CourseModule.Resource -> ResourceView(module) { /*...*/ }
                is CourseModule.Assignment -> AssignmentView(module) { /*...*/ }
                is CourseModule.Forum -> SimpleModuleView(module, Res.drawable.ic_forum_24px) { /*...*/ }
                is CourseModule.Quiz -> QuizView(module) { /*...*/ }
                else -> SimpleModuleView(module, Res.drawable.ic_extension_24px) { /*...*/ }
            }
        }
    }
}

// 提取判断逻辑
private fun isFullWidth(module: CourseModule): Boolean =
    module !is CourseModule.Label && module !is CourseModule.SubSection

@Composable
fun SectionHeader(section: SectionLike) = Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), Arrangement.spacedBy(16.dp)) {

    Text(section.name, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleLarge)

    section.summary?.let { if (it.isNotBlank()) Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium) }
}

@Composable
fun ResourceView(resource: CourseModule.Resource, onClick: (CourseModule.Resource) -> Unit) = ListItem(
    { Text(resource.name) }, Modifier.clickable { onClick(resource) },
    supportingContent = {
        val info = listOfNotNull(resource.fileSize, resource.uploadDate, resource.availability?.description).joinToString(" · ")
        if (info.isNotBlank()) Text(info)
    },
    leadingContent = { MIcon(Res.drawable.ic_file_24px, MaterialTheme.colorScheme.primary) }, trailingContent = { resource.availability?.let { RestrictionBadge(it) } }
)

@Composable
fun AssignmentView(assign: CourseModule.Assignment, onClick: () -> Unit) = ListItem(
    { Text(assign.name) }, Modifier.clickable { onClick() }, leadingContent = { MIcon(Res.drawable.ic_task_24px, MaterialTheme.colorScheme.tertiary) },
    supportingContent = { Text(listOfNotNull("开始日期：${assign.openDate}", "截止日期：${assign.dueDate}", assign.description).joinToString("\n"), color = MaterialTheme.colorScheme.error) }
)

@Composable
fun LabelView(label: CourseModule.Label) {
    // TODO：简单处理 HTML，以后用库
    val plainText = remember(label.contentHtml) { label.contentHtml.replace(Regex("<[^>]*>"), "").replace("&nbsp;", " ").trim() }

    if (plainText.isNotEmpty()) Text(plainText, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium)
}

@Composable
fun SimpleModuleView(module: CourseModule, icon: DrawableResource, onClick: () -> Unit) = ListItem(
    { Text(module.name) }, Modifier.clickable { onClick() }, leadingContent = { MIcon(icon) }
)

@Composable
fun RestrictionBadge(availability: CourseModuleAvailability) {
    if (availability.isRestricted) MIcon(Res.drawable.ic_block_24px)
}

@Composable
fun QuizView(quiz: CourseModule.Quiz, onClick: () -> Unit) = ListItem(
    { Text(quiz.name) }, Modifier.clickable { onClick() }, leadingContent = { MIcon(Res.drawable.ic_quiz_24px, MaterialTheme.colorScheme.tertiary) },
    supportingContent = { Text(listOfNotNull("开始日期：${quiz.openDate}", "截止日期：${quiz.closeDate}", quiz.description, quiz.availability?.description).joinToString("\n"), color = MaterialTheme.colorScheme.error) },
    trailingContent = { quiz.availability?.let { RestrictionBadge(it) } }
)