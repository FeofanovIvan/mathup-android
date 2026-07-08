package com.feofanova.mathup.ui.components

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.*
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat


@Suppress("DEPRECATION")
class VideoWebActivity : ComponentActivity() {

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = 0xFF1F2E59.toInt() // глубокий синий
        WindowCompat.setDecorFitsSystemWindows(window, true)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false

        val videoUrl = intent.getStringExtra("VIDEO_URL") ?: return

        val webView = WebView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                mediaPlaybackRequiresUserGesture = false
                loadWithOverviewMode = true
                useWideViewPort = true
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                userAgentString =
                    "Mozilla/5.0 (Linux; Android 12; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
            }

            webChromeClient = WebChromeClient()
            webViewClient = WebViewClient()

            val html = """
                <html>
                    <body style="margin:0;padding:0;background:black;">
                        <iframe width="100%" height="100%" src="$videoUrl" frameborder="0" allow="autoplay; fullscreen" allowfullscreen></iframe>
                    </body>
                </html>
            """.trimIndent()

            loadDataWithBaseURL("https://vk.com", html, "text/html", "utf-8", null)
        }

        setContentView(webView)
    }
}
