package com.feofanova.mathup.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AnswerInputBlock(
    coordinator: MathKeyboardCoordinator,
    modifier: Modifier = Modifier,
    onAnswerChanged: (String) -> Unit = {}
) {
    var latexText by remember { mutableStateOf(coordinator.getLatexPreview()) }
    var webViewHeight by remember { mutableIntStateOf(24) }

    // Подписка на обновление LaTeX
    LaunchedEffect(Unit) {
        coordinator.onLatexUpdate = { updatedLatex ->
            latexText = updatedLatex.replace("\\lceil", "\\lceil")
            onAnswerChanged(latexText) // 👈 теперь пробрасываем наверх
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 24.dp)
            .padding(vertical = 8.dp)
    ) {
        KeyboardPreviewWebView(
            content = latexText,
            onHeightCalculated = { height -> webViewHeight = height },
            modifier = Modifier
                .fillMaxWidth()
                .height(webViewHeight.dp)
        )
    }
}

