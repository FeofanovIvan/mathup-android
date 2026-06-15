package com.feofanova.mathup

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.feofanova.mathup.data.characters.GameDataSyncManager
import com.feofanova.mathup.data.characters.GameDatabase
import com.feofanova.mathup.data.local.db.MathUpDatabase
import com.feofanova.mathup.data.local.db.MathUpOgeDatabase
import com.feofanova.mathup.data.repository.DataSyncManager
import com.feofanova.mathup.sound.LocalSoundPlayer
import com.feofanova.mathup.sound.SoundPlayer
import com.feofanova.mathup.sound.SoundSettingsDataStore
import com.feofanova.mathup.ui.navigation.AppNavGraph
import com.feofanova.mathup.ui.screens.main.ProfileViewModel
import com.feofanova.mathup.ui.theme.MathUpTheme
import com.google.gson.Gson
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val syncDone = mutableStateOf(false)
    private lateinit var soundPlayer: SoundPlayer

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Устанавливаем цвет статус-бара
        window.statusBarColor = Color(0xFF1F2E59).toArgb()
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false

        soundPlayer = SoundPlayer(this) // Глобально

        lifecycleScope.launch {
            syncFromAssetsIfFirstLaunch(applicationContext)
            syncDone.value = true
        }

        setContent {
            val soundEnabledState = remember { mutableStateOf(true) }

            // Загружаем сохранённую настройку звука
            LaunchedEffect(Unit) {
                val saved = SoundSettingsDataStore.loadSoundEnabled(applicationContext)
                soundPlayer.isSoundEnabled = saved
                soundEnabledState.value = saved
            }

            CompositionLocalProvider(LocalSoundPlayer provides soundPlayer) {
                MathUpTheme {
                    if (syncDone.value) {
                        val navController = rememberNavController()
                        val profileViewModel: ProfileViewModel by viewModels()

                        AppNavGraph(
                            navController = navController,
                            dao = MathUpDatabase.getInstance(applicationContext).appDao(),
                            profileViewModel = profileViewModel,
                            isSoundEnabled = soundEnabledState.value,
                            onSoundToggle = { newValue ->
                                soundEnabledState.value = newValue
                                soundPlayer.isSoundEnabled = newValue
                                lifecycleScope.launch {
                                    SoundSettingsDataStore.saveSoundEnabled(applicationContext, newValue)
                                }
                            }
                        )
                    } else {
                        // Экран загрузки
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = Color(0xFF1F2E59)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.CloudDownload,
                                        contentDescription = "Загрузка",
                                        tint = Color.White,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    CircularProgressIndicator(color = Color(0xFF4CAF50))
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        "Настраиваем базы данных...",
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        soundPlayer.release()
    }
}

// 🔁 Синхронизация баз данных при первом запуске
private suspend fun syncFromAssetsIfFirstLaunch(context: Context) {
    val prefs = context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
    val metadataJson = prefs.getString("sync_metadata", null)

    if (metadataJson == null) {
        Log.d("SyncAssets", "🆕 Первый запуск. Начинаем загрузку из assets...")

        val gameDb = GameDatabase.getInstance(context)
        val egeDb = MathUpDatabase.getInstance(context)
        val ogeDb = MathUpOgeDatabase.getInstance(context)

        try {
            DataSyncManager.syncFromAssets(context, egeDb)
            DataSyncManager.syncOgeFromAssets(context, ogeDb)
            GameDataSyncManager.syncGameDataFromAssets(context, gameDb)

            val metadata = SyncMetadata(
                version_ege = 1,
                version_oge = 1,
                version_game = 1
            )
            prefs.edit { putString("sync_metadata", Gson().toJson(metadata)) }
            Log.d("SyncAssets", "✅ Загрузка завершена и metadata сохранена")

        } catch (e: Exception) {
            Log.e("SyncAssets", "❌ Ошибка при синхронизации из assets", e)
        }
    } else {
        Log.d("SyncAssets", "ℹ️ Данные уже есть — пропускаем загрузку из assets")
    }
}

data class SyncMetadata(
    val version_game: Int = 1,
    val version_oge: Int = 1,
    val version_ege: Int = 1,
)
