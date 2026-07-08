package com.feofanova.mathup.data.characters

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.room.withTransaction
import com.feofanova.mathup.data.characters.entities.MathCharacterEntity
import com.feofanova.mathup.sync.SyncMetadata
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import java.io.File
import java.net.URL
import kotlinx.coroutines.withContext as withContext1
import androidx.core.content.edit

object GameDataSyncManager {
    private const val JSON_URL =
        "https://firebasestorage.googleapis.com/v0/b/mathappforrus.appspot.com/o/math_game%2Fcharacters_export.json?alt=media"
    private const val IMAGE_BASE = "https://firebasestorage.googleapis.com/v0/b/mathappforrus.appspot.com/o/math_game%2Fimages%2F"

    suspend fun syncGameData(context: Context, gameDb: GameDatabase) {
        withContext1(context = Dispatchers.IO) {
            try {
                val json = URL(JSON_URL).readText()
                val gson = Gson()
                val type = object : TypeToken<List<MathCharacterEntity>>() {}.type
                val characters: List<MathCharacterEntity> = gson.fromJson(json, type)

                gameDb.withTransaction {
                    val dao = gameDb.characterDao()
                    dao.clearAll()
                    dao.insertAll(characters)
                }

                val filesDir = File(context.filesDir, "character_images").apply { mkdirs() }

                var imageErrorsFound = false

                characters.forEach { character ->
                    val imageFile = File(filesDir, character.imageName)
                    if (!imageFile.exists()) {
                        val imageUrl = "$IMAGE_BASE${Uri.encode(character.imageName)}?alt=media"
                        try {
                            URL(imageUrl).openStream().use { input ->
                                imageFile.outputStream().use { output ->
                                    input.copyTo(output)
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("GameImageSync", "Ошибка загрузки: ${character.imageName}", e)
                            imageErrorsFound = true
                        }
                    }
                }

                if (imageErrorsFound) {
                    throw Exception("Не все изображения были успешно загружены")
                }

                try {
                    val firestore = FirebaseFirestore.getInstance()
                    val gameVersion = firestore.collection("sync_metadata")
                        .document("GameDatabase").get().await().getLong("version")?.toInt() ?: 1

                    val prefs = context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
                    val existing = prefs.getString("sync_metadata", null)
                    val oldMetadata = existing?.let {
                        try { Gson().fromJson(it, SyncMetadata::class.java) } catch (_: Exception) { null }
                    }

                    val updated = oldMetadata?.copy(versionGame = gameVersion)
                        ?: SyncMetadata(versionGame = gameVersion)

                    prefs.edit { putString("sync_metadata", Gson().toJson(updated)) }

                } catch (e: Exception) {
                    Log.e("GameDataSync", "Ошибка при обновлении версии GAME", e)
                }


            } catch (e: Exception) {
                Log.e("GameDataSync", "Ошибка при синхронизации данных персонажей", e)
                throw e
            }
        }
    }
    suspend fun syncGameDataFromAssets(context: Context, gameDb: GameDatabase) {
        withContext1(context = Dispatchers.IO) {
            try {

                // 1. Чтение JSON из assets
                val json = context.assets.open("characters_export.json").bufferedReader().use { it.readText() }
                val gson = Gson()
                val type = object : TypeToken<List<MathCharacterEntity>>() {}.type
                val characters: List<MathCharacterEntity> = gson.fromJson(json, type)


                // 2. Импорт в БД
                gameDb.withTransaction {
                    val dao = gameDb.characterDao()
                    dao.clearAll()
                    dao.insertAll(characters)
                }

                // 3. Копирование изображений из assets в filesDir (если ещё не скопированы)
                val outputDir = File(context.filesDir, "character_images").apply { mkdirs() }
                var imageErrorsFound = false

                for (character in characters) {
                    val imageFile = File(outputDir, character.imageName)
                    if (!imageFile.exists()) {
                        try {
                            context.assets.open("character_images/${character.imageName}").use { input ->
                                imageFile.outputStream().use { output ->
                                    input.copyTo(output)
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("GameImageSync", "Ошибка при копировании ${character.imageName}", e)
                            imageErrorsFound = true
                        }
                    }
                }

                if (imageErrorsFound) {
                    throw Exception("Не все изображения были успешно скопированы из assets")
                }


            } catch (e: Exception) {
                Log.e("GameDataSync", "Ошибка при импорте персонажей из assets", e)
                throw e
            }
        }
    }

}
