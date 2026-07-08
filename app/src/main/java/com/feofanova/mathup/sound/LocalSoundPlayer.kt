package com.feofanova.mathup.sound

import androidx.compose.runtime.staticCompositionLocalOf

val LocalSoundPlayer = staticCompositionLocalOf<SoundPlayer> {
    error("SoundPlayer not provided")
}