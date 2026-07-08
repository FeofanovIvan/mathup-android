package com.feofanova.mathup.ui.screens.preparation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.feofanova.mathup.data.local.entities.TaskEntity

@Composable
fun HintPopup(
    hintLatex: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(16.dp)
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
            modifier = Modifier
                .widthIn(max = 360.dp)
                .heightIn(max = 360.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFD32F2F))
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Закрыть",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(top = 4.dp)
                ) {
                    Text(
                        text = "Подсказка:",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    MathWebView(content = hintLatex)
                }
            }
        }
    }
}

@Composable
fun BottomButtons(
    onClose: () -> Unit,
    onHint: () -> Unit,
    onContinue: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircleButtonStyled(
            icon = Icons.Default.Close,
            iconTint = Color.Red,
            background = Color.White,
            shadowColor = Color.Red,
            onClick = onClose,
            size = 56.dp
        )

        Spacer(modifier = Modifier.width(24.dp))

        CircleButtonStyled(
            icon = Icons.Default.Lightbulb,
            iconTint = Color(0xFFFFC107),
            background = Color(0xFFFFF8E1),
            shadowColor = Color(0xFFFFC107),
            onClick = onHint,
            size = 72.dp
        )

        Spacer(modifier = Modifier.width(24.dp))

        CircleButtonStyled(
            icon = Icons.Default.PlayArrow,
            iconTint = Color(0xFF4CAF50),
            background = Color.White,
            shadowColor = Color(0xFF4CAF50),
            onClick = onContinue,
            size = 56.dp
        )
    }
}

@Composable
fun CircleButtonStyled(
    icon: ImageVector,
    iconTint: Color,
    background: Color,
    shadowColor: Color,
    onClick: () -> Unit,
    size: Dp = 56.dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .shadow(elevation = 6.dp, shape = CircleShape, ambientColor = shadowColor, spotColor = shadowColor)
            .background(background, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(size * 0.5f)
        )
    }
}

@Composable
fun SideMenu(
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
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50)
                            )
                        } else if (isSelected) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color.Blue
                            )
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
