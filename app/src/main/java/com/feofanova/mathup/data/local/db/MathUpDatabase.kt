package com.feofanova.mathup.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.feofanova.mathup.data.local.dao.AppDao
import com.feofanova.mathup.data.local.entities.*

@Database(
    entities = [
        QuestionBlockEntity::class,
        TaskEntity::class,
        StepEntity::class,
        FormulaEntity::class
    ],
    version = 3,
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
                            )
                                .addMigrations(MIGRATION_2_3)
                                .fallbackToDestructiveMigration(false)
                                .build()
                INSTANCE = instance
                instance
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_TaskEntity_blockOwnerID` ON `TaskEntity` (`blockOwnerID`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_StepEntity_taskOwnerID` ON `StepEntity` (`taskOwnerID`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_FormulaEntity_blockOwnerID` ON `FormulaEntity` (`blockOwnerID`)")
            }
        }
    }
}
