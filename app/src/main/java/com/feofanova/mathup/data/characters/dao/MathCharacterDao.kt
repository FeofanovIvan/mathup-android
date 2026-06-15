package com.feofanova.mathup.data.characters.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.feofanova.mathup.data.characters.entities.MathCharacterEntity


@Dao
interface MathCharacterDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(characters: List<MathCharacterEntity>)

    @Query("SELECT * FROM math_characters")
    suspend fun getAll(): List<MathCharacterEntity>

    @Query("DELETE FROM math_characters")
    suspend fun clearAll()


    @Query("SELECT COUNT(*) FROM math_characters")
    suspend fun getCount(): Int

}
