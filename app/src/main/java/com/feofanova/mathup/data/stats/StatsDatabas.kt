package com.feofanova.mathup.data.stats

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.feofanova.mathup.data.stats.entities.ExamSessionEntity
import com.feofanova.mathup.data.stats.entities.ExamTaskEntity
import com.feofanova.mathup.data.stats.dao.StatsDao
import com.feofanova.mathup.data.stats.entities.FormulaResultEntity
import com.feofanova.mathup.data.stats.entities.SessionAnswerEntity
import com.feofanova.mathup.data.stats.entities.SessionStepEntity

@Database(
    entities = [
        FormulaResultEntity::class,
        SessionStepEntity::class,
        SessionAnswerEntity::class,
        ExamSessionEntity::class,
        ExamTaskEntity::class
    ],
    version = 4
)
abstract class StatsDatabase : RoomDatabase() {
    abstract fun statsDao(): StatsDao

    companion object {
        @Volatile private var INSTANCE: StatsDatabase? = null

        fun getInstance(context: Context): StatsDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    StatsDatabase::class.java,
                    "stats.db"
                )
                    .fallbackToDestructiveMigration(false)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
