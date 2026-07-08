package com.feofanova.mathup.ui.screens.exam

import android.util.Log
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.feofanova.mathup.data.local.dao.AppDao
import com.feofanova.mathup.data.local.entities.TaskEntity
import com.feofanova.mathup.data.stats.StatsDatabase
import com.feofanova.mathup.data.stats.entities.ExamSessionEntity
import com.feofanova.mathup.ui.components.AnswerInputBlock
import com.feofanova.mathup.ui.components.CanvasState
import com.feofanova.mathup.ui.components.CustomKeyboard
import com.feofanova.mathup.ui.components.DraftCanvasScreen
import com.feofanova.mathup.ui.components.MathKeyboardCoordinator
import com.feofanova.mathup.ui.screens.main.SetStatusBarColor
import com.feofanova.mathup.ui.screens.preparation.CircleButtonStyled
import com.feofanova.mathup.ui.screens.preparation.MathWebView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.key
import androidx.compose.ui.viewinterop.AndroidView
import com.feofanova.mathup.ui.components.wrapCheckerHtml
import com.feofanova.mathup.ui.screens.exam.ExamViewModel.AnswerCheckItem
import com.feofanova.mathup.sound.LocalSoundPlayer


@Composable
fun ExamScreen(
    profile: String,
    baseDao: AppDao,
    ogeDao: AppDao,
    coordinator: MathKeyboardCoordinator
) {
    SetStatusBarColor(color = Color.Transparent, darkIcons = true)

    val context = LocalContext.current
    val statsDao = remember { StatsDatabase.getInstance(context).statsDao() }

    val viewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current)
    val examViewModel: ExamViewModel = viewModel(
        viewModelStoreOwner = viewModelStoreOwner,
        factory = ExamViewModelFactory(
            baseDao = baseDao,
            ogeDao = ogeDao,
            statsDao = statsDao,
            profile = profile
        )
    )


    val tasks by examViewModel.examTasks.collectAsState()
    val selectedTab = examViewModel.selectedTab
    val selectedTask = tasks.firstOrNull { it.taskID.toLong() == examViewModel.selectedTaskId }

    var showDraftCanvas by rememberSaveable { mutableStateOf(false) }
    var showSidebarMenu by rememberSaveable { mutableStateOf(false) }
    var showInstructionExamPopup by remember { mutableStateOf(false) }

    val draftCanvasState = remember { CanvasState() }
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

// ─── Состояния восстановления и ввода ───
    var showResumePrompt by rememberSaveable { mutableStateOf(false) }
    var restoredSessionId by rememberSaveable { mutableStateOf<Long?>(null) }
    var examInitialized by rememberSaveable { mutableStateOf(false) }

    var userAnswer by rememberSaveable { mutableStateOf("") }
    var showEmptyAnswerAlert by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val coroutineScope = rememberCoroutineScope()
    var showSavedAlert by rememberSaveable { mutableStateOf(false) }
    var showExitConfirmation by rememberSaveable { mutableStateOf(false) }

// ─── Проверка и завершение ───
    val currentIndex = remember { mutableIntStateOf(0) }
    val checkAnswers = remember { mutableStateOf<List<ExamViewModel.AnswerCheckItem>>(emptyList()) }
    val resultList = remember { mutableStateOf<List<Pair<Int, Boolean>>>(emptyList()) }
    var showChecker by remember { mutableStateOf(false) }
    var showResultSummary by rememberSaveable { mutableStateOf(false) }


    val exitAfterFinalization = remember { mutableStateOf(false) }
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

// ─── Незавершённая сессия ───
    LaunchedEffect(Unit) {
        if (!examInitialized) {
            val lastSession = withContext(Dispatchers.IO) {
                statsDao.getLastUnfinishedSession(profile)
            }

            if (lastSession != null) {
                restoredSessionId = lastSession.sessionId
                showResumePrompt = true
            } else {
                examViewModel.loadExam(profile)
            }

            examInitialized = true
        }
    }

// ─── Автовыбор первого вопроса ───
    LaunchedEffect(tasks) {
        if (examViewModel.selectedTaskId == null && tasks.isNotEmpty()) {
            examViewModel.selectedTaskId = tasks.first().taskID.toLong()
        }
    }

