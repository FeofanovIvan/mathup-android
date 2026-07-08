package com.feofanova.mathup.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.feofanova.mathup.sound.LocalSoundPlayer

@Composable
fun PlayFinalSound() {
    val player = LocalSoundPlayer.current

    LaunchedEffect(Unit) {
        player.playFinal()
    }
}
