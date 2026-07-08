package com.feofanova.mathup.sound

import android.content.Context
import android.media.SoundPool
import com.feofanova.mathup.R

class SoundPlayer(context: Context) {
    private val soundPool = SoundPool.Builder().setMaxStreams(3).build()

    private val clickSoundId: Int = soundPool.load(context, R.raw.click2, 1)
    private val correctSoundId: Int = soundPool.load(context, R.raw.correct, 1)
    private val wrongSoundId: Int = soundPool.load(context, R.raw.wrong, 1)
    private val finalSoundId: Int = soundPool.load(context, R.raw.finish, 1)

    var masterVolume: Float = 0.4f
    var isSoundEnabled: Boolean = true  // 👈 добавили переменную

    fun playClick() {
        if (isSoundEnabled) {
            soundPool.play(clickSoundId, masterVolume, masterVolume, 1, 0, 1f)
        }
    }

    fun playCorrect() {
        if (isSoundEnabled) {
            soundPool.play(correctSoundId, masterVolume, masterVolume, 1, 0, 1f)
        }
    }

    fun playWrong() {
        if (isSoundEnabled) {
            soundPool.play(wrongSoundId, masterVolume, masterVolume, 1, 0, 1f)
        }
    }

    fun playFinal() {
        if (isSoundEnabled) {
            soundPool.play(finalSoundId, masterVolume, masterVolume, 1, 0, 1f)
        }
    }

    fun release() {
        soundPool.release()
    }
}
