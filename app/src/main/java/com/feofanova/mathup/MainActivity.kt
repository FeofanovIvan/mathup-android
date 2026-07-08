package com.feofanova.mathup

import android.os.Build
import android.os.Bundle
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
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.feofanova.mathup.data.local.db.MathUpDatabase
import com.feofanova.mathup.sync.InitialContentSyncUseCase
import com.feofanova.mathup.sound.LocalSoundPlayer
import com.feofanova.mathup.sound.SoundPlayer
import com.feofanova.mathup.sound.SoundSettingsDataStore
import com.feofanova.mathup.ui.navigation.AppNavGraph
import com.feofanova.mathup.ui.screens.main.ProfileViewModel
import com.feofanova.mathup.ui.theme.MathUpTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val syncDone = mutableStateOf(false)
    private lateinit var soundPlayer: SoundPlayer
    private val initialContentSync = InitialContentSyncUseCase()

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Устанавливаем цвет статус-бара
        window.statusBarColor = Color(0xFF1F2E59).toArgb()
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false

        soundPlayer = SoundPlayer(this) // Глобально

        lifecycleScope.launch {
            initialContentSync(applicationContext)
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
