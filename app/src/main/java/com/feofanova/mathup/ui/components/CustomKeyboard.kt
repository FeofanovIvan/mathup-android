package com.feofanova.mathup.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.feofanova.mathup.sound.LocalSoundPlayer


@Composable
fun CustomKeyboard(
    coordinator: MathKeyboardCoordinator
) {
    // Список «меток» (текстов) кнопок для каждой строки:
    val keysRow1 = listOf("sin", "cos", "tg", "ctg", "asin", "acs", "atg", "actg", "<", "U", ">")
    val keysRow2 = listOf("+", "1", "2", "3", "x", "y", "°", "CE")
    val keysRow3 = listOf("-", "4", "5", "6", "a", "b", "c", "стереть")
    val keysRow4 = listOf("×", "7", "8", "9", "√", "aⁿ", "log", "e")
    val keysRow5 = listOf("\\", ".", "0", "=", "⏎", "(", ")", "π")

    // BoxWithConstraints позволяет узнать maxWidth/maxHeight в dp:
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
    ) {
        // Порог, при котором считаем «планшетной» раскладку:
        val isTablet = maxWidth >= 600.dp

        // Отступы у общего контейнера:
        val outerHorizontalPadding: Dp = if (isTablet) 16.dp else 8.dp
        val outerVerticalPadding: Dp = if (isTablet) 12.dp else 8.dp

        // Расстояние между кнопками:
        val betweenButtonsSpacing: Dp = if (isTablet) 8.dp else 4.dp

        // Высота каждой кнопки:
        val buttonHeight: Dp = if (isTablet) 56.dp else 40.dp

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = outerHorizontalPadding, vertical = outerVerticalPadding)
        ) {
            KeyboardRow(
                keys = keysRow1,
                coordinator = coordinator,
                spacing = betweenButtonsSpacing,
                buttonHeight = buttonHeight
            )
            Spacer(modifier = Modifier.height(betweenButtonsSpacing))
            KeyboardRow(
                keys = keysRow2,
                coordinator = coordinator,
                spacing = betweenButtonsSpacing,
                buttonHeight = buttonHeight
            )
            Spacer(modifier = Modifier.height(betweenButtonsSpacing))
            KeyboardRow(
                keys = keysRow3,
                coordinator = coordinator,
                spacing = betweenButtonsSpacing,
                buttonHeight = buttonHeight
            )
            Spacer(modifier = Modifier.height(betweenButtonsSpacing))
            KeyboardRow(
                keys = keysRow4,
                coordinator = coordinator,
                spacing = betweenButtonsSpacing,
                buttonHeight = buttonHeight
            )
            Spacer(modifier = Modifier.height(betweenButtonsSpacing))
            KeyboardRow(
                keys = keysRow5,
                coordinator = coordinator,
                spacing = betweenButtonsSpacing,
                buttonHeight = buttonHeight
            )
        }
    }
}

@Composable
private fun KeyboardRow(
    keys: List<String>,
    coordinator: MathKeyboardCoordinator,
    spacing: Dp,
    buttonHeight: Dp
) {
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing)
    ) {
        for (key in keys) {
            KeyboardButton(
                title = key,
                coordinator = coordinator,
                modifier = Modifier
                    .weight(1f)
                    .height(buttonHeight)
            )
        }
    }
}

@Composable
private fun KeyboardButton(
    title: String,
    coordinator: MathKeyboardCoordinator,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when (title) {
        "стереть", "CE" -> Color.Red
        "⏎"           -> Color.Green
        else          -> Color(0xFF1F2E59)
    }
    val soundPlayer = LocalSoundPlayer.current
    Button(
        onClick = {
            soundPlayer.playClick()
            when (title) {
                "стереть" -> coordinator.handleButtonPress("ster", "стереть")
                "CE"      -> coordinator.handleButtonPress("bt_check", "CE")
                "⏎"       -> coordinator.handleButtonPress("bt_down", "⏎")
                else      -> coordinator.handleButtonPress(title, title)
            }
        },
        modifier = modifier,
        shape = RoundedCornerShape(5.dp),
        colors = ButtonDefaults.buttonColors(containerColor = backgroundColor),
        contentPadding = PaddingValues(0.dp)
    ) {
        when (title) {
            "стереть" -> {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = "стереть",
                    tint = Color.White,
                    modifier = Modifier.size(if (modifier == Modifier) 16.dp else 20.dp)
                )
            }
            "CE" -> {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "очистить",
                    tint = Color.White,
                    modifier = Modifier.size(if (modifier == Modifier) 16.dp else 20.dp)
                )
            }
            else -> {
                Text(
                    text = title,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