// ─── Завершённые вопросы ───
    val completedTaskIDsState = remember { mutableStateOf<Set<Long>>(emptySet()) }

    LaunchedEffect(tasks) {
        if (tasks.isNotEmpty()) {
            val completed = examViewModel.getCompletedTaskIds()
            completedTaskIDsState.value = completed
        }
    }

    LaunchedEffect(currentIndex.intValue) {
        if (showChecker && currentIndex.intValue >= checkAnswers.value.size) {
            showChecker = false
            examViewModel.finalizeExamResults(resultList.value)
            showResultSummary = true // 👈 Показываем окно
        }
    }



// ─── Проверка текущего задания через WebView ───

    if (showChecker && currentIndex.intValue < checkAnswers.value.size) {
        val current = checkAnswers.value.getOrNull(currentIndex.intValue)

        current?.let { item ->
            key(item.taskId) {
                SingleAnswerCheckWebView(
                    item = item,
                    onComplete = { taskId, isCorrect ->
                        Log.d("Trigger", "✅ Проверка завершена: taskId=$taskId, isCorrect=$isCorrect")
                        resultList.value += (taskId to isCorrect)
                        currentIndex.intValue++ // 🔁 Переход к следующему
                        Log.d("Trigger", "➡️ currentIndex увеличен до ${currentIndex.intValue}")
                    }
                )
            }
        }
    }

    if (selectedTab == TaskExamTab.Task) {
        Column(modifier = Modifier.fillMaxSize().systemBarsPadding()
        ) {
            TabExamSelector(
                selectedTab = selectedTab,
                onTabSelected = { examViewModel.selectedTab = it },
                onMenuClick = { showSidebarMenu = true },
                initialTimeSeconds = 3 * 3600 + 55 * 60
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                TaskExamContent(task = selectedTask)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { showExitConfirmation = true },
                    modifier = Modifier.weight(1f).height(42.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1F2E59),
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.Close, "Завершить", tint = Color.Red, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("ЗАВЕРШИТЬ", fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = {
                        val currentId = examViewModel.selectedTaskId
                        val taskList = tasks
                        val completed = completedTaskIDsState.value

                        val currentIndex = taskList.indexOfFirst { it.taskID.toLong() == currentId }

                        // 🔍 Пытаемся найти следующий неотвеченный вопрос после текущего
                        val nextUnanswered = taskList
                            .drop(currentIndex + 1)
                            .firstOrNull { it.taskID.toLong() !in completed }

                        // 🟢 Если нашли неотвеченный — переходим к нему
                        val nextTaskId = when {
                            nextUnanswered != null -> nextUnanswered.taskID.toLong()

                            // 🟡 Иначе — просто следующий по списку (если есть)
                            currentIndex in taskList.indices && currentIndex + 1 < taskList.size ->
                                taskList[currentIndex + 1].taskID.toLong()

                            // 🔴 Иначе остаёмся на текущем
                            else -> currentId
                        }

                        examViewModel.selectedTaskId = nextTaskId
                        examViewModel.selectedTab = TaskExamTab.Task // ✅ всегда показываем задание нового вопроса
                    }
                    ,
                    modifier = Modifier.weight(1f).height(42.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50),
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.PlayArrow, "Продолжить", modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("ПРОДОЛЖИТЬ", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    } else {

    if (isLandscape) {
        Row(modifier = Modifier.fillMaxSize().systemBarsPadding()
        ) {
            // ─── Слева: меню + вкладки, контент, кнопки «лампочка» и «черновик» ───
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                TabExamSelector(
                    selectedTab = selectedTab,
                    onTabSelected = { examViewModel.selectedTab = it },
                    onMenuClick = { showSidebarMenu = true },
                    initialTimeSeconds = 3 * 3600 + 55 * 60
                )

                // Основной контент: либо задание, либо ввод ответа
                Box(modifier = Modifier.weight(1f)) {
                    when (selectedTab) {
                        TaskExamTab.Task -> {
                            Box(
                                Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Содержимое задания")
                            }
                        }
                        TaskExamTab.Answer -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .padding(horizontal = 16.dp) // ← добавлены отступы по бокам
                            ) {
                                Spacer(modifier = Modifier.height(24.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Введите ответ ниже:",
                                        color = Color.Gray,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Button(
                                        onClick = {
                                            if (userAnswer.isBlank()) {
                                                showEmptyAnswerAlert = true
                                            } else {
                                                examViewModel.submitAnswer(userAnswer.trim())

                                                examViewModel.selectedTab = TaskExamTab.Task // ← переключение на вкладку "Задание"

                                                userAnswer = ""
                                                coordinator.resetText()
                                                showSavedAlert = true

                                                scope.launch {
                                                    val completed = examViewModel.getCompletedTaskIds()
                                                    completedTaskIDsState.value = completed
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                                    ) {
                                        Text("ОТВЕТИТЬ", color = Color.White)
                                    }

                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                AnswerInputBlock(
                                    coordinator = coordinator,
                                    modifier = Modifier.fillMaxWidth(),
                                    onAnswerChanged = { userAnswer = it }
                                )


                                Spacer(modifier = Modifier.height(2.dp))

                                HorizontalDivider(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp),
                                    color = Color(0xFF4CAF50)
                                )
                            }

                        }
                    }
                }

                // Кнопки «лампочка» (hint) и «черновик» под контентом
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Левая кнопка – подсказка
                    CircleButtonStyled(
                        icon = Icons.AutoMirrored.Filled.Help,
                        iconTint = Color(0xFF2196F3),
                        background = Color.White,
                        shadowColor = Color(0xFF2196F3),
                        onClick = { showInstructionExamPopup = true },
                        size = 56.dp
                    )

                    Spacer(modifier = Modifier.weight(1f))


                    // Правая кнопка – чертёж (чистовик / набросок)
                    CircleButtonStyled(
                        icon = Icons.Default.Edit, // или Icons.Default.Create
                        iconTint = Color(0xFFFFC107),
                        background = Color.White,
                        shadowColor = Color(0xFFFFC107),
                        onClick = { showDraftCanvas = true },
                        size = 56.dp
                    )
                }
            }

            // ─── Справа: клавиатура + кнопки «Завершить» и «Продолжить» ───
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(horizontal = 4.dp, vertical = 8.dp)
            ) {
                // Карточка с клавиатурой, занимает всю оставшуюся высоту
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    CustomKeyboard(coordinator = coordinator)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Две круглые кнопки «Завершить» и «Продолжить» под клавиатурой
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // ❌ ЗАВЕРШИТЬ
                    Button(
                        onClick = { showExitConfirmation = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1F2E59), // глубокий синий фон
                            contentColor = Color.White
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Завершить",
                            tint = Color.Red,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ЗАВЕРШИТЬ",
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }

                    // ▶️ ПРОДОЛЖИТЬ
                    Button(
                        onClick = {
                            val currentId = examViewModel.selectedTaskId
                            val taskList = tasks
                            val completed = completedTaskIDsState.value

                            val currentIndex = taskList.indexOfFirst { it.taskID.toLong() == currentId }

                            // 🔍 Пытаемся найти следующий неотвеченный вопрос после текущего
                            val nextUnanswered = taskList
                                .drop(currentIndex + 1)
                                .firstOrNull { it.taskID.toLong() !in completed }

                            // 🟢 Если нашли неотвеченный — переходим к нему
                            val nextTaskId = when {
                                nextUnanswered != null -> nextUnanswered.taskID.toLong()

                                // 🟡 Иначе — просто следующий по списку (если есть)
                                currentIndex in taskList.indices && currentIndex + 1 < taskList.size ->
                                    taskList[currentIndex + 1].taskID.toLong()

                                // 🔴 Иначе остаёмся на текущем
                                else -> currentId
                            }

                            examViewModel.selectedTaskId = nextTaskId
                            examViewModel.selectedTab = TaskExamTab.Task // ✅ всегда показываем задание нового вопроса
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50), // зелёный фон
                            contentColor = Color.White
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Продолжить",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ПРОДОЛЖИТЬ",
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    } else {
        // ─── Портретная версия (оставлена без изменений) ───
        Column(modifier = Modifier.fillMaxSize()) {
            TabExamSelector(
                selectedTab = selectedTab,
                onTabSelected = { examViewModel.selectedTab = it },
                onMenuClick = { showSidebarMenu = true },
                initialTimeSeconds = 3 * 3600 + 55 * 60
            )

            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    TaskExamTab.Task -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Содержимое задания")
                        }
                    }
                    TaskExamTab.Answer -> {
                        Column(
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(horizontal = 16.dp) // ← добавлены отступы по бокам
                        ) {
                            Spacer(modifier = Modifier.height(24.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Введите ответ ниже:",
                                    color = Color.Gray,
                                    modifier = Modifier.weight(1f)
                                )
                                Button(
                                    onClick = {
                                        if (userAnswer.isBlank()) {
                                            showEmptyAnswerAlert = true
                                        } else {
                                            examViewModel.submitAnswer(userAnswer.trim())

                                            examViewModel.selectedTab = TaskExamTab.Task // ← переключение на вкладку "Задание"

                                            userAnswer = ""
                                            coordinator.resetText()
                                            showSavedAlert = true

                                            scope.launch {
                                                val completed = examViewModel.getCompletedTaskIds()
                                                completedTaskIDsState.value = completed
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                                ) {
                                    Text("ОТВЕТИТЬ", color = Color.White)
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            AnswerInputBlock(
                                coordinator = coordinator,
                                modifier = Modifier.fillMaxWidth(),
                                onAnswerChanged = { userAnswer = it }
                            )


                            Spacer(modifier = Modifier.height(2.dp))

                            HorizontalDivider(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp),
                                color = Color(0xFF4CAF50)
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Левая кнопка – подсказка
                CircleButtonStyled(
                    icon = Icons.AutoMirrored.Filled.Help,
                    iconTint = Color(0xFF2196F3),
                    background = Color.White,
                    shadowColor = Color(0xFF2196F3),
                    onClick = { showInstructionExamPopup = true },
                    size = 56.dp
                )

                Spacer(modifier = Modifier.weight(1f))


                // Правая кнопка – чертёж (чистовик / набросок)
                CircleButtonStyled(
                    icon = Icons.Default.Edit, // или Icons.Default.Create
                    iconTint = Color(0xFFFFC107),
                    background = Color.White,
                    shadowColor = Color(0xFFFFC107),
                    onClick = { showDraftCanvas = true },
                    size = 56.dp
                )
            }


            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(horizontal = 2.dp, vertical = 4.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                CustomKeyboard(coordinator = coordinator)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ❌ ЗАВЕРШИТЬ
                Button(
                    onClick = { showExitConfirmation = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1F2E59), // глубокий синий фон
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Завершить",
                        tint = Color.Red,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ЗАВЕРШИТЬ",
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }

                // ▶️ ПРОДОЛЖИТЬ
                Button(
                    onClick = {
                        val currentId = examViewModel.selectedTaskId
                        val taskList = tasks
                        val completed = completedTaskIDsState.value

                        val currentIndex = taskList.indexOfFirst { it.taskID.toLong() == currentId }

                        // 🔍 Пытаемся найти следующий неотвеченный вопрос после текущего
                        val nextUnanswered = taskList
                            .drop(currentIndex + 1)
                            .firstOrNull { it.taskID.toLong() !in completed }

                        // 🟢 Если нашли неотвеченный — переходим к нему
                        val nextTaskId = when {
                            nextUnanswered != null -> nextUnanswered.taskID.toLong()

                            // 🟡 Иначе — просто следующий по списку (если есть)
                            currentIndex in taskList.indices && currentIndex + 1 < taskList.size ->
                                taskList[currentIndex + 1].taskID.toLong()

                            // 🔴 Иначе остаёмся на текущем
                            else -> currentId
                        }

                        examViewModel.selectedTaskId = nextTaskId
                        examViewModel.selectedTab = TaskExamTab.Task // ✅ всегда показываем задание нового вопроса
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50), // зелёный фон
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Продолжить",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ПРОДОЛЖИТЬ",
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            }
        }
    }
    }
    if (showSidebarMenu) {
        SideMenuExam(
            taskName = "Экзамен",
            taskEntities = tasks,
            completedTaskIDs = completedTaskIDsState.value,
            selectedTaskID = examViewModel.selectedTaskId,
            onTaskSelected = {
                examViewModel.selectedTaskId = it
                showSidebarMenu = false
            },
            onDismiss = { showSidebarMenu = false },
            profile = profile
        )
    }



    if (showInstructionExamPopup) {
        InstructionExamPopup(onDismiss = { showInstructionExamPopup = false })
    }
    if (showDraftCanvas) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            DraftCanvasScreen(
                onClose = {
                    showDraftCanvas = false
                },
                canvasState = draftCanvasState
            )
        }
    }
    if (showResumePrompt && restoredSessionId != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f)),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(0.85f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("У вас уже начатый экзамен", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Вы хотите продолжить его?", fontSize = 16.sp)

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = {
                                showResumePrompt = false
                                examViewModel.restoreSession(
                                    ExamSessionEntity(sessionId = restoredSessionId!!, profile = profile, remainingTimeSeconds = 0)
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                        ) {
                            Text("Да", color = Color.White)
                        }

                        Button(
                            onClick = {
                                showResumePrompt = false
                                examViewModel.discardAndStartNew(profile)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                        ) {
                            Text("Нет", color = Color.White)
                        }
                    }
                }
            }
        }
    }
    if (showEmptyAnswerAlert) {
        AlertDialog(
            onDismissRequest = { showEmptyAnswerAlert = false },
            confirmButton = {
                Button(onClick = { showEmptyAnswerAlert = false }) {
                    Text("ОК")
                }
            },
            title = { Text("Пустой ответ") },
            text = { Text("Введите ответ перед отправкой.") }
        )
    }
    if (showSavedAlert) {
        AlertDialog(
            onDismissRequest = { showSavedAlert = false },
            confirmButton = {
                Button(onClick = { showSavedAlert = false }) {
                    Text("ОК")
                }
            },
            title = { Text("Ответ сохранён") }
        )
    }
    if (showExitConfirmation) {
        val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

        AlertDialog(
            onDismissRequest = { showExitConfirmation = false },
            title = { Text("Завершить экзамен?") },
            text = { Text("Вы действительно хотите завершить экзамен?") },
            confirmButton = {
                Button(
                    onClick = {
                        showExitConfirmation = false // 👈 Закрываем AlertDialog

                        coroutineScope.launch {
                            val answers = examViewModel.getAnswersForCheck()
                            if (answers.isEmpty()) {
                                Log.d("AnswerCheck", "⚠️ Нет проверяемых данных")
                            } else {
                                checkAnswers.value = answers
                                resultList.value = emptyList()
                                currentIndex.intValue = 0
                                showChecker = true
                                showResultSummary = true // 👈 Открываем окно результатов
                            }
                        }
                    }
                ) {
                    Text("ДА")
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        showExitConfirmation = false
                        dispatcher?.onBackPressed()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F2E59))
                ) {
                    Text("СОХРАНИТЬ И ВЫЙТИ", color = Color.White)
                }
            }
        )
    }
    val summaryResults = checkAnswers.value.zip(resultList.value) { item, result ->
        item to result.second
    }

    if (showResultSummary) {
        ResultSummaryDialog(
            results = summaryResults,
            expectedCount = checkAnswers.value.size,
            onExit = {
                showResultSummary = false
                backDispatcher?.onBackPressed()
            },
            onRestart = {
                showResultSummary = false
                examViewModel.discardAndStartNew(profile)
            }
        )
    }
}
@Composable
fun PlayFinalSound() {
    val player = LocalSoundPlayer.current

    LaunchedEffect(Unit) {
        player.playFinal()
    }
}

@Composable
fun InstructionExamPopup(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(16.dp)
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
            modifier = Modifier
                .widthIn(max = 360.dp)
                .heightIn(max = 360.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
            ) {
                // ─── Кнопка закрытия ───
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFD32F2F)) // красный круг
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Закрыть",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // ─── Содержимое с прокруткой ───
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Краткое руководство по использованию математической клавиатуры:",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text("• Перемещение курсора происходит с помощью клавиши ⏎ (например, из числителя в знаменатель или основания логарифма в подлогарифмическое выражение).")
                    Text("• Символы ≥ и ≤ создаются при последовательном вводе символов >= или <=.")
                    Text("• Кнопка CE очищает всё.")
                    Text("• Кнопка ⌫ удаляет последний символ.")
                }
            }
        }
    }
}
enum class TaskExamTab(val title: String) {
    Task("Задание"),
    Answer("Ответ")
}
@Composable
fun TabExamSelector(
    selectedTab: TaskExamTab,
    onTabSelected: (TaskExamTab) -> Unit,
    modifier: Modifier = Modifier,
    onMenuClick: () -> Unit,
    initialTimeSeconds: Int = 3 * 3600 + 55 * 60 // 3:55:00
) {
    val timerColor = Color(0xFFFFC107) // жёлтый
    var remainingSeconds by rememberSaveable { mutableIntStateOf(initialTimeSeconds) }

    // Мерцание двоеточия
    val blinkAlpha by rememberInfiniteTransition(label = "blink").animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(700),
            repeatMode = RepeatMode.Reverse
        ), label = "blink"
    )

    // Таймер логика: уменьшение на 1 каждую секунду
    LaunchedEffect(Unit) {
        while (remainingSeconds > 0) {
            delay(1000)
            remainingSeconds--
        }
    }

    val hours = remainingSeconds / 3600
    val minutes = (remainingSeconds % 3600) / 60

    val hStr = hours.toString().padStart(2, '0')
    val mStr = minutes.toString().padStart(2, '0')

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Меню
        IconButton(onClick = onMenuClick) {
            Icon(Icons.Default.Menu, contentDescription = "Меню", tint = Color(0xFF1F2E59))
        }

        // Вкладки
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.weight(1f)
        ) {
            TaskExamTab.entries.forEach { tab ->
                val interactionSource = remember { MutableInteractionSource() }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null
                        ) { onTabSelected(tab) }
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = tab.title.uppercase(),
                        color = if (selectedTab == tab) Color(0xFF4CAF50) else Color(0xFF1F2E59),
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .height(2.dp)
                            .width(48.dp)
                            .background(if (selectedTab == tab) Color(0xFF4CAF50) else Color.Transparent)
                    )
                }
            }
        }

        // Таймер (HH:MM) с анимированным разделителем
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFF5F5F5))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = hStr, color = timerColor, fontWeight = FontWeight.SemiBold)
                Text(
                    text = ":",
                    color = timerColor.copy(alpha = blinkAlpha),
                    fontWeight = FontWeight.SemiBold
                )
                Text(text = mStr, color = timerColor, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun SideMenuExam(
    taskName: String,
    taskEntities: List<TaskEntity>,
    completedTaskIDs: Set<Long>,
    selectedTaskID: Long?,
    onTaskSelected: (Long) -> Unit,
    onDismiss: () -> Unit,
    profile: String
) {
    Row(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .width(250.dp)
                .fillMaxHeight()
                .background(Color.White)
                .padding(8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = taskName,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1F2E59),
                modifier = Modifier.padding(8.dp)
            )

            HorizontalDivider(
                modifier = Modifier.fillMaxWidth().height(1.dp),
                color = Color.LightGray
            )
            Spacer(Modifier.height(4.dp))

            taskEntities.forEachIndexed { index, task ->
                val isSelected = selectedTaskID?.toInt() == task.taskID
                val isCompleted = completedTaskIDs.contains(task.taskID.toLong())

                Button(
                    onClick = { onTaskSelected(task.taskID.toLong()) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when {
                            isSelected -> Color(0xFFE3F2FD)
                            isCompleted -> Color(0xFFE8F5E9)
                            else -> Color.Transparent
                        },
                        contentColor = if (isCompleted) Color(0xFF4CAF50) else Color(0xFF1F2E59)
                    )
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Вопрос ${index + 1}", modifier = Modifier.weight(1f))

                        if (isCompleted) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50))
                        } else if (isSelected) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.Blue)
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f))
                .clickable { onDismiss() }
        )
    }
}

