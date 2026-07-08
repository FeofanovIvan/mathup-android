package com.feofanova.mathup.sound


import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "sound_settings")

object SoundSettingsDataStore {
    private val SOUND_ENABLED_KEY = booleanPreferencesKey("is_sound_enabled")

    suspend fun saveSoundEnabled(context: Context, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[SOUND_ENABLED_KEY] = enabled
        }
    }

    suspend fun loadSoundEnabled(context: Context): Boolean {
        return context.dataStore.data
            .map { prefs -> prefs[SOUND_ENABLED_KEY] ?: true } // по умолчанию звук включен
            .first()
    }
}
