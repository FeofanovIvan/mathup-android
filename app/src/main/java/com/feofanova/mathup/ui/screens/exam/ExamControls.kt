package com.feofanova.mathup.ui.screens.exam

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.feofanova.mathup.data.local.entities.TaskEntity
import kotlinx.coroutines.delay

enum class TaskExamTab(val title: String) {
    Task("Задание"),
    Answer("Ответ")
}

@Composable
fun TabExamSelector(
    selectedTab: TaskExamTab,
    onTabSelected: (TaskExamTab) -> Unit,
    modifier: Modifier = Modifier,
    onMenuClick: () -> Unit,
    initialTimeSeconds: Int = 3 * 3600 + 55 * 60
) {
    val timerColor = Color(0xFFFFC107)
    var remainingSeconds by rememberSaveable { mutableIntStateOf(initialTimeSeconds) }
    val blinkAlpha by rememberInfiniteTransition(label = "blink").animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(700),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blink"
    )

    LaunchedEffect(Unit) {
        while (remainingSeconds > 0) {
            delay(1000)
            remainingSeconds--
        }
    }

    val hours = remainingSeconds / 3600
    val minutes = (remainingSeconds % 3600) / 60
    val hStr = hours.toString().padStart(2, '0')
    val mStr = minutes.toString().padStart(2, '0')

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onMenuClick) {
            Icon(Icons.Default.Menu, contentDescription = "Меню", tint = Color(0xFF1F2E59))
        }

        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.weight(1f)
        ) {
            TaskExamTab.entries.forEach { tab ->
                val interactionSource = remember { MutableInteractionSource() }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null
                        ) { onTabSelected(tab) }
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = tab.title.uppercase(),
                        color = if (selectedTab == tab) Color(0xFF4CAF50) else Color(0xFF1F2E59),
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .height(2.dp)
                            .width(48.dp)
                            .background(if (selectedTab == tab) Color(0xFF4CAF50) else Color.Transparent)
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFF5F5F5))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = hStr, color = timerColor, fontWeight = FontWeight.SemiBold)
                Text(
                    text = ":",
                    color = timerColor.copy(alpha = blinkAlpha),
                    fontWeight = FontWeight.SemiBold
                )
                Text(text = mStr, color = timerColor, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun SideMenuExam(
    taskName: String,
    taskEntities: List<TaskEntity>,
    completedTaskIDs: Set<Long>,
    selectedTaskID: Long?,
    onTaskSelected: (Long) -> Unit,
    onDismiss: () -> Unit,
    profile: String
) {
    Row(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .width(250.dp)
                .fillMaxHeight()
                .background(Color.White)
                .padding(8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = taskName,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1F2E59),
                modifier = Modifier.padding(8.dp)
            )

            HorizontalDivider(
                modifier = Modifier.fillMaxWidth().height(1.dp),
                color = Color.LightGray
            )
            Spacer(Modifier.height(4.dp))

            taskEntities.forEachIndexed { index, task ->
                val isSelected = selectedTaskID?.toInt() == task.taskID
                val isCompleted = completedTaskIDs.contains(task.taskID.toLong())

                Button(
                    onClick = { onTaskSelected(task.taskID.toLong()) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when {
                            isSelected -> Color(0xFFE3F2FD)
                            isCompleted -> Color(0xFFE8F5E9)
                            else -> Color.Transparent
                        },
                        contentColor = if (isCompleted) Color(0xFF4CAF50) else Color(0xFF1F2E59)
                    )
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Вопрос ${index + 1}", modifier = Modifier.weight(1f))

                        if (isCompleted) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50))
                        } else if (isSelected) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.Blue)
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f))
                .clickable { onDismiss() }
        )
    }
}
