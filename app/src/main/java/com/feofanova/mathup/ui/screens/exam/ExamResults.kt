package com.feofanova.mathup.ui.screens.exam

import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.feofanova.mathup.domain.repository.ExamAnswerCheckItem
import com.feofanova.mathup.ui.components.PlayFinalSound
import com.feofanova.mathup.ui.components.wrapCheckerHtml

@Composable
fun SingleAnswerCheckWebView(
    item: ExamAnswerCheckItem,
    onComplete: (taskId: Int, isCorrect: Boolean) -> Unit
) {
    val context = LocalContext.current
    val isHandled = remember { mutableStateOf(false) }

    if (item.userAnswer == "—" || item.userAnswer.trim().isEmpty()) {
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
                        evaluateJavascript("processComparison()", null)
                    }
                }

                addJavascriptInterface(object {
                    @JavascriptInterface
                    fun postMessage(msg: String) {
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
    results: List<Pair<ExamAnswerCheckItem, Boolean>>,
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
        buildResultHtml(results.map { (item, isCorrect) ->
            ExamAnswerCheckItem(
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
                modifier = Modifier.padding(20.dp),
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
                    HtmlResultView(htmlContent = html)
                }
            }
        }
    }
}

fun buildResultHtml(results: List<ExamAnswerCheckItem>): String {
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
            document.getElementById('user${index + 1}').innerHTML = katex.renderToString("$escapedUser", { throwOnError: false });
            try {
                const correctExpr${index + 1} = math.parse("$escapedCorrect").toTex();
                document.getElementById('correct${index + 1}').innerHTML = katex.renderToString(correctExpr${index + 1}, { throwOnError: false });
            } catch (e) {
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
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                loadDataWithBaseURL(null, htmlContent, "text/html", "utf-8", null)
            }
        },
        modifier = Modifier.fillMaxWidth().height(400.dp)
    )
}
