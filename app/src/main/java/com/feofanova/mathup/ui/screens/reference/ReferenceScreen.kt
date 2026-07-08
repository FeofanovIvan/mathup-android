package com.feofanova.mathup.ui.screens.reference

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.feofanova.mathup.ui.screens.main.SetStatusBarColor


@Composable
fun ReferenceScreen(
    profile: String,
    onNavigateToDetail: (Long) -> Unit,
    onBack: () -> Unit
) {
    SetStatusBarColor(color = Color(0xFF1F2E59), darkIcons = false)
    val numberOfTasks = when (profile) {
        "Профиль" -> 19
        "ОГЭ" -> 25
        else -> 21
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFF2F9FF),
                        Color.White
                    )
                )
            )
    ) {
        // 🔹 Top bar — как на других экранах
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1F2E59))
                .windowInsetsPadding(WindowInsets.statusBars)
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
                text = "Справочник",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.width(60.dp)) // пустота вместо кнопки справа
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(numberOfTasks) { index ->
                val blockId = when (profile) {
                    "Профиль" -> index + 22
                    "ОГЭ" -> index + 1
                    else -> index + 1
                }

                ReferenceBlock(title = "Задание ${index + 1}") {
                    onNavigateToDetail(blockId.toLong())
                }
            }
        }
    }
}

@Composable
fun ReferenceBlock(title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .shadow(4.dp, shape = RoundedCornerShape(10.dp))
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White)
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Book,
            contentDescription = null,
            tint = Color(0xFF1F2E59),
            modifier = Modifier.size(32.dp)
        )

        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF1F2E59),
            modifier = Modifier.weight(1f).padding(start = 12.dp)
        )

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color(0xFF1F2E59)
        )
    }
}
