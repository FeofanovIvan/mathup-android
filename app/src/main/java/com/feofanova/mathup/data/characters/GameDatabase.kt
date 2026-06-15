package com.feofanova.mathup.data.characters

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.feofanova.mathup.data.characters.dao.MathCharacterDao
import com.feofanova.mathup.data.characters.entities.MathCharacterEntity

@Database(
    entities = [MathCharacterEntity::class],
    version = 1
)
abstract class GameDatabase : RoomDatabase() {
    abstract fun characterDao(): MathCharacterDao

    companion object {
        @Volatile private var INSTANCE: GameDatabase? = null

        fun getInstance(context: Context): GameDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context,
                    GameDatabase::class.java,
                    "game_database"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