@Composable
fun TaskExamContent(task: TaskEntity?) {
    Log.d("TaskContent", "task = $task")
    if (task == null) {
        Text("Выберите вопрос", modifier = Modifier.padding(16.dp))
        return
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            MathWebView(content = task.description!!)
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            MathWebView(content = task.drawingLink!!)
        }

    }
}
@Composable
fun SingleAnswerCheckWebView(
    item: ExamViewModel.AnswerCheckItem,
    onComplete: (taskId: Int, isCorrect: Boolean) -> Unit
) {
    val context = LocalContext.current
    val isHandled = remember { mutableStateOf(false) }

    if (item.userAnswer == "—" || item.userAnswer.trim().isEmpty()) {
        Log.d("Trigger", "⛔ Пропуск пустого ответа: taskId=${item.taskId}")
        LaunchedEffect(item.taskId) {
            onComplete(item.taskId, false)
        }
        return
    }

    key(item.taskId) {
        AndroidView(factory = {
            WebView(context).apply {
                visibility = View.GONE
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        // Запускаем сравнение после загрузки страницы
                        evaluateJavascript("processComparison()", null)
                    }
                }

                addJavascriptInterface(object {
                    @JavascriptInterface
                    fun postMessage(msg: String) {
                        Log.d("MathCheckWebView", "taskId=${item.taskId}, result=$msg")
                        if (!isHandled.value && (msg == "true" || msg == "false")) {
                            isHandled.value = true
                            onComplete(item.taskId, msg == "true")
                        }
                    }
                }, "logger")

                val html = wrapCheckerHtml(
                    item.userAnswer.replace("\n", ""),
                    item.correctAnswer.replace("\n", "")
                )
                loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
            }
        })
    }
}

@Composable
fun ResultSummaryDialog(
    results: List<Pair<ExamViewModel.AnswerCheckItem, Boolean>>,
    expectedCount: Int,
    onExit: () -> Unit,
    onRestart: () -> Unit
) {
    val correctCount = results.count { it.second }
    val isLoading = results.size < expectedCount
    PlayFinalSound()
    val html = rememberSaveable(
        inputs = arrayOf(results.hashCode())
    ) {
        buildResultHtml(results.mapIndexed { index, (item, isCorrect) ->
            ExamViewModel.AnswerCheckItem(
                taskId = item.taskId,
                userAnswer = item.userAnswer,
                correctAnswer = item.correctAnswer,
                isCorrect = isCorrect
            )
        })
    }


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            shadowElevation = 10.dp,
            tonalElevation = 0.dp,
            color = Color.White,
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth()
                .defaultMinSize(minHeight = 200.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("📊 Ваш результат:", fontSize = 20.sp, fontWeight = FontWeight.Bold)

                Text(
                    "Правильных ответов: $correctCount из $expectedCount",
                    fontSize = 18.sp,
                    color = Color(0xFF4CAF50),
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    Button(
                        onClick = onExit,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Выйти")
                    }

                    Button(
                        onClick = onRestart,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Новый")
                    }
                }

                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(40.dp),
                        color = Color.Gray,
                        strokeWidth = 3.dp
                    )
                } else {
                    HtmlResultView(htmlContent = html.toString())
                }


            }
        }
    }
}

