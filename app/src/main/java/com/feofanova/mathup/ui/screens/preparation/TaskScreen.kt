package com.feofanova.mathup.ui.screens.preparation

import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
import com.feofanova.mathup.ui.screens.formulas.PlayFeedbackSound


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
    LaunchedEffect(blockId) {
        viewModel.loadTasks(blockId)
    }
    val uiState = viewModel.uiState
    val selectedTask = uiState.selectedTaskId
        ?.let { id -> uiState.tasks.find { it.taskID.toLong() == id } }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
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
        if (isMenuOpen) {
            val localBlockNumber = when {
                profile.lowercase() == "профиль" && blockId > 21 -> blockId - 21
                profile.lowercase() == "огэ" && blockId in 1..25 -> blockId
                else -> blockId
            }


            SideMenu(
                taskName = "Задание $localBlockNumber",
                taskEntities = uiState.tasks,
                completedTaskIDs = uiState.completedTaskIds,
                selectedTaskID = uiState.selectedTaskId,
                onTaskSelected = { newId ->
                    viewModel.selectTask(newId)
                    isMenuOpen = false
                },
                onDismiss = { isMenuOpen = false },
                profile = profile
            )
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
                        selectedTab = previousTab
                    },
                    canvasState = draftCanvasState
                )
            }
        }
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
fun TaskContent(task: TaskEntity?) {
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
                    .clip(RoundedCornerShape(4.dp))
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null
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
