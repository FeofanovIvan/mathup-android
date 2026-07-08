package com.feofanova.mathup.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.feofanova.mathup.data.local.dao.AppDao
import com.feofanova.mathup.data.local.entities.FormulaEntity
import com.feofanova.mathup.data.local.entities.QuestionBlockEntity
import com.feofanova.mathup.data.local.entities.StepEntity
import com.feofanova.mathup.data.local.entities.TaskEntity

@Database(
    entities = [
        QuestionBlockEntity::class,
        TaskEntity::class,
        StepEntity::class,
        FormulaEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MathUpOgeDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao // добавь это


    companion object {
        @Volatile
        private var INSTANCE: MathUpOgeDatabase? = null

        fun getInstance(context: Context): MathUpOgeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MathUpOgeDatabase::class.java,
                    "mathup_oge.db" // ✅ другое имя
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}