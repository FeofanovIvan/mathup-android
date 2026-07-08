package com.feofanova.mathup.data.local.dao

import androidx.room.*
import com.feofanova.mathup.data.local.entities.FormulaEntity
import com.feofanova.mathup.data.local.entities.QuestionBlockEntity
import com.feofanova.mathup.data.local.entities.StepEntity
import com.feofanova.mathup.data.local.entities.TaskEntity

@Dao
interface AppDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlocks(blocks: List<QuestionBlockEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<TaskEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSteps(steps: List<StepEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFormulas(formulas: List<FormulaEntity>)

    @Query("DELETE FROM QuestionBlockEntity")
    suspend fun clearBlocks()

    @Query("DELETE FROM TaskEntity")
    suspend fun clearTasks()

    @Query("DELETE FROM StepEntity")
    suspend fun clearSteps()

    @Query("DELETE FROM FormulaEntity")
    suspend fun clearFormulas()

    @Query("SELECT COUNT(*) FROM QuestionBlockEntity")
    suspend fun getBlockCount(): Int

    @Query("SELECT * FROM QuestionBlockEntity WHERE blockID = :id")
    suspend fun getBlockById(id: Long): QuestionBlockEntity?

    @Query("SELECT * FROM FormulaEntity")
    suspend fun getAllFormulas(): List<FormulaEntity>

    @Query("SELECT * FROM FormulaEntity WHERE blockOwnerID = :blockId")
    fun getFormulasForBlock(blockId: Long): List<FormulaEntity>

    @Query("SELECT * FROM TaskEntity WHERE blockOwnerID = :blockId")
    suspend fun getTasksByBlock(blockId: Int): List<TaskEntity>

    @Query("SELECT * FROM TaskEntity")
    suspend fun getAllTasks(): List<TaskEntity>

    @Query("SELECT * FROM StepEntity WHERE taskOwnerID = :taskId ORDER BY stepID")
    suspend fun getStepsForTask(taskId: Int): List<StepEntity>

    @Query("SELECT * FROM TaskEntity WHERE taskID IN (:ids)")
    suspend fun getTasksByIds(ids: List<Int>): List<TaskEntity>

    @Query("SELECT * FROM QuestionBlockEntity WHERE blockID BETWEEN :from AND :to ORDER BY blockID ASC")
    fun getBlocksInRange(from: Int, to: Int): List<QuestionBlockEntity>

    @Query("SELECT * FROM QuestionBlockEntity WHERE blockID = :id")
    suspend fun getBlockById(id: Int): QuestionBlockEntity?

}