package com.feofanova.mathup.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.RoomDatabase
import com.feofanova.mathup.data.local.dao.AppDao
import com.feofanova.mathup.data.local.entities.*
import androidx.room.Room


@Database(
    entities = [QuestionBlockEntity::class, TaskEntity::class, StepEntity::class, FormulaEntity::class],
    version = 1
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                                context.applicationContext,
                                AppDatabase::class.java,
                                "mathup_database"
                            ).fallbackToDestructiveMigration(false).build().also {
                    INSTANCE = it
                }
            }
        }
    }
}
