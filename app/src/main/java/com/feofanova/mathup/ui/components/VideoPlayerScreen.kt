package com.feofanova.mathup.ui.components

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.feofanova.mathup.ui.screens.main.SetStatusBarColor

@Composable
fun VideoPlayerScreen(
    videoUrl: String,
    onBack: () -> Unit
) {
    SetStatusBarColor(color = Color(0xFF1F2E59), darkIcons = false)
    val context = LocalContext.current

    LaunchedEffect(videoUrl) {
        val intent = Intent(context, VideoWebActivity::class.java)
        intent.putExtra("VIDEO_URL", videoUrl)
        context.startActivity(intent)
        onBack() // возвращаемся назад сразу или по желанию
    }

    // Пока просто отображаем заглушку
    Box(modifier = Modifier.fillMaxSize().background(Color.Black))
}

