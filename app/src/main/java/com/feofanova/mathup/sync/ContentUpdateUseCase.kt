package com.feofanova.mathup.sync

import android.content.Context
import com.feofanova.mathup.data.characters.GameDataSyncManager
import com.feofanova.mathup.data.characters.GameDatabase
import com.feofanova.mathup.data.local.db.MathUpDatabase
import com.feofanova.mathup.data.local.db.MathUpOgeDatabase
import com.feofanova.mathup.data.repository.DataSyncManager

class ContentUpdateUseCase {
    suspend operator fun invoke(
        context: Context,
        databases: List<ContentDatabase>
    ): Result<Unit> {
        val appContext = context.applicationContext

        return runCatching {
            databases.forEach { database ->
                when (database) {
                    ContentDatabase.EGE -> {
                        val db = MathUpDatabase.getInstance(appContext)
                        DataSyncManager.syncFromRemote(appContext, db)
                    }
                    ContentDatabase.OGE -> {
                        val db = MathUpOgeDatabase.getInstance(appContext)
                        DataSyncManager.syncOgeFromRemote(appContext, db)
                    }
                    ContentDatabase.GAME -> {
                        val db = GameDatabase.getInstance(appContext)
                        GameDataSyncManager.syncGameData(appContext, db)
                    }
                    ContentDatabase.NONE -> Unit
                }
            }
        }
    }
}
