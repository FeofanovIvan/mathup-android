package com.feofanova.mathup.ui.screens.preparation

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.feofanova.mathup.data.local.db.MathUpDatabase
import com.feofanova.mathup.data.local.db.MathUpOgeDatabase
import com.feofanova.mathup.ui.navigation.Routes
import com.feofanova.mathup.ui.screens.main.SetStatusBarColor
import kotlinx.coroutines.launch


@Composable
fun PreparationScreen(profile: String, onBack: () -> Unit, navController: NavController, viewModel: TaskViewModel) {
    SetStatusBarColor(color = Color(0xFF1F2E59), darkIcons = false)
    val blockIDs = when (profile) {
        "Профиль" -> (22..40).toList()
        "ОГЭ"     -> (1..25).toList()
        else      -> (1..21).toList() // База
    }
    val blockProgressMap = remember { mutableStateMapOf<Int, Float>() }

    LaunchedEffect(profile) {
        viewModel.loadBlockProgress(blockIDs) { result ->
            blockProgressMap.putAll(result)
        }
    }

    val numberOfTasks = when (profile) {
        "Профиль" -> 19
        "ОГЭ"     -> 25
        else      -> 21 // База
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFFF2F9FF), Color.White)
                )
            )
    ) {
        // 🔹 Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1F2E59))
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White)
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "Подготовка к экзамену",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.weight(1f))
        }

        // 🔹 Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(8.dp))

            repeat(numberOfTasks) { index ->
                val displayIndex = index + 1
                val actualBlockID = if (profile == "Профиль") displayIndex + 21 else displayIndex

                PreparationBlockCard(
                    title = "Задание $displayIndex",
                    blockID = actualBlockID,
                    profile = profile,
                    progress = blockProgressMap[actualBlockID] ?: 0f, // 👈 передаём
                    navController = navController,
                    onBlockClick = { blockId ->
                        navController.navigate("${Routes.TASKS}/$blockId/$profile")
                    },
                    onVideoClick = { videoUrl ->
                        navController.navigate("${Routes.VIDEO_PLAYER}/${Uri.encode(videoUrl)}")
                    },
                    onReferenceClick = { blockId, prof ->
                        navController.navigate("referenceDetail/$blockId/$prof")
                    }
                )
            }

        }
    }
}
@Composable
fun PreparationBlockCard(
    title: String,
    blockID: Int,
    profile: String,
    progress: Float,
    onReferenceClick: (Long, String) -> Unit,
    onVideoClick: (String) -> Unit,
    onBlockClick: (Int) -> Unit,
    navController: NavController
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() } // если Snackbar используешь

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onBlockClick(blockID) }
                .padding(16.dp)
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color(0xFF1F2E59)
            )

            Spacer(Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(50)),
                color = Color.Green,
                trackColor = Color(0xFFE0E0E0),
            )

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PreparationButton(
                    text = "формулы",
                    onClick = { navController.navigate("${Routes.FORMULAS_DETAIL}/$blockID/$profile") },
                    modifier = Modifier.weight(1f)
                )

                PreparationButton(
                    text = "справочник",
                    onClick = { onReferenceClick(blockID.toLong(), profile) },
                    modifier = Modifier.weight(1f)
                )

                PreparationButton(
                    text = "видео",
                    onClick = {
                        scope.launch {
                            val dao = when (profile) {
                                "ОГЭ"     -> MathUpOgeDatabase.getInstance(context).appDao()
                                else      -> MathUpDatabase.getInstance(context).appDao()
                            }

                            val block = dao.getBlockById(blockID)
                            val videoUrl = block?.videoLink

                            if (!videoUrl.isNullOrBlank()) {
                                onVideoClick(videoUrl)
                            } else {
                                snackbarHostState.showSnackbar("Видео недоступно для блока $blockID")
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}



@Composable
fun PreparationButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier // ⚠️ обязателен
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .padding(horizontal = 4.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F2E59))
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

