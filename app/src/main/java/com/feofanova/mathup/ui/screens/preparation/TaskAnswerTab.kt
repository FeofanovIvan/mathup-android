package com.feofanova.mathup.ui.screens.preparation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.feofanova.mathup.ui.components.AnswerInputBlock
import com.feofanova.mathup.ui.components.CustomKeyboard
import com.feofanova.mathup.ui.components.MathAnswerChecker
import com.feofanova.mathup.ui.components.MathKeyboardCoordinator
import kotlinx.coroutines.delay

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

    val uiState = viewModel.uiState
    val currentStep = uiState.steps.getOrNull(uiState.currentStepIndex)
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
                    if (uiState.previousSteps.isNotEmpty()) {
                        uiState.previousSteps.forEach { stepResult ->
                            val step = uiState.steps.getOrNull(stepResult.stepIndex)
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
                                val cleanAnswer = stepResult.answerLatex.replace("\\lceil", "").trim()
                                val htmlSafe = cleanAnswer.replace("<", "&lt;").replace(">", "&gt;")
                                val wrappedAnswerLandscape = "\\($htmlSafe\\)"
                                MathWebView(content = wrappedAnswerLandscape)
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                    }

                    if (currentStep == null && uiState.steps.isNotEmpty()) {
                        TaskSolvedContent(onReset = { viewModel.resetTaskProgress() })
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
                if (uiState.previousSteps.isNotEmpty()) {
                    uiState.previousSteps.forEach { stepResult ->
                        val step = uiState.steps.getOrNull(stepResult.stepIndex)
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
                            val cleanAnswer = stepResult.answerLatex.replace("\\lceil", "").trim()
                            val htmlSafe = cleanAnswer.replace("<", "&lt;").replace(">", "&gt;")
                            val wrappedAnswer = "\\($htmlSafe\\)"
                            MathWebView(content = wrappedAnswer)
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }

                if (currentStep == null && uiState.steps.isNotEmpty()) {
                    TaskSolvedContent(onReset = { viewModel.resetTaskProgress() })
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
private fun TaskSolvedContent(onReset: () -> Unit) {
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
            onClick = onReset,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
        ) {
            Text("Попробовать ещё раз", color = Color.White)
        }
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
                modifier = Modifier.padding(20.dp)
            ) {
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
