package com.feofanova.mathup.ui.components

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun KeyboardPreviewWebView(
    content: String,
    onHeightCalculated: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.allowFileAccess = true
                settings.domStorageEnabled = true
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false

                // Класс для получения высоты
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        // Вызываем JS, чтобы получить высоту
                        evaluateJavascript(
                            """
                            (function() {
                                return document.body.scrollHeight.toString();
                            })();
                            """.trimIndent()
                        ) { value ->
                            value?.replace("\"", "")?.toIntOrNull()?.let { height ->
                                onHeightCalculated(height)
                            }
                        }
                    }
                }

                loadDataWithBaseURL(null, wrapHtml(content), "text/html", "UTF-8", null)
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL(null, wrapHtml(content), "text/html", "UTF-8", null)
        },
        modifier = modifier
    )
}
fun wrapHtml(content: String): String {
    val escaped = content
        .replace("\\", "\\\\")
        .replace("'", "\\'")
        .replace("\n", "")

    return """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <title>Math Preview</title>
            <link href="https://cdn.jsdelivr.net/npm/katex@0.13.0/dist/katex.min.css" rel="stylesheet">
            <script src="https://cdn.jsdelivr.net/npm/katex@0.13.0/dist/katex.min.js"></script>
            <style>
                body {
                    font-size: 18px;
                    text-align: center;
                    margin: 0;
                    padding: 0px;
                    background-color: transparent;
                }
                #mathRender {
                    border: 2px solid transparent;
                    padding: 0px;
                    background-color: transparent;
                    min-height: 20px;
                    display: flex;
                }
            </style>
        </head>
        <body>
            <div id="mathRender"></div>
            <script>
                try {
                    katex.render('$escaped', document.getElementById('mathRender'), {
                        throwOnError: false
                    });
                } catch (e) {
                    document.getElementById('mathRender').innerText = 'Ошибка рендера';
                }
            </script>
        </body>
        </html>
    """.trimIndent()
}
