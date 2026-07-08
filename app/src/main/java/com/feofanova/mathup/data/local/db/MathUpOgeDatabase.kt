package com.feofanova.mathup.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 2,
    exportSchema = false
)
abstract class MathUpOgeDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao


    companion object {
        @Volatile
        private var INSTANCE: MathUpOgeDatabase? = null

        fun getInstance(context: Context): MathUpOgeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MathUpOgeDatabase::class.java,
                    "mathup_oge.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration(false)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_TaskEntity_blockOwnerID` ON `TaskEntity` (`blockOwnerID`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_StepEntity_taskOwnerID` ON `StepEntity` (`taskOwnerID`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_FormulaEntity_blockOwnerID` ON `FormulaEntity` (`blockOwnerID`)")
            }
        }
    }
}
