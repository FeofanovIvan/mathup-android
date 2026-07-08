package com.feofanova.mathup.sync

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import kotlinx.coroutines.tasks.await

class ContentUpdateChecker(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val gson: Gson = Gson()
) {
    suspend fun findOutdatedDatabases(context: Context): List<ContentDatabase> {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(SYNC_PREFS, Context.MODE_PRIVATE)
        val localMetadata = prefs.getString(SYNC_METADATA, null)
            ?.let { json ->
                runCatching { gson.fromJson(json, SyncMetadata::class.java) }.getOrNull()
            }
            ?: SyncMetadata()

        return runCatching {
            val remoteEge = remoteVersion("MathUpDatabase")
            val remoteOge = remoteVersion("MathUpOgeDatabase")
            val remoteGame = remoteVersion("GameDatabase")

            buildList {
                if (localMetadata.versionEge < remoteEge) add(ContentDatabase.EGE)
                if (localMetadata.versionOge < remoteOge) add(ContentDatabase.OGE)
                if (localMetadata.versionGame < remoteGame) add(ContentDatabase.GAME)
            }
        }.onFailure { error ->
            Log.e(TAG, "Failed to check remote content versions", error)
        }.getOrDefault(emptyList())
    }

    private suspend fun remoteVersion(documentId: String): Int {
        return firestore.collection("sync_metadata")
            .document(documentId)
            .get()
            .await()
            .getLong("version")
            ?.toInt()
            ?: 1
    }

    private companion object {
        const val TAG = "ContentUpdateChecker"
        const val SYNC_PREFS = "sync_prefs"
        const val SYNC_METADATA = "sync_metadata"
    }
}
