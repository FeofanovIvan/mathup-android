package com.feofanova.mathup.ui.screens.preparation


import android.content.Context
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.feofanova.mathup.data.local.db.MathUpDatabase
import com.feofanova.mathup.ui.screens.main.SetStatusBarColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun FormulasPreparationScreen(
    blockId: Long,
    profile: String,
    onBack: () -> Unit
) {
    SetStatusBarColor(color = Color(0xFF1F2E59), darkIcons = false)
    val context = LocalContext.current
    var htmlContent by remember { mutableStateOf("<p>Загрузка...</p>") }

    LaunchedEffect(blockId, profile) {
        htmlContent = loadFormulasHtml(context, blockId, profile)
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // 🔹 Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1F2E59))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Назад",
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "Формулы",
                color = Color.White,
                fontSize = MaterialTheme.typography.titleMedium.fontSize
            )

            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.width(60.dp))
        }

        // 🔹 WebView
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    webViewClient = WebViewClient()
                    loadDataWithBaseURL(null, htmlContent, "text/html", "utf-8", null)
                }
            },
            update = {
                it.loadDataWithBaseURL(null, htmlContent, "text/html", "utf-8", null)
            }
        )
    }
}

suspend fun loadFormulasHtml(context: Context, blockId: Long, profile: String): String {
    return withContext(Dispatchers.IO) {
        val dao = when (profile) {
            "ОГЭ" -> com.feofanova.mathup.data.local.db.MathUpOgeDatabase.getInstance(context).appDao()
            else  -> com.feofanova.mathup.data.local.db.MathUpDatabase.getInstance(context).appDao()
        }

        val formulas = dao.getFormulasForBlock(blockId)
        if (formulas.isEmpty()) {
            return@withContext "<p style='color:gray;text-align:center;padding-top:32px;'>Формул для этого блока нет.</p>"
        }

        val rows = formulas.joinToString("") { formula ->
            val name = formula.name ?: ""
            val latex = formula.formula ?: ""
            """
            <tr>
                <td style=\"padding: 8px 8px; font-size: 16px; vertical-align: top; color: #1E3050; width: 50%; font-weight: bold;\">
                    $name
                </td>
                <td style=\"padding: 16px 8px; color: #1E3050; \">
                \(${latex}\)
                </td>
            </tr>
            """
        }

        """
        <!DOCTYPE html>
        <html>
        <head>
        <script src="https://polyfill.io/v3/polyfill.min.js?features=es6"></script>
            <script id="MathJax-script" async 
              src="https://cdn.jsdelivr.net/npm/mathjax@3/es5/tex-mml-chtml.js">
            </script>
            <meta charset=\"UTF-8\">
            <script src=\"https://polyfill.io/v3/polyfill.min.js?features=es6\"></script>
            <script id=\"MathJax-script\" async src=\"https://cdn.jsdelivr.net/npm/mathjax@3/es5/tex-mml-chtml.js\"></script>
            <style>
                body {
                    font-family: sans-serif;
                    font-size: 16px;
                    padding: 5px;
                    margin: 0;
                    background-color: white;
                }
                table {
                    width: 100%;
                    border-collapse: separate;
                    border-spacing: 2px 5px;
                }
                tr:nth-child(even) {
                    background-color: #f2f4f8;
                }
                tr:nth-child(odd) {
                    background-color: #ffffff;
                }
                td {
                    
                    vertical-align: middle;
                }
            </style>
        </head>
        <body>
            <table>
                $rows
            </table>
        </body>
        </html>
        """
    }
}

