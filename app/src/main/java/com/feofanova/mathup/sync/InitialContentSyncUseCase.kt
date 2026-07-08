package com.feofanova.mathup.sync

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import com.feofanova.mathup.data.characters.GameDataSyncManager
import com.feofanova.mathup.data.characters.GameDatabase
import com.feofanova.mathup.data.local.db.MathUpDatabase
import com.feofanova.mathup.data.local.db.MathUpOgeDatabase
import com.feofanova.mathup.data.repository.DataSyncManager
import com.google.gson.Gson

class InitialContentSyncUseCase(
    private val gson: Gson = Gson()
) {
    suspend operator fun invoke(context: Context): Result<Unit> {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(SYNC_PREFS, Context.MODE_PRIVATE)
        val metadataJson = prefs.getString(SYNC_METADATA, null)

        if (metadataJson != null) {
            return Result.success(Unit)
        }

        return runCatching {

            val gameDb = GameDatabase.getInstance(appContext)
            val egeDb = MathUpDatabase.getInstance(appContext)
            val ogeDb = MathUpOgeDatabase.getInstance(appContext)

            DataSyncManager.syncFromAssets(appContext, egeDb)
            DataSyncManager.syncOgeFromAssets(appContext, ogeDb)
            GameDataSyncManager.syncGameDataFromAssets(appContext, gameDb)

            prefs.edit {
                putString(SYNC_METADATA, gson.toJson(SyncMetadata()))
            }
            Unit
        }.onFailure { error ->
            Log.e(TAG, "Failed to import bundled content", error)
        }
    }

    private companion object {
        const val TAG = "InitialContentSync"
        const val SYNC_PREFS = "sync_prefs"
        const val SYNC_METADATA = "sync_metadata"
    }
}