fun buildResultHtml(results: List<AnswerCheckItem>): String {
    val header = """
        <tr>
            <th style="font-size: 20px;">№</th>
            <th style="font-size: 20px;">Ответ</th>
            <th style="font-size: 20px;">Правильно</th>
        </tr>
    """

    val rows = results.mapIndexed { index, item ->
        val escapedUser = item.userAnswer
            .replace("\\", "\\\\")
            .replace("\\\\lceil", "")
            .replace("\n", "")
        val escapedCorrect = item.correctAnswer.replace("\\", "\\\\").replace("\n", "")

        val userColor = if (item.isCorrect == true) "" else "style='background-color: #ffe5e5'"
        val correctColor = if (item.isCorrect == true) "style='background-color: #e5ffe5'" else ""

        """
        <tr>
            <td style="font-size: 16px;">${index + 1}</td>
            <td $userColor><span id="user${index + 1}" style="font-size: 100%;"></span></td>
            <td $correctColor><span id="correct${index + 1}" style="font-size: 100%;"></span></td>
        </tr>
        <script>
            console.log("➡️ Вопрос №${index + 1}: начало рендеринга");

            console.log("🔹 Пользовательский LaTeX: $escapedUser");
            document.getElementById('user${index + 1}').innerHTML = katex.renderToString("$escapedUser", { throwOnError: false });

            console.log("🔹 Math.js вход (правильный ответ): $escapedCorrect");
            try {
                const correctExpr${index + 1} = math.parse("$escapedCorrect").toTex();
                document.getElementById('correct${index + 1}').innerHTML = katex.renderToString(correctExpr${index + 1}, { throwOnError: false });
            } catch (e) {
                console.error("❌ Ошибка при парсинге:", e);
            }
        </script>
        """
    }.joinToString("")

    return """
        <html>
        <head>
        <meta charset="UTF-8">
        <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/katex@0.13.0/dist/katex.min.css">
        <script src="https://cdn.jsdelivr.net/npm/katex@0.13.0/dist/katex.min.js"></script>
        <script src="https://cdnjs.cloudflare.com/ajax/libs/mathjs/9.4.4/math.min.js"></script>
        </head>
        <body>
        <table border="1" cellpadding="12" cellspacing="0" style="width:100%; border-collapse: collapse;">
            $header
            $rows
        </table>
        </body>
        </html>
    """
}


@Composable
fun HtmlResultView(htmlContent: String) {
    AndroidView(factory = { context ->
        WebView(context).apply {
            settings.javaScriptEnabled = true
            loadDataWithBaseURL(null, htmlContent, "text/html", "utf-8", null)
        }
    }, modifier = Modifier.fillMaxWidth().height(400.dp))
}
