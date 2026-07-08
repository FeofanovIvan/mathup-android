package com.feofanova.mathup.ui.screens.exam

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.feofanova.mathup.data.local.dao.AppDao
import com.feofanova.mathup.data.stats.StatsDatabase
import com.feofanova.mathup.data.stats.entities.ExamSessionEntity
import com.feofanova.mathup.domain.repository.ExamAnswerCheckItem
import com.feofanova.mathup.ui.components.AnswerInputBlock
import com.feofanova.mathup.ui.components.CanvasState
import com.feofanova.mathup.ui.components.CustomKeyboard
import com.feofanova.mathup.ui.components.DraftCanvasScreen
import com.feofanova.mathup.ui.components.MathKeyboardCoordinator
import com.feofanova.mathup.ui.screens.main.SetStatusBarColor
import com.feofanova.mathup.ui.screens.preparation.CircleButtonStyled
import kotlinx.coroutines.launch
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.key


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


    val examState = examViewModel.uiState
    val tasks = examState.tasks
    val selectedTab = examState.selectedTab
    val selectedTask = tasks.firstOrNull { it.taskID.toLong() == examState.selectedTaskId }

    var showDraftCanvas by rememberSaveable { mutableStateOf(false) }
    var showSidebarMenu by rememberSaveable { mutableStateOf(false) }
    var showInstructionExamPopup by remember { mutableStateOf(false) }

    val draftCanvasState = remember { CanvasState() }
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    var showResumePrompt by rememberSaveable { mutableStateOf(false) }
    var restoredSessionId by rememberSaveable { mutableStateOf<Long?>(null) }
    var examInitialized by rememberSaveable { mutableStateOf(false) }

    var userAnswer by rememberSaveable { mutableStateOf("") }
    var showEmptyAnswerAlert by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val coroutineScope = rememberCoroutineScope()
    var showSavedAlert by rememberSaveable { mutableStateOf(false) }
    var showExitConfirmation by rememberSaveable { mutableStateOf(false) }
    val currentIndex = remember { mutableIntStateOf(0) }
    val checkAnswers = remember { mutableStateOf<List<ExamAnswerCheckItem>>(emptyList()) }
    val resultList = remember { mutableStateOf<List<Pair<Int, Boolean>>>(emptyList()) }
    var showChecker by remember { mutableStateOf(false) }
    var showResultSummary by rememberSaveable { mutableStateOf(false) }


    val exitAfterFinalization = remember { mutableStateOf(false) }
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    LaunchedEffect(Unit) {
        if (!examInitialized) {
            val lastSession = examViewModel.getLastUnfinishedSession(profile)

            if (lastSession != null) {
                restoredSessionId = lastSession.sessionId
                showResumePrompt = true
            } else {
                examViewModel.loadExam(profile)
            }

            examInitialized = true
        }
    }
    LaunchedEffect(tasks) {
        if (examState.selectedTaskId == null && tasks.isNotEmpty()) {
            examViewModel.selectTask(tasks.first().taskID.toLong())
        }
    }
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
            showResultSummary = true
        }
    }

    if (showChecker && currentIndex.intValue < checkAnswers.value.size) {
        val current = checkAnswers.value.getOrNull(currentIndex.intValue)

        current?.let { item ->
            key(item.taskId) {
                SingleAnswerCheckWebView(
                    item = item,
                    onComplete = { taskId, isCorrect ->
                        resultList.value += (taskId to isCorrect)
                        currentIndex.intValue++
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
                onTabSelected = { examViewModel.selectTab(it) },
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
                        val currentId = examState.selectedTaskId
                        val completed = completedTaskIDsState.value
                        val nextTaskId = ExamTaskNavigator.findNextTaskId(
                            taskIds = tasks.map { it.taskID.toLong() },
                            currentTaskId = currentId,
                            completedTaskIds = completed
                        )

                        examViewModel.selectTask(nextTaskId)
                        examViewModel.selectTab(TaskExamTab.Task)
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
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                TabExamSelector(
                    selectedTab = selectedTab,
                    onTabSelected = { examViewModel.selectTab(it) },
                    onMenuClick = { showSidebarMenu = true },
                    initialTimeSeconds = 3 * 3600 + 55 * 60
                )
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
                                    .padding(horizontal = 16.dp)
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

                                                examViewModel.selectTab(TaskExamTab.Task)

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
                    CircleButtonStyled(
                        icon = Icons.AutoMirrored.Filled.Help,
                        iconTint = Color(0xFF2196F3),
                        background = Color.White,
                        shadowColor = Color(0xFF2196F3),
                        onClick = { showInstructionExamPopup = true },
                        size = 56.dp
                    )

                    Spacer(modifier = Modifier.weight(1f))
                    CircleButtonStyled(
                        icon = Icons.Default.Edit,
                        iconTint = Color(0xFFFFC107),
                        background = Color.White,
                        shadowColor = Color(0xFFFFC107),
                        onClick = { showDraftCanvas = true },
                        size = 56.dp
                    )
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(horizontal = 4.dp, vertical = 8.dp)
            ) {
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { showExitConfirmation = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1F2E59),
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
                    Button(
                        onClick = {
                            val currentId = examState.selectedTaskId
                            val completed = completedTaskIDsState.value
                            val nextTaskId = ExamTaskNavigator.findNextTaskId(
                                taskIds = tasks.map { it.taskID.toLong() },
                                currentTaskId = currentId,
                                completedTaskIds = completed
                            )

                            examViewModel.selectTask(nextTaskId)
                            examViewModel.selectTab(TaskExamTab.Task)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50),
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
        Column(modifier = Modifier.fillMaxSize()) {
            TabExamSelector(
                selectedTab = selectedTab,
                onTabSelected = { examViewModel.selectTab(it) },
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
                                .padding(horizontal = 16.dp)
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

                                            examViewModel.selectTab(TaskExamTab.Task)

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
                CircleButtonStyled(
                    icon = Icons.AutoMirrored.Filled.Help,
                    iconTint = Color(0xFF2196F3),
                    background = Color.White,
                    shadowColor = Color(0xFF2196F3),
                    onClick = { showInstructionExamPopup = true },
                    size = 56.dp
                )

                Spacer(modifier = Modifier.weight(1f))
                CircleButtonStyled(
                    icon = Icons.Default.Edit,
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
                Button(
                    onClick = { showExitConfirmation = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1F2E59),
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
                Button(
                    onClick = {
                        val currentId = examState.selectedTaskId
                        val completed = completedTaskIDsState.value
                        val nextTaskId = ExamTaskNavigator.findNextTaskId(
                            taskIds = tasks.map { it.taskID.toLong() },
                            currentTaskId = currentId,
                            completedTaskIds = completed
                        )

                        examViewModel.selectTask(nextTaskId)
                        examViewModel.selectTab(TaskExamTab.Task)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50),
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
            selectedTaskID = examState.selectedTaskId,
            onTaskSelected = {
                examViewModel.selectTask(it)
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
                        showExitConfirmation = false

                        coroutineScope.launch {
                            val answers = examViewModel.getAnswersForCheck()
                            if (answers.isEmpty()) {
                            } else {
                                checkAnswers.value = answers
                                resultList.value = emptyList()
                                currentIndex.intValue = 0
                                showChecker = true
                                showResultSummary = true
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
