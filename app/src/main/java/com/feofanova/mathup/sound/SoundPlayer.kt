package com.feofanova.mathup.sound

import android.content.Context
import android.media.SoundPool

class SoundPlayer(context: Context) {
    private val soundPool = SoundPool.Builder().setMaxStreams(3).build()

    private val clickSoundId: Int = loadSound(context, "click2")
    private val correctSoundId: Int = loadSound(context, "correct")
    private val wrongSoundId: Int = loadSound(context, "wrong")
    private val finalSoundId: Int = loadSound(context, "finish")

    var masterVolume: Float = 0.4f
    var isSoundEnabled: Boolean = true

    fun playClick() {
        if (isSoundEnabled && clickSoundId != 0) {
            soundPool.play(clickSoundId, masterVolume, masterVolume, 1, 0, 1f)
        }
    }

    fun playCorrect() {
        if (isSoundEnabled && correctSoundId != 0) {
            soundPool.play(correctSoundId, masterVolume, masterVolume, 1, 0, 1f)
        }
    }

    fun playWrong() {
        if (isSoundEnabled && wrongSoundId != 0) {
            soundPool.play(wrongSoundId, masterVolume, masterVolume, 1, 0, 1f)
        }
    }

    fun playFinal() {
        if (isSoundEnabled && finalSoundId != 0) {
            soundPool.play(finalSoundId, masterVolume, masterVolume, 1, 0, 1f)
        }
    }

    fun release() {
        soundPool.release()
    }

    private fun loadSound(context: Context, name: String): Int {
        val resourceId = context.resources.getIdentifier(name, "raw", context.packageName)
        return if (resourceId != 0) soundPool.load(context, resourceId, 1) else 0
    }
}
