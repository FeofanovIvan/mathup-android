package com.feofanova.mathup.ui.screens.main

import android.app.Activity
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Feed
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavHostController
import com.feofanova.mathup.ui.screens.home.HomeScreen
import com.feofanova.mathup.ui.screens.news.NewsScreen
import com.feofanova.mathup.ui.screens.settings.SettingsScreen
import androidx.core.content.edit
import androidx.core.view.WindowCompat
import com.feofanova.mathup.SyncMetadata
import com.feofanova.mathup.data.characters.GameDataSyncManager
import com.feofanova.mathup.data.characters.GameDatabase
import com.feofanova.mathup.data.local.db.MathUpDatabase
import com.feofanova.mathup.data.local.db.MathUpOgeDatabase
import com.feofanova.mathup.data.repository.DataSyncManager
import com.feofanova.mathup.sound.LocalSoundPlayer
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import kotlinx.coroutines.tasks.await

@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(navController: NavHostController,
               isSoundEnabled: Boolean,
               onSoundToggle: (Boolean) -> Unit) {
    SetStatusBarColor(color = Color(0xFF1F2E59), darkIcons = false)
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }
    // Проверяем, есть ли уже сохранённый профиль
    val storedProfile = sharedPrefs.getString("selectedProfile", null)
    var showProfileDialog by remember { mutableStateOf(storedProfile == null) }
    var selectedProfile by rememberSaveable { mutableStateOf(storedProfile ?: "База") }
    var currentTitle by remember {
        mutableStateOf(
            when (storedProfile) {
                "База", "Профиль" -> "ЕГЭ математика"
                "ОГЭ" -> "ОГЭ математика"
                else -> "Математика"
            }
        )
    }

    var selectedTab by remember { mutableStateOf(MainTab.Home) }
    var isMenuOpen by rememberSaveable { mutableStateOf(false) }
    val showUpdateDialog = remember { mutableStateOf(false) }
    val dbToUpdate = remember { mutableStateOf(DbType.NONE) }
    val isUpdatingContent = remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val dbUpdateQueue = remember { mutableStateListOf<DbType>() }
    val soundPlayer = LocalSoundPlayer.current


    UpdateDialog(
        showDialog = showUpdateDialog,
        dbToUpdate = dbToUpdate,
        isUpdating = isUpdatingContent,
        snackbarHostState = snackbarHostState,
        context = context,
        dbUpdateQueue = dbUpdateQueue // обязательно!
    )


    LaunchedEffect(selectedProfile, showProfileDialog) {
        if (!showProfileDialog) {
            val firestore = FirebaseFirestore.getInstance()
            val prefs = context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
            val localMetadataJson = prefs.getString("sync_metadata", null)
            val localMetadata = localMetadataJson?.let {
                try {
                    Gson().fromJson(it, SyncMetadata::class.java)
                } catch (_: Exception) { null }
            } ?: SyncMetadata()

            try {
                val remoteEge = firestore.collection("sync_metadata")
                    .document("MathUpDatabase").get().await()
                    .getLong("version")?.toInt() ?: 1
                val remoteOge = firestore.collection("sync_metadata")
                    .document("MathUpOgeDatabase").get().await()
                    .getLong("version")?.toInt() ?: 1
                val remoteGame = firestore.collection("sync_metadata")
                    .document("GameDatabase").get().await()
                    .getLong("version")?.toInt() ?: 1

                val outdatedDbs = mutableListOf<DbType>()
                if (localMetadata.version_ege < remoteEge) outdatedDbs.add(DbType.EGE)
                if (localMetadata.version_oge < remoteOge) outdatedDbs.add(DbType.OGE)
                if (localMetadata.version_game < remoteGame) outdatedDbs.add(DbType.GAME)

                if (outdatedDbs.isNotEmpty()) {
                    dbUpdateQueue.clear()
                    dbUpdateQueue.addAll(outdatedDbs)
                    dbToUpdate.value = dbUpdateQueue.removeFirst()
                    showUpdateDialog.value = true
                }

            } catch (e: Exception) {
                Log.e("MainScreen", "❌ Ошибка получения версий из Firestore", e)
            }
        }
    }



    Box(Modifier.fillMaxSize()) {
        // 👇 Snackbar — поверх всего, всегда видим
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            snackbar = { data ->
                Snackbar(
                    modifier = Modifier
                        .padding(8.dp),
                    containerColor = Color(0xFF4CAF50),
                    contentColor = Color.White,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = data.visuals.message,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        )
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TextButton(
                                onClick = {
                                    soundPlayer.playClick()     // 🔊 Проигрываем звук
                                    isMenuOpen = true           // 📦 Открываем меню
                                }
                            ) {
                                Text(
                                    text = selectedProfile,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    modifier = Modifier
                                        .background(Color.Green, shape = RoundedCornerShape(8.dp))
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                            Text(currentTitle, color = Color.White, fontSize = 18.sp)
                            Box(modifier = Modifier.size(60.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Премиум",
                                    tint = Color.Yellow,
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .size(36.dp)
                                )
                                PromoLabel(modifier = Modifier.align(Alignment.TopStart))
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1F2E59))
                )
            },
            bottomBar = {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color(0xFFE5EAF3))
                    )

                    NavigationBar(containerColor = Color.White) {
                        NavigationBarItem(
                            selected = selectedTab == MainTab.Home,
                            onClick = {
                                selectedTab = MainTab.Home
                                currentTitle = getTitleForProfile(selectedProfile)
                                soundPlayer.playClick()
                            },
                            icon = { Icon(Icons.Default.Home, contentDescription = "Дом") },
                            label = { Text("Дом") },
                            alwaysShowLabel = true,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFF4CAF50),
                                unselectedIconColor = Color(0xFF1F2E59),
                                selectedTextColor = Color(0xFF4CAF50),
                                unselectedTextColor = Color(0xFF1F2E59),
                                indicatorColor = Color.Transparent
                            )
                        )
                        NavigationBarItem(
                            selected = selectedTab == MainTab.News,
                            onClick = {
                                selectedTab = MainTab.News
                                currentTitle = "Новости"
                                soundPlayer.playClick()
                            },
                            icon = { Icon(Icons.AutoMirrored.Filled.Feed, contentDescription = "Новости") },
                            label = { Text("Новости") },
                            alwaysShowLabel = true,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFF4CAF50),
                                unselectedIconColor = Color(0xFF1F2E59),
                                selectedTextColor = Color(0xFF4CAF50),
                                unselectedTextColor = Color(0xFF1F2E59),
                                indicatorColor = Color.Transparent
                            )
                        )
                        NavigationBarItem(
                            selected = selectedTab == MainTab.Settings,
                            onClick = {
                                selectedTab = MainTab.Settings
                                currentTitle = "Настройки"
                                soundPlayer.playClick()
                            },
                            icon = { Icon(Icons.Default.Settings, contentDescription = "Настройки") },
                            label = { Text("Настройки") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFF4CAF50),
                                unselectedIconColor = Color(0xFF1F2E59),
                                selectedTextColor = Color(0xFF4CAF50),
                                unselectedTextColor = Color(0xFF1F2E59),
                                indicatorColor = Color.Transparent
                            )
                        )
                    }
                }
            },
            content = { innerPadding ->
                Box(modifier = Modifier.padding(innerPadding)) {
                    when (selectedTab) {
                        MainTab.Home -> HomeScreen(navController = navController, selectedProfile = selectedProfile)
                        MainTab.News -> NewsScreen(navController = navController)
                        MainTab.Settings -> SettingsScreen(
                            navController = navController,
                            isSoundEnabled = isSoundEnabled,
                            onSoundToggle = onSoundToggle
                        )
                    }
                }
            }
        )

        // ─── Боковое меню профилей ───
        if (isMenuOpen) {
            Row(Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .width(250.dp)
                        .fillMaxHeight()
                        .background(Color.White)
                        .padding(12.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Выберите профиль",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1F2E59),
                        modifier = Modifier.padding(8.dp)
                    )

                    HorizontalDivider(Modifier.fillMaxWidth(), color = Color.LightGray)

                    Spacer(Modifier.height(8.dp))

                    listOf("База", "Профиль", "ОГЭ").forEach { profile ->
                    val isSelected = selectedProfile == profile
                        Button(
                            onClick = {
                                soundPlayer.playClick()
                                selectedProfile = profile
                                currentTitle = when (profile) {
                                    "База" -> "ЕГЭ математика"
                                    "Профиль" -> "ЕГЭ математика"
                                    "ОГЭ" -> "ОГЭ математика"
                                    else -> "Математика"
                                }
                                saveProfile(context, selectedProfile)
                                isMenuOpen = false
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) Color(0xFFE8F5E9) else Color.Transparent,
                                contentColor = if (isSelected) Color(0xFF4CAF50) else Color(0xFF1F2E59)
                            )
                        ) {
                            Text(profile)
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f))
                        .clickable { isMenuOpen = false }
                )
            }
        }
    }
    if (showProfileDialog) {
        AlertDialog(
            onDismissRequest = { /* нельзя закрыть */ },
            title = { Text("Выберите профиль") },
            text = {
                val displayToInternalMap = mapOf(
                    "ЕГЭ база" to "База",
                    "ЕГЭ профиль" to "Профиль",
                    "ОГЭ" to "ОГЭ"
                )

                Column {
                    displayToInternalMap.forEach { (displayName, internalValue) ->
                        Button(
                            onClick = {
                                selectedProfile = internalValue
                                sharedPrefs.edit {
                                    putString("selectedProfile", internalValue)
                                }
                                showProfileDialog = false
                                soundPlayer.playClick()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1F2E59),
                                contentColor = Color.White
                            )
                        ) {
                            Text(displayName)
                        }
                    }
                }
            },
            confirmButton = {}, // нет кнопки OK
            dismissButton = {}
        )
    }
}

