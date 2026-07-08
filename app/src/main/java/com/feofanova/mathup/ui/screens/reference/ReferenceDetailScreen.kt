package com.feofanova.mathup.ui.screens.reference

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.feofanova.mathup.data.local.db.MathUpDatabase
import com.feofanova.mathup.ui.screens.main.SetStatusBarColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


@Composable
fun ReferenceDetailScreen(
    blockId: Long,
    profile: String,
    onBack: () -> Unit
) {
    SetStatusBarColor(color = Color(0xFF1F2E59), darkIcons = false)
    val context = LocalContext.current
    var title by remember { mutableStateOf("Справочник") }
    var htmlContent by remember { mutableStateOf("<p>Загрузка...</p>") }

    LaunchedEffect(blockId, profile) {
        val (loadedTitle, loadedHtml) = loadContent(context, blockId, profile)
        title = loadedTitle
        htmlContent = loadedHtml
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
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
                text = title,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.width(60.dp))
        }
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            factory = { context ->
                WebView(context).apply {
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        builtInZoomControls = true
                        displayZoomControls = false
                        loadWithOverviewMode = true
                        useWideViewPort = true
                    }
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



suspend fun loadContent(context: Context, blockId: Long, profile: String): Pair<String, String> {
    return withContext(Dispatchers.IO) {
        val dao = when (profile) {
            "ОГЭ" -> com.feofanova.mathup.data.local.db.MathUpOgeDatabase.getInstance(context).appDao()
            else -> MathUpDatabase.getInstance(context).appDao()
        }

        val block = dao.getBlockById(blockId)
        val title = block?.name ?: "Задание $blockId"
        val html = block?.referenceMaterial ?: "<p>Материал не найден</p>"
        title to html
    }
}

