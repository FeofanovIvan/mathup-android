package com.feofanova.mathup.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EditOff
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.feofanova.mathup.ui.screens.main.SetStatusBarColor
import kotlin.math.hypot

// ----------------------------------------------------------------------------
// DATA CLASS: points — SnapshotStateList<Offset>
// ----------------------------------------------------------------------------
data class DrawingLine(
    val points: SnapshotStateList<Offset>,
    val color: Color,
    val lineWidth: Float
)

// ----------------------------------------------------------------------------
// CanvasState: хранит все линии и текущую линию с изменяемым цветом/толщиной
// ----------------------------------------------------------------------------
class CanvasState {
    var lines = mutableStateListOf<DrawingLine>()
    var currentLine by mutableStateOf(
        DrawingLine(
            points = mutableStateListOf(),
            color = Color.Blue,
            lineWidth = 4f
        )
    )
    var selectedColor by mutableStateOf(Color.Blue)
    var lineWidth by mutableFloatStateOf(4f)
    var isEraser by mutableStateOf(false)
    var isMoveMode by mutableStateOf(false)
    var offset by mutableStateOf(Offset.Zero)

    fun clear() {
        lines.clear()
        currentLine = DrawingLine(
            points = mutableStateListOf(),
            color = selectedColor,
            lineWidth = lineWidth
        )
        offset = Offset.Zero
    }

    fun updateColor(newColor: Color) {
        selectedColor = newColor
        if (currentLine.points.isEmpty()) {
            currentLine = DrawingLine(
                points = mutableStateListOf(),
                color = newColor,
                lineWidth = lineWidth
            )
        } else {
            val oldPoints = currentLine.points
            currentLine = DrawingLine(
                points = mutableStateListOf<Offset>().apply { addAll(oldPoints) },
                color = newColor,
                lineWidth = lineWidth
            )
        }
    }

    fun updateLineWidth(newWidth: Float) {
        lineWidth = newWidth
        if (currentLine.points.isEmpty()) {
            currentLine = DrawingLine(
                points = mutableStateListOf(),
                color = selectedColor,
                lineWidth = newWidth
            )
        } else {
            val oldPoints = currentLine.points
            currentLine = DrawingLine(
                points = mutableStateListOf<Offset>().apply { addAll(oldPoints) },
                color = selectedColor,
                lineWidth = newWidth
            )
        }
    }

    fun eraseAt(location: Offset) {
        val radius = 40f
        val newLines = lines.flatMap { line ->
            val segments = mutableListOf<DrawingLine>()
            var currentSegment = mutableListOf<Offset>()
            for (p in line.points) {
                val dist = hypot(p.x - location.x, p.y - location.y)
                if (dist > radius) {
                    currentSegment.add(p)
                } else {
                    if (currentSegment.isNotEmpty()) {
                        segments += DrawingLine(
                            points = mutableStateListOf<Offset>().apply { addAll(currentSegment) },
                            color = line.color,
                            lineWidth = line.lineWidth
                        )
                        currentSegment = mutableListOf()
                    }
                }
            }
            if (currentSegment.isNotEmpty()) {
                segments += DrawingLine(
                    points = mutableStateListOf<Offset>().apply { addAll(currentSegment) },
                    color = line.color,
                    lineWidth = line.lineWidth
                )
            }
            segments
        }
        lines.clear()
        lines.addAll(newLines)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DraftCanvasScreen(
    onClose: () -> Unit,
    canvasState: CanvasState
) {
     SetStatusBarColor(color = Color(0xFFFFFFFF), darkIcons = true)
    Column(Modifier.fillMaxSize()) {
        CenterAlignedTopAppBar(
            title = {
                Text(
                    text = "Черновик",
                    textAlign = TextAlign.Center
                )
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = Color(0xFFFFFFFF) // ✅ Цвет фона панели
            ),
            actions = {
                Box(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFD32F2F))
                        .clickable { onClose() },
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
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            GridBackground(offset = canvasState.offset)

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { downOffset ->
                                if (!canvasState.isMoveMode && !canvasState.isEraser) {
                                    canvasState.currentLine.points.add(downOffset - canvasState.offset)
                                }
                            },
                            onDrag = { change, dragAmt ->
                                if (canvasState.isMoveMode) {
                                    canvasState.offset += dragAmt
                                } else if (canvasState.isEraser) {
                                    canvasState.eraseAt(change.position - canvasState.offset)
                                } else {
                                    canvasState.currentLine.points.add(change.position - canvasState.offset)
                                }
                            },
                            onDragEnd = {
                                if (!canvasState.isMoveMode && !canvasState.isEraser) {
                                    canvasState.lines.add(
                                        DrawingLine(
                                            points = mutableStateListOf<Offset>().apply {
                                                addAll(canvasState.currentLine.points)
                                            },
                                            color = canvasState.currentLine.color,
                                            lineWidth = canvasState.currentLine.lineWidth
                                        )
                                    )
                                    canvasState.currentLine = DrawingLine(
                                        points = mutableStateListOf(),
                                        color = canvasState.selectedColor,
                                        lineWidth = canvasState.lineWidth
                                    )
                                }
                            }
                        )
                    }
            ) {
                withTransform({
                    translate(canvasState.offset.x, canvasState.offset.y)
                }) {
                    canvasState.lines.forEach { line ->
                        val path = Path().apply {
                            line.points.firstOrNull()?.let { moveTo(it.x, it.y) }
                            line.points.drop(1).forEach { lineTo(it.x, it.y) }
                        }
                        drawPath(path, color = line.color, style = Stroke(line.lineWidth))
                    }

                    val currPath = Path().apply {
                        canvasState.currentLine.points.firstOrNull()?.let { moveTo(it.x, it.y) }
                        canvasState.currentLine.points.drop(1).forEach { lineTo(it.x, it.y) }
                    }
                    drawPath(currPath, color = canvasState.currentLine.color, style = Stroke(canvasState.currentLine.lineWidth))
                }
            }
        }

        ToolBar(canvasState)
    }
}

// ----------------------------------------------------------------------------
// ToolBar: включает DropdownMenu для выбора цвета и адаптивную верстку кнопок
// ----------------------------------------------------------------------------
@Composable
fun ToolBar(canvasState: CanvasState) {
    val palette = listOf(Color.Blue, Color.Red, Color.Green, Color.Magenta, Color.Yellow)
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .background(Color.White, shape = RoundedCornerShape(16.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── Круглая кнопка выбора цвета ──
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)                             // ограничиваем кругом
                .background(canvasState.selectedColor)         // текущий цвет круга
                .border(2.dp, Color.Gray, CircleShape)         // обводка круга
                .clickable { expanded = true },                // клик по кругу открывает меню
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "Выбрать цвет",
                tint = Color.White
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            palette.forEach { colorOption ->
                DropdownMenuItem(
                    onClick = {
                        canvasState.updateColor(colorOption)
                        expanded = false
                    },
                    text = {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(colorOption)
                                .border(1.dp, Color.Gray, CircleShape)
                        )
                    }
                )
            }
        }

        // ── Слайдер для толщины линии ──
        Slider(
            value = canvasState.lineWidth,
            onValueChange = { newWidth ->
                canvasState.updateLineWidth(newWidth)
            },
            valueRange = 1f..10f,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
        )

        // ── Инструментальные кнопки ──
        ToolButton(
            icon = Icons.Default.Create,
            defaultBg = Color.LightGray,
            activeBg = Color.Green,
            selected = !canvasState.isEraser && !canvasState.isMoveMode
        ) {
            canvasState.isEraser = false
            canvasState.isMoveMode = false
        }

        ToolButton(
            icon = Icons.Default.EditOff,
            defaultBg = Color.LightGray,
            activeBg = Color.Green,
            selected = canvasState.isEraser
        ) {
            canvasState.isEraser = true
            canvasState.isMoveMode = false
        }

        ToolButton(
            icon = Icons.Default.Delete,
            defaultBg = Color.Red,
            activeBg = Color.Red,
            selected = false
        ) {
            canvasState.clear()
        }

        ToolButton(
            icon = Icons.Default.OpenWith,
            defaultBg = Color.LightGray,
            activeBg = Color.Green,
            selected = canvasState.isMoveMode
        ) {
            canvasState.isMoveMode = true
            canvasState.isEraser = false
        }
    }
}

@Composable
fun ToolButton(
    icon: ImageVector,
    defaultBg: Color,
    activeBg: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (selected) activeBg else defaultBg
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(bgColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
    }
}


// ----------------------------------------------------------------------------
// GridBackground без изменений
// ----------------------------------------------------------------------------
@Composable
fun GridBackground(offset: Offset) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val spacing = 40f
        val startX = -(offset.x % spacing)
        val startY = -(offset.y % spacing)
        val width = size.width
        val height = size.height

        var x = startX
        while (x <= width) {
            drawLine(
                color = Color.LightGray,
                start = Offset(x, 0f),
                end = Offset(x, height),
                strokeWidth = 0.5f
            )
            x += spacing
        }

        var y = startY
        while (y <= height) {
            drawLine(
                color = Color.LightGray,
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 0.5f
            )
            y += spacing
        }
    }
}