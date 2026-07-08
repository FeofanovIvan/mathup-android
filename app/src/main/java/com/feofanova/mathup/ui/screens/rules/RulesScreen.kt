package com.feofanova.mathup.ui.screens.rules

import android.content.res.Configuration
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

import com.feofanova.mathup.ui.screens.main.SetStatusBarColor
import com.feofanova.mathup.R

@Composable
fun RulesScreen(profile: String, onBack: () -> Unit) {
    SetStatusBarColor(color = Color(0xFF1F2E59), darkIcons = false)
    val configuration = LocalConfiguration.current
    val windowInfo = LocalWindowInfo.current
    val screenWidth = with(LocalDensity.current) { windowInfo.containerSize.width.toDp() }
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val fileName = if (profile == "ОГЭ") "rules_oge" else "rules"
    val image1 = if (profile == "ОГЭ") R.drawable.blanc_1 else R.drawable.list1
    val image2 = if (profile == "ОГЭ") R.drawable.blanc_2 else R.drawable.list2

    // Сюда WebView отдаст высоту в пикселях
    var contentHeightPx by remember { mutableFloatStateOf(0f) }
    // Переводим физ. пиксели в dp
    val contentHeightDp = with(LocalDensity.current) { contentHeightPx.toDp() }

    val scaleFactor = if (screenWidth < 600.dp) {
        if (isLandscape) 1f else 1f
    } else {
        if (isLandscape) 1f else 1f
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1F2E59))
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Назад",
                    tint = Color.White
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Правила экзамена",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.size(48.dp)) // Чтобы сбалансировать иконку слева
        }

        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .background(Color.White)
                .padding(vertical = 16.dp)
        ) {
            // Применяем scaleFactor к высоте
            RulesWebView(
                fileName = fileName,
                onHeightChanged = { hPx -> contentHeightPx = hPx },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(contentHeightDp * scaleFactor)
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .shadow(5.dp)
            )


            Spacer(Modifier.height(20.dp))


            Column(Modifier.padding(horizontal = 16.dp)) {
                ZoomableImageContainer(title = "Бланк 1", resId = image1)

                Spacer(modifier = Modifier.height(16.dp))

                ZoomableImageContainer(title = "Бланк 2", resId = image2)
            }
        }
    }
}

@Composable
fun RulesWebView(
    fileName: String,
    onHeightChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            // Чтобы wrap_content работало корректно:
            WebView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )

                settings.javaScriptEnabled = true
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                settings.domStorageEnabled = true

                addJavascriptInterface(object {
                    @JavascriptInterface
                    fun onContentHeight(heightPx: Float) {
                        onHeightChanged(heightPx)
                    }
                }, "AndroidInterface")

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String?) {
                        // возвращаем реальную высоту в физ. пикселях
                        view.evaluateJavascript(
                            """
                            (function() {
                                const h = document.documentElement.scrollHeight * window.devicePixelRatio;
                                AndroidInterface.onContentHeight(h);
                            })();
                            """.trimIndent(),
                            null
                        )
                    }
                }

                loadUrl("file:///android_asset/$fileName.html")
            }
        }
    )
}



@Composable
fun ZoomableImageContainer(title: String, resId: Int) {
    val scale = remember { mutableFloatStateOf(1f) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .shadow(4.dp, shape = RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF2F4F7)) // светлый стиль, общий фон
            .padding(12.dp)
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF1F2E59),
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 200.dp, max = 400.dp) // рамка, в которой можно зумить
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = resId),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .graphicsLayer(
                        scaleX = scale.floatValue.coerceIn(1f, 3f),
                        scaleY = scale.floatValue.coerceIn(1f, 3f)
                    )
                    .pointerInput(Unit) {
                        detectTransformGestures { _, _, zoom, _ ->
                            scale.floatValue *= zoom
                        }
                    }
            )
        }
    }
}