@Composable
fun PromoLabel(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition()
    val rotation by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Text(
        text = "Акция",
        color = Color.White,
        fontSize = 10.sp,
        modifier = modifier
            .rotate(rotation)
            .background(Color.Red, RoundedCornerShape(8.dp))
            .padding(horizontal = 4.dp)
    )
}
fun getTitleForProfile(profile: String): String = when (profile) {
    "База", "Профиль" -> "ЕГЭ математика"
    "ОГЭ" -> "ОГЭ математика"
    else -> "Математика"
}

private fun saveProfile(context: Context, value: String) {
    context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        .edit {
            putString("selectedProfile", value)
        }
}
@Composable
fun SetStatusBarColor(color: Color, darkIcons: Boolean = true) {
    val view = LocalView.current
    val window = (view.context as Activity).window

    SideEffect {
        // Официально поддерживаемый способ даже с deprecated
        @Suppress("DEPRECATION")
        window.statusBarColor = color.toArgb()

        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkIcons
    }
}

enum class MainTab {
    Home, News, Settings
}
enum class DbType {
    NONE, EGE, OGE, GAME
}

@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
@Composable
fun UpdateDialog(
    showDialog: MutableState<Boolean>,
    dbToUpdate: MutableState<DbType>, // можно оставить, но будет не нужен
    isUpdating: MutableState<Boolean>,
    snackbarHostState: SnackbarHostState,
    context: Context,
    dbUpdateQueue: SnapshotStateList<DbType>
) {
    if (showDialog.value && dbUpdateQueue.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Обновление баз данных") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (isUpdating.value) "Идёт обновление всех баз данных..."
                        else "Требуется обновление контента. Выполнить сейчас?",
                        fontSize = 16.sp
                    )
                    if (isUpdating.value) {
                        Spacer(modifier = Modifier.height(16.dp))
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            color = Color(0xFF4CAF50)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { isUpdating.value = true },
                    enabled = !isUpdating.value,
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF1F2E59))
                ) {
                    Text("Да")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDialog.value = false
                        dbUpdateQueue.clear()
                    },
                    enabled = !isUpdating.value,
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF1F2E59))
                ) {
                    Text("Нет")
                }
            }
        )
    }

    if (isUpdating.value) {
        LaunchedEffect(Unit) {
            try {
                for (dbType in dbUpdateQueue.toList()) {
                    when (dbType) {
                        DbType.EGE -> {
                            val db = MathUpDatabase.getInstance(context)
                            DataSyncManager.syncFromRemote(context, db)
                        }
                        DbType.OGE -> {
                            val db = MathUpOgeDatabase.getInstance(context)
                            DataSyncManager.syncOgeFromRemote(context, db)
                        }
                        DbType.GAME -> {
                            val db = GameDatabase.getInstance(context)
                            GameDataSyncManager.syncGameData(context, db)
                        }
                        else -> {}
                    }
                }
                snackbarHostState.showSnackbar("Все базы обновлены ✅")
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("❌ Ошибка при обновлении: ${e.localizedMessage}")
            } finally {
                isUpdating.value = false
                showDialog.value = false
                dbToUpdate.value = DbType.NONE
                dbUpdateQueue.clear()
            }
        }
    }
}
