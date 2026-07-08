package com.feofanova.mathup.ui.screens.news

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.feofanova.mathup.R

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

// --- Модель ---
data class NewsItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val content: String = "",
    val date: String = ""
) {
    val dateObject: Date?
        get() = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).parse(date)
}

@Composable
fun NewsScreen(navController: NavHostController) {
    var newsList by remember { mutableStateOf(listOf<NewsItem>()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        newsList = fetchNewsFromFirestore()
        isLoading = false
    }

    // Если экрана нет внутри Scaffold с topBar, учтём высоту статус-бара
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(listOf(Color(0xFFF2F9FF), Color.White))
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(16.dp)
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = topInset ,    // ⬅️ аккуратный отступ сверху
                    start = 16.dp,
                    end = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(newsList.sortedByDescending { it.dateObject }) { item ->
                    NewsCardView(item)
                }
            }
        }
    }
}

@Composable
fun NewsCardView(item: NewsItem) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.drawable.logo_only),
                    contentDescription = null,
                    modifier = Modifier
                        .size(44.dp)
                        .padding(end = 8.dp)
                )
                Column {
                    Text(
                        text = item.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF1F2E59)
                    )
                    Text(
                        text = item.date,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = item.content,
                fontSize = 14.sp,
                color = Color.Black
            )
        }
    }
}

suspend fun fetchNewsFromFirestore(): List<NewsItem> {
    return try {
        val snapshot = FirebaseFirestore.getInstance().collection("news").get().await()
        snapshot.documents.mapNotNull { doc ->
            val data = doc.data ?: return@mapNotNull null
            NewsItem(
                id = doc.id,
                title = data["title"] as? String ?: "Без заголовка",
                content = data["content"] as? String ?: "Нет описания",
                date = data["date"] as? String ?: "—"
            )
        }
    } catch (e: Exception) {
        emptyList()
    }
}