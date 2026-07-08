package com.feofanova.mathup.ui.screens.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.navigation.NavHostController
import com.feofanova.mathup.R
import com.feofanova.mathup.sound.LocalSoundPlayer


@Composable
fun HomeScreen(navController: NavHostController, selectedProfile: String) {
    val scrollState = rememberScrollState()
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedBackground()
        AnimatedBlobs()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(top = 22.dp, bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            cards.forEach { card ->
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .wrapContentSize(Alignment.Center)
                ) {
                    CircleCardItem(card = card, screenWidth = screenWidth) {
                        when (card.title) {
                            "Правила" -> navController.navigate("rules/$selectedProfile")
                            "Экзамен" -> navController.navigate("exam/$selectedProfile")
                            "Подготовка" -> navController.navigate("preparation/$selectedProfile")
                            "Справочник" -> navController.navigate("reference/$selectedProfile")
                            "Формулы" -> navController.navigate("formulas/$selectedProfile")
                            "Видеоразбор" -> navController.navigate("video/$selectedProfile")
                            "Игры" -> navController.navigate("games/$selectedProfile")
                        }
                    }
                }
            }
        }
    }
}

// ---- Card Data

data class CircleCard(
    val title: String,
    val imageRes: Int,
    val alignLeft: Boolean
)

val cards = listOf(
    CircleCard("Правила", R.drawable.rb_10492, true),
    CircleCard("Экзамен", R.drawable.rb_13742, false),
    CircleCard("Подготовка", R.drawable.rb_2754, true),
    CircleCard("Справочник", R.drawable.rb_2149341898, false),
    CircleCard("Формулы", R.drawable.cartoon, true),
    CircleCard("Видеоразбор", R.drawable.analysis, false),
    CircleCard("Игры", R.drawable.cute, true)
)

// ---- Circle Card

@Composable
fun CircleCardItem(
    card: CircleCard,
    screenWidth: Dp,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    // 🔹 Адаптивный отступ: ближе к центру
    val horizontalPadding = (screenWidth * 0.06f).coerceAtLeast(12.dp)
    val imageSize = (screenWidth * 0.26f).coerceIn(90.dp, 140.dp) // 🔹 размер круга
    val soundPlayer = LocalSoundPlayer.current
    Row(
        modifier = Modifier
            .clickable(
                onClick = {
                    soundPlayer.playClick() // 🔊 Проиграть звук
                    onClick()               // 📦 Выполнить переданное действие
                },
                indication = null, // 🔕 убираем визуальный эффект
                interactionSource = remember { MutableInteractionSource() }
            )
            .fillMaxWidth()
            .scale(scale)
            .padding(horizontal = horizontalPadding),
        horizontalArrangement = if (card.alignLeft) Arrangement.Start else Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (card.alignLeft) {
            CircleImage(card.imageRes, imageSize)
            Spacer(modifier = Modifier.width(20.dp))
            CardTitle(text = card.title)
        } else {
            CardTitle(text = card.title)
            Spacer(modifier = Modifier.width(20.dp))
            CircleImage(card.imageRes, imageSize)
        }
    }
}

@Composable
fun CircleImage(imageRes: Int, size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .shadow(8.dp, shape = CircleShape)
            .background(Color.White, shape = CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = imageRes),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(size * 0.75f)
        )
    }
}


@Composable
fun CardTitle(text: String) {
    Text(
        text = text,
        fontSize = 20.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 0.8.sp,
        color = Color(0xFF1F2E59),
        style = MaterialTheme.typography.titleMedium.copy(
            shadow = Shadow(
                color = Color(0x33000000),
                offset = Offset(1f, 1f),
                blurRadius = 1f
            )
        )
    )
}


// ---- Backgrounds

@Composable
fun AnimatedBackground() {
    val animate = rememberInfiniteTransition()
    val offset by animate.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 10000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFEFF5FF), // #F0F6FF в Swift → #EFF5FF точнее
                        Color.White,
                        Color(0xFFEBF2FC)
                    ),
                    start = Offset(offset * 1000f, offset * 1000f),
                    end = Offset((1f - offset) * 1000f, (1f - offset) * 1000f)
                )
            )
    )
}

@Composable
fun AnimatedBlobs(
    modifier: Modifier = Modifier
) {
    // бесконечный переход
    val transition = rememberInfiniteTransition()

    // три пары анимированных смещений
    val offset1X by transition.animateFloat(
        initialValue = -80f, targetValue = 100f,
        animationSpec = infiniteRepeatable(
            tween(durationMillis = 12_000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    val offset1Y by transition.animateFloat(
        initialValue =  80f, targetValue = -100f,
        animationSpec = infiniteRepeatable(
            tween(durationMillis = 12_000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val offset2X by transition.animateFloat(
        initialValue =  60f, targetValue = -120f,
        animationSpec = infiniteRepeatable(
            tween(durationMillis = 10_000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    val offset2Y by transition.animateFloat(
        initialValue = -90f, targetValue =  100f,
        animationSpec = infiniteRepeatable(
            tween(durationMillis = 10_000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val offset3X by transition.animateFloat(
        initialValue = -70f, targetValue =  90f,
        animationSpec = infiniteRepeatable(
            tween(durationMillis = 14_000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    val offset3Y by transition.animateFloat(
        initialValue = -100f, targetValue =  60f,
        animationSpec = infiniteRepeatable(
            tween(durationMillis = 14_000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    // Преобразуем флоаты в Dp
    val density = LocalDensity.current
    val dp1X = with(density) { offset1X.dp }
    val dp1Y = with(density) { offset1Y.dp }
    val dp2X = with(density) { offset2X.dp }
    val dp2Y = with(density) { offset2Y.dp }
    val dp3X = with(density) { offset3X.dp }
    val dp3Y = with(density) { offset3Y.dp }

    Box(
        modifier = modifier
            .fillMaxSize()
            // Размываем всё содержимое Box-а
            .blur(90.dp)
    ) {
        // Блоб 1
        Box(
            modifier = Modifier
                .size(350.dp)
                .offset(x = dp1X, y = dp1Y)
                .background(Color(0xFF9C27B0).copy(alpha = 0.25f), CircleShape)
        )
        // Блоб 2
        Box(
            modifier = Modifier
                .size(280.dp)
                .offset(x = dp2X, y = dp2Y)
                .background(Color(0xFF2196F3).copy(alpha = 0.25f), CircleShape)
        )
        // Блоб 3
        Box(
            modifier = Modifier
                .size(300.dp)
                .offset(x = dp3X, y = dp3Y)
                .background(Color(0xFFE91E63).copy(alpha = 0.25f), CircleShape)
        )
    }
}