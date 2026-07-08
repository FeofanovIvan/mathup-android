package com.feofanova.mathup.ui.screens.game

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import com.feofanova.mathup.data.characters.GameDatabase
import com.feofanova.mathup.data.characters.entities.MathCharacterEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import com.feofanova.mathup.ui.screens.exam.PlayFinalSound
import com.feofanova.mathup.ui.screens.main.SetStatusBarColor
import java.io.File

// Tile data class
data class Tile(val id: Int, val image: Bitmap?, var row: Int, var col: Int)

@Composable
fun GamesScreen(profile: String, onBack: () -> Unit) {
    SetStatusBarColor(color = Color(0xFF1F2E59), darkIcons = false)
    val context = LocalContext.current
    val db = remember { GameDatabase.getInstance(context) }
    val scope = rememberCoroutineScope()

    var characters by remember { mutableStateOf<List<MathCharacterEntity>>(emptyList()) }
    var currentIndex by remember { mutableIntStateOf(0) }
    var tiles by remember { mutableStateOf<List<Tile>>(emptyList()) }
    var isCompleted by remember { mutableStateOf(false) }
    var showInfoCard by remember { mutableStateOf(false) }
    var showVictoryDialog by remember { mutableStateOf(false) }


    val currentCharacter = characters.getOrNull(currentIndex)

    fun resetPuzzle() {
        isCompleted = false
        showInfoCard = false
        val image = currentCharacter?.imageName?.let { loadImageFromStorage(context, it) }
        tiles = if (image != null) shuffleTiles(sliceImage(image)) else emptyList()
    }

    fun checkCompletion() {
        if (tiles.indices.all { tiles[it].id == it }) {
            isCompleted = true
            scope.launch {
                kotlinx.coroutines.delay(500)
                showVictoryDialog = true
                showInfoCard = true
            }
        }
    }



    fun moveTile(tile: Tile) {
        val emptyIndex = tiles.indexOfFirst { it.image == null }
        val tappedIndex = tiles.indexOfFirst { it.id == tile.id }
        if (emptyIndex == -1 || tappedIndex == -1) return

        val emptyTile = tiles[emptyIndex]
        val rowDiff = kotlin.math.abs(tile.row - emptyTile.row)
        val colDiff = kotlin.math.abs(tile.col - emptyTile.col)

        if ((rowDiff == 1 && colDiff == 0) || (rowDiff == 0 && colDiff == 1)) {
            val newTiles = tiles.toMutableList()
            newTiles[emptyIndex] = tile.copy(row = emptyTile.row, col = emptyTile.col)
            newTiles[tappedIndex] = emptyTile.copy(row = tile.row, col = tile.col)
            tiles = newTiles
            checkCompletion()
        }
    }


    LaunchedEffect(Unit) {
        characters = withContext(Dispatchers.IO) { db.characterDao().getAll() }
    }

    LaunchedEffect(currentIndex, characters) {
        resetPuzzle()
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
        // 🔹 Custom Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1F2E59))
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Назад",
                    tint = Color.White
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Собери портрет",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.size(48.dp)) // Чтобы сбалансировать иконку слева
        }

        // 🔹 Main Content with padding and scroll
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {

                Text(
                    text = "🔍 Здесь зашифрован портрет великого математика",
                    textAlign = TextAlign.Center,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1F2E59)
                )

            Spacer(Modifier.height(16.dp))


            if (isCompleted && currentCharacter != null) {
                val image = loadImageFromStorage(context, currentCharacter.imageName)
                image?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .shadow(4.dp)
                    )
                }
                if (showInfoCard) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .background(Color.White, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text(currentCharacter.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(currentCharacter.achievement)
                        Spacer(Modifier.height(4.dp))
                        Text("\uD83D\uDCDD ${currentCharacter.info}", fontSize = 13.sp)
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    PuzzleGrid(tiles = tiles, onTileClick = { moveTile(it) })
                }
            }
            Spacer(Modifier.height(16.dp))

            if (currentCharacter != null && currentCharacter.info.isNotBlank()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                    ) {
                        Text("📌", fontSize = 20.sp)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = currentCharacter.info,
                            fontSize = 14.sp,
                            color = Color(0xFF1F2E59),
                            lineHeight = 18.sp
                        )
                    }
                }
            }



            Spacer(Modifier.height(16.dp))


            val darkBlue = Color(0xFF1F2E59)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = {
                        if (currentIndex > 0) {
                            currentIndex--
                            resetPuzzle()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = darkBlue)
                ) {
                    Text("Назад", color = Color.White)
                }

                Button(
                    onClick = {
                        if (currentIndex < characters.lastIndex) {
                            currentIndex++
                            resetPuzzle()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = darkBlue)
                ) {
                    Text("Вперёд", color = Color.White)
                }
            }
            if (showVictoryDialog && currentCharacter != null) {
                PlayFinalSound()
                AlertDialog(
                    onDismissRequest = { showVictoryDialog = false },
                    confirmButton = {
                        TextButton(onClick = { showVictoryDialog = false }) {
                            Text("Ок", color = Color(0xFF1F2E59))
                        }
                    },
                    title = {
                        Text(
                            text = "🎉 Победа!",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F2E59)
                        )
                    },
                    text = {
                        Column {
                            Text(
                                text = currentCharacter.name,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "🏆 ${currentCharacter.achievement}",
                                fontSize = 15.sp,
                                lineHeight = 20.sp
                            )
                        }
                    },
                    containerColor = Color.White,
                    shape = RoundedCornerShape(16.dp)
                )
            }

        }
    }
}

@Composable
fun PuzzleGrid(tiles: List<Tile>, onTileClick: (Tile) -> Unit) {
    val gridSize = 4
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        for (row in 0 until gridSize) {
            Row {
                for (col in 0 until gridSize) {
                    val tile = tiles.find { it.row == row && it.col == col }
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .padding(2.dp)
                            .background(Color.White, RoundedCornerShape(8.dp))
                            .clickable(enabled = tile?.image != null) { tile?.let(onTileClick) },
                        contentAlignment = Alignment.Center
                    ) {
                        tile?.image?.let { bitmap ->
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.clip(RoundedCornerShape(8.dp))
                            )
                        }
                    }
                }
            }
        }
    }
}

fun sliceImage(image: Bitmap, gridSize: Int = 4): List<Tile> {
    val tileWidth = image.width / gridSize
    val tileHeight = image.height / gridSize
    val tiles = mutableListOf<Tile>()
    for (row in 0 until gridSize) {
        for (col in 0 until gridSize) {
            val id = row * gridSize + col
            val tileBitmap = Bitmap.createBitmap(image, col * tileWidth, row * tileHeight, tileWidth, tileHeight)
            tiles.add(Tile(id, tileBitmap, row, col))
        }
    }
    tiles[tiles.lastIndex] = Tile(tiles.lastIndex, null, gridSize - 1, gridSize - 1)
    return tiles
}

fun loadImageFromStorage(context: android.content.Context, fileName: String): Bitmap? {
    val file = File(context.filesDir, "character_images/$fileName")
    return if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
}
fun shuffleTiles(tiles: List<Tile>, gridSize: Int = 4): List<Tile> {
    val shuffled = tiles.toMutableList()
    do {
        shuffled.shuffle()
    } while (!isSolvable(shuffled, gridSize) || shuffled.indices.all { shuffled[it].id == it }) // не разрешим или уже собран
    // Обновляем позиции после перемешивания
    shuffled.forEachIndexed { index, tile ->
        tile.row = index / gridSize
        tile.col = index % gridSize
    }
    return shuffled
}
fun isSolvable(tiles: List<Tile>, gridSize: Int): Boolean {
    val tileIds = tiles.map { it.id }
    val inversionCount = tileIds.withIndex().sumOf { (i, a) ->
        tileIds.drop(i + 1).count { b -> a != tileIds.size - 1 && b != tileIds.size - 1 && a > b }
    }
    val emptyTileRow = tiles.first { it.image == null }.row
    return if (gridSize % 2 == 1) {
        inversionCount % 2 == 0
    } else {
        (gridSize - emptyTileRow) % 2 != inversionCount % 2
    }
}
