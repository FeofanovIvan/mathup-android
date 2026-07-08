package com.feofanova.mathup.ui.screens.preparation

import android.content.ContentValues.TAG
import android.util.Log
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.feofanova.mathup.data.local.dao.AppDao
import com.feofanova.mathup.data.local.entities.TaskEntity
import com.feofanova.mathup.ui.components.CanvasState
import com.feofanova.mathup.ui.components.DraftCanvasScreen
import com.feofanova.mathup.ui.screens.main.SetStatusBarColor
import androidx.compose.ui.platform.LocalConfiguration
import com.feofanova.mathup.data.stats.StatsDatabase
import com.feofanova.mathup.ui.components.*
import com.feofanova.mathup.ui.screens.exam.PlayFinalSound
import com.feofanova.mathup.ui.screens.formulas.PlayFeedbackSound
import kotlinx.coroutines.delay


@Composable
fun TaskScreen(
    blockId: Int,
    profile: String,
    onBack: () -> Unit,
    baseDao: AppDao,
    ogeDao: AppDao
) {
    SetStatusBarColor(color = Color.Transparent, darkIcons = true)
    val context = LocalContext.current

    val statsDao = remember { StatsDatabase.getInstance(context).statsDao() }
    val factory = remember { TaskViewModelFactory(baseDao, ogeDao, statsDao, profile) }
    val viewModel: TaskViewModel = viewModel(factory = factory)


    var showHintPopup by remember { mutableStateOf(false) }
    var isMenuOpen by rememberSaveable { mutableStateOf(false) }
    var selectedTab by rememberSaveable { mutableStateOf(TaskTab.Task) }
    var previousTab by rememberSaveable { mutableStateOf(TaskTab.Task) }
    var showDraftCanvas by rememberSaveable { mutableStateOf(false) }

    val draftCanvasState = remember { CanvasState() }
    val coordinator = remember { MathKeyboardCoordinator() }

    val screenWidthPx = remember { context.resources.displayMetrics.widthPixels }
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    // 1) Запускаем загрузку задач из VM
    LaunchedEffect(blockId) {
        Log.d(TAG, "LaunchedEffect(blockId): blockId=$blockId → viewModel.loadTasks()")
        viewModel.loadTasks(blockId)
    }

    // 2) Сами задачи и уже выбранный ID берем из viewModel
    val tasks by remember { viewModel::tasks }
    val completedTaskIDs by remember { viewModel::completedTaskIDs }
    val selectedTaskID by remember { viewModel::selectedTaskID }

    // 3) Находим сам объект TaskEntity по ID
    val selectedTask = selectedTaskID
        ?.let { id -> tasks.find { it.taskID.toLong() == id } }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding() // ← добавлено!
    ) {
        if (selectedTab == TaskTab.Answer && !showDraftCanvas) {
            AnswerTabRefactored(
                viewModel = viewModel,
                coordinator = coordinator,
                screenWidthPx = screenWidthPx,
                isLandscape = isLandscape,
                onBack = onBack,
                onHint = { showHintPopup = true },
                onMenuClick = { isMenuOpen = true },
                selectedTab = selectedTab,
                previousTab = previousTab,
                setPreviousTab = { previousTab = it },
                showDraftCanvas = showDraftCanvas,
                setShowDraftCanvas = { showDraftCanvas = it },
                onTabSelected = { selectedTab = it }
            )
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                // Верхний бар: меню + TabSelector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { isMenuOpen = true },
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color.Transparent)
                    ) {
                        Icon(
                            Icons.Default.Menu,
                            contentDescription = "Меню",
                            tint = Color(0xFF1F2E59),
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    TabSelector(
                        selectedTab = selectedTab,
                        onTabSelected = { tab ->
                            if (tab == TaskTab.Draft) {
                                previousTab = selectedTab
                                showDraftCanvas = true
                            } else {
                                selectedTab = tab
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Box(modifier = Modifier.weight(1f)) {
                    when (selectedTab) {
                        TaskTab.Task -> {
                            TaskContent(task = selectedTask)
                        }
                        TaskTab.Draft -> {
                            // Пусто
                        }
                        else -> { /* handled выше */ }
                    }
                }

                BottomButtons(
                    onClose = onBack,
                    onHint = { showHintPopup = true },
                    onContinue = { viewModel.goToNextTask() }
                )
            }
        }

        // ─── Боковое меню ───
        if (isMenuOpen) {
            val localBlockNumber = when {
                profile.lowercase() == "профиль" && blockId > 21 -> blockId - 21
                profile.lowercase() == "огэ" && blockId in 1..25 -> blockId
                else -> blockId
            }


            SideMenu(
                taskName = "Задание $localBlockNumber",
                taskEntities = tasks,
                completedTaskIDs = completedTaskIDs,
                selectedTaskID = selectedTaskID,
                onTaskSelected = { newId ->
                    Log.d(TAG, "SideMenu.onTaskSelected(): newId=$newId")
                    viewModel.selectTask(newId)  // сразу уведомляем VM
                    isMenuOpen = false
                },
                onDismiss = { isMenuOpen = false },
                profile = profile
            )
        }

        // ─── Черновик поверх всего ───
        if (showDraftCanvas) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
            ) {
                DraftCanvasScreen(
                    onClose = {
                        showDraftCanvas = false
                        selectedTab = previousTab
                    },
                    canvasState = draftCanvasState
                )
            }
        }

        // ─── Всплывающий хинт ───
        if (showHintPopup) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 80.dp, end = 16.dp),
                contentAlignment = Alignment.TopEnd
            ) {
                val hint = selectedTask?.hint.orEmpty()
                HintPopup(
                    hintLatex = hint,
                    onDismiss = { showHintPopup = false }
                )
            }
        }
    }
}

@Composable
fun HintPopup(
    hintLatex: String,
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
                            .background(Color(0xFFD32F2F))
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

                // ─── Подсказка ───
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(top = 4.dp)
                ) {
                    Text(
                        text = "Подсказка:",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    MathWebView(content = hintLatex)
                }
            }
        }
    }
}


@Composable
fun AnswerTabRefactored(
    viewModel: TaskViewModel,
    coordinator: MathKeyboardCoordinator,
    screenWidthPx: Int,
    isLandscape: Boolean,
    onBack: () -> Unit,
    onHint: () -> Unit,
    selectedTab: TaskTab,
    previousTab: TaskTab,
    setPreviousTab: (TaskTab) -> Unit,
    showDraftCanvas: Boolean,
    setShowDraftCanvas: (Boolean) -> Unit,
    onMenuClick: () -> Unit,
    onTabSelected: (TaskTab) -> Unit
) {
    var showAnswerCheck by remember { mutableStateOf(false) }
    var showResultDialog by remember { mutableStateOf(false) }
    var isCorrect by remember { mutableStateOf(false) }
    var showInstructionPopup by remember { mutableStateOf(false) }

    val currentStep = viewModel.steps.getOrNull(viewModel.currentStepIndex)
    val correctMathJS = currentStep?.stepAction.orEmpty()

    if (isLandscape) {
        Row(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onMenuClick,
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color.Transparent)
                    ) {
                        Icon(
                            Icons.Default.Menu,
                            contentDescription = "Меню",
                            tint = Color(0xFF1F2E59),
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    TabSelector(
                        selectedTab = selectedTab,
                        onTabSelected = { tab ->
                            if (tab == TaskTab.Draft) {
                                setPreviousTab(selectedTab)
                                setShowDraftCanvas(true)
                            } else {
                                onTabSelected(tab)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (viewModel.previousSteps.isNotEmpty()) {
                        viewModel.previousSteps.forEach { stepResult ->
                            val step = viewModel.steps.getOrNull(stepResult.stepIndex)
                            if (step != null) {
                                MathWebView(content = step.stepDescription ?: "")
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Ответ:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                                )
                                val cleanAnswer = stepResult.answerLatex.replace("\\lceil", "")
                                    .trim()

                                val htmlSafe = cleanAnswer
                                    .replace("<", "&lt;")
                                    .replace(">", "&gt;")

                                val wrappedAnswerLandscape = "\\($htmlSafe\\)"
                                MathWebView(content = wrappedAnswerLandscape)
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                    }

                    if (currentStep == null && viewModel.steps.isNotEmpty()) {

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                "🎉 Задача решена!",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4CAF50)
                            )
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.resetTaskProgress() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
                            ) {
                                Text("Попробовать ещё раз", color = Color.White)
                            }
                        }
                    } else if (currentStep != null) {
                        AnswerContentSection(
                            description = currentStep.stepDescription.orEmpty(),
                            coordinator = coordinator,
                            onAnswerClick = { showAnswerCheck = true },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }



                    Box(modifier = Modifier.fillMaxWidth()) {
                        if (showAnswerCheck && currentStep != null) {
                            MathAnswerChecker(
                                userLatex = coordinator.getLatexPreview(),
                                correctLatex = correctMathJS,
                                onResult = { result ->
                                    isCorrect = result
                                    val userLatex = coordinator.getLatexPreview()

                                    if (result) {
                                        viewModel.saveCurrentStepAnswer(
                                            isCorrect = true,
                                            answerLatex = userLatex
                                        )

                                        coordinator.resetText()
                                    }

                                    showResultDialog = true
                                    showAnswerCheck = false
                                }
                            )
                        }


                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp, vertical = 0.dp),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    CustomKeyboard(coordinator = coordinator)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    BottomButtons(
                        onClose = onBack,
                        onHint = onHint,
                        onContinue = { viewModel.goToNextTask() }
                    )
                }
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onMenuClick,
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color.Transparent)
                ) {
                    Icon(
                        Icons.Default.Menu,
                        contentDescription = "Меню",
                        tint = Color(0xFF1F2E59),
                        modifier = Modifier.size(32.dp)
                    )
                }

                TabSelector(
                    selectedTab = selectedTab,
                    onTabSelected = { tab ->
                        if (tab == TaskTab.Draft) {
                            setPreviousTab(selectedTab)
                            setShowDraftCanvas(true)
                        } else {
                            onTabSelected(tab)
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                if (viewModel.previousSteps.isNotEmpty()) {
                    viewModel.previousSteps.forEach { stepResult ->
                        val step = viewModel.steps.getOrNull(stepResult.stepIndex)
                        if (step != null) {
                            MathWebView(content = step.stepDescription ?: "")
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Ответ:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                            )
                            val cleanAnswer = stepResult.answerLatex.replace("\\lceil", "")
                                .trim()

                            val htmlSafe = cleanAnswer
                                .replace("<", "&lt;")
                                .replace(">", "&gt;")

                            val wrappedAnswer = "\\($htmlSafe\\)"
                            Log.d("MathRender", "Rendering user answer: $wrappedAnswer")
                            MathWebView(content = wrappedAnswer)
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }

                if (currentStep == null && viewModel.steps.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            "🎉 Задача решена!",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50)
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.resetTaskProgress() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
                        ) {
                            Text("Попробовать ещё раз", color = Color.White)
                        }
                    }
                } else if (currentStep != null) {
                    AnswerContentSection(
                        description = currentStep.stepDescription.orEmpty(),
                        coordinator = coordinator,
                        onAnswerClick = { showAnswerCheck = true },
                        modifier = Modifier.fillMaxWidth()
                    )
                }


                Box(modifier = Modifier.fillMaxWidth()) {
                    if (showAnswerCheck && currentStep != null) {
                        MathAnswerChecker(
                            userLatex = coordinator.getLatexPreview(),
                            correctLatex = correctMathJS,
                            onResult = { result ->
                                isCorrect = result
                                val userLatex = coordinator.getLatexPreview()

                                if (result) {
                                    viewModel.saveCurrentStepAnswer(
                                        isCorrect = true,
                                        answerLatex = userLatex
                                    )

                                    coordinator.resetText()
                                }

                                showResultDialog = true
                                showAnswerCheck = false
                            }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, bottom = 8.dp)
            ) {
                CircleButtonStyled(
                    icon = Icons.AutoMirrored.Filled.Help,
                    iconTint = Color(0xFF2196F3),
                    background = Color.White,
                    shadowColor = Color(0xFF2196F3),
                    onClick = { showInstructionPopup = true },
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

            BottomButtons(
                onClose = onBack,
                onHint = onHint,
                onContinue = { viewModel.goToNextTask() }
            )
        }

    }
    if (showResultDialog) {
        LaunchedEffect(Unit) {
            delay(2000)
            showResultDialog = false
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            AnswerFeedbackToast(isCorrect = isCorrect)
        }
    }
    if (showInstructionPopup) {
        InstructionPopup(onDismiss = { showInstructionPopup = false })
    }
}

@Composable
fun InstructionPopup(
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


@Composable
private fun AnswerContentSection(
    description: String,
    coordinator: MathKeyboardCoordinator,
    onAnswerClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
    ) {
        if (description.isNotEmpty()) {
            MathWebView(content = description)
        } else {
            Text("Шаги отсутствуют.", fontSize = 16.sp, color = Color.Gray)
        }

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
                onClick = onAnswerClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text("ОТВЕТИТЬ", color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        AnswerInputBlock(
            coordinator = coordinator,
            modifier = Modifier.fillMaxWidth()
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


@Composable
fun TaskContent(task: TaskEntity?) {
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
fun MathWebView(
    content: String
) {
    val context = LocalContext.current

    key(content) {
        AndroidView(
            factory = {
                WebView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    settings.javaScriptEnabled = true
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true
                    isVerticalScrollBarEnabled = false
                    isHorizontalScrollBarEnabled = false
                    setBackgroundColor(Color.Transparent.toArgb())

                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            view?.invalidate()
                            view?.requestLayout()
                        }
                    }

                    loadDataWithBaseURL(
                        null,
                        wrapHtml(content),
                        "text/html",
                        "utf-8",
                        null
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
        )
    }
}

fun wrapHtml(content: String): String {
    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <script src="https://cdnjs.cloudflare.com/ajax/libs/mathjax/3.2.2/es5/tex-mml-chtml.js" async></script>
            <style>
                html, body {
                    margin: 0;
                    padding: 0;
                    background-color: transparent;
                    font-family: sans-serif;
                    font-size: 18px;
                    color: #000000;
                }
                #content {
                    display: block;
                    width: 100%;
                }
                svg {
                    width: 100%;
                    height: auto;
                    display: block;
                }
                img {
                    max-width: 100%;
                    height: auto;
                }
            </style>
        </head>
        <body>
            <div id="content">
                $content
            </div>
            <script>
                window.getRealHeight = function() {
                    const content = document.getElementById('content');
                    const rect = content.getBoundingClientRect();
                    return Math.max(rect.height, content.scrollHeight);
                };
            </script>
        </body>
        </html>
    """.trimIndent()
}







enum class TaskTab(val title: String) {
    Task("Задание"),
    Answer("Ответ"),
    Draft("Черновик")
}

@Composable
fun TabSelector(
    selectedTab: TaskTab,
    onTabSelected: (TaskTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        TaskTab.entries.forEach { tab ->
            val interactionSource = remember { MutableInteractionSource() }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp)) // если нужно
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null // ✅ Отключили ripple
                    ) { onTabSelected(tab) }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = tab.title.uppercase(),
                    color = if (selectedTab == tab) Color.Green else Color(0xFF1F2E59),
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
}


@Composable
fun BottomButtons(
    onClose: () -> Unit,
    onHint: () -> Unit,
    onContinue: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircleButtonStyled(
            icon = Icons.Default.Close,
            iconTint = Color.Red,
            background = Color.White,
            shadowColor = Color.Red,
            onClick = onClose,
            size = 56.dp
        )

        Spacer(modifier = Modifier.width(24.dp))

        CircleButtonStyled(
            icon = Icons.Default.Lightbulb,
            iconTint = Color(0xFFFFC107),
            background = Color(0xFFFFF8E1),
            shadowColor = Color(0xFFFFC107),
            onClick = onHint,
            size = 72.dp // крупнейшая центральная кнопка
        )

        Spacer(modifier = Modifier.width(24.dp))

        CircleButtonStyled(
            icon = Icons.Default.PlayArrow,
            iconTint = Color(0xFF4CAF50),
            background = Color.White,
            shadowColor = Color(0xFF4CAF50),
            onClick = onContinue,
            size = 56.dp
        )
    }
}



@Composable
fun CircleButtonStyled(
    icon: ImageVector,
    iconTint: Color,
    background: Color,
    shadowColor: Color,
    onClick: () -> Unit,
    size: Dp = 56.dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .shadow(elevation = 6.dp, shape = CircleShape, ambientColor = shadowColor, spotColor = shadowColor)
            .background(background, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(size * 0.5f)
        )
    }
}

@Composable
fun SideMenu(
    taskName: String,
    taskEntities: List<TaskEntity>,
    completedTaskIDs: Set<Long>,
    selectedTaskID: Long?,
    onTaskSelected: (Long) -> Unit,
    onDismiss: () -> Unit,
    profile: String // <--- добавить сюда
) {
    Row(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .width(250.dp)
                .fillMaxHeight()
                .background(Color.White)
                .padding(8.dp)
                // Добавляем прокрутку списка внутри
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
                            isCompleted -> Color(0xFFE8F5E9) // светло-зелёный фон
                            else -> Color.Transparent
                        },
                        contentColor = if (isCompleted) Color(0xFF4CAF50) else Color(0xFF1F2E59)
                    )
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Вопрос ${index + 1}", modifier = Modifier.weight(1f))

                        if (isCompleted) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50)
                            )
                        } else if (isSelected) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color.Blue
                            )
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
fun AnswerFeedbackToast(
    isCorrect: Boolean,
    modifier: Modifier = Modifier
) {
    PlayFeedbackSound(isCorrect)
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            Text(
                text = if (isCorrect) "✅ Правильно!" else "❌ Неправильно",
                color = if (isCorrect) Color(0xFF4CAF50) else Color(0xFFF44336),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
            )
        }
    }
}
