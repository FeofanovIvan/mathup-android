package com.feofanova.mathup.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.feofanova.mathup.data.local.dao.AppDao
import com.feofanova.mathup.data.local.entities.*

@Database(
    entities = [
        QuestionBlockEntity::class,
        TaskEntity::class,
        StepEntity::class,
        FormulaEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class MathUpDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao

    companion object {
        @Volatile
        private var INSTANCE: MathUpDatabase? = null

        fun getInstance(context: Context): MathUpDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                                context.applicationContext,
                                MathUpDatabase::class.java,
                                "mathup_database"
                            ).fallbackToDestructiveMigration(false).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
