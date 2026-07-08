package com.feofanova.mathup.data.stats.dao

import androidx.room.*
import com.feofanova.mathup.data.stats.entities.ExamSessionEntity
import com.feofanova.mathup.data.stats.entities.ExamTaskEntity
import com.feofanova.mathup.data.stats.entities.FormulaResultEntity
import com.feofanova.mathup.data.stats.entities.SessionAnswerEntity
import com.feofanova.mathup.data.stats.entities.SessionStepEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StatsDao {

    // --- Formula Stats ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFormulaResult(result: FormulaResultEntity)

    @Query("SELECT * FROM FormulaResultEntity WHERE formulaID = :id")
    suspend fun getFormulaResultById(id: Int): FormulaResultEntity?

    @Query("UPDATE FormulaResultEntity SET correctCount = correctCount + 1 WHERE formulaID = :id")
    suspend fun incrementCorrect(id: Int)

    @Query("UPDATE FormulaResultEntity SET wrongCount = wrongCount + 1 WHERE formulaID = :id")
    suspend fun incrementWrong(id: Int)

    @Query("DELETE FROM FormulaResultEntity")
    suspend fun clearAll()

    @Query("SELECT SUM(correctCount) FROM FormulaResultEntity")
    suspend fun getTotalCorrect(): Int?

    @Query("SELECT SUM(correctCount + wrongCount) FROM FormulaResultEntity")
    suspend fun getTotalAnswered(): Int?

    // --- Session Steps ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessionStep(step: SessionStepEntity)

    @Query("SELECT * FROM session_steps WHERE taskId = :taskId")
    suspend fun getStepsForTask(taskId: Long): List<SessionStepEntity>

    @Query("DELETE FROM session_steps WHERE taskId = :taskId")
    suspend fun clearStepsForTask(taskId: Long)

    // --- Session Answers ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessionAnswer(answer: SessionAnswerEntity)

    @Query("SELECT * FROM session_answers WHERE taskId = :taskId LIMIT 1")
    suspend fun getSessionAnswer(taskId: Long): SessionAnswerEntity?

    @Query("DELETE FROM session_answers WHERE taskId = :taskId")
    suspend fun clearSessionAnswer(taskId: Long)

    @Query("SELECT answerLatex FROM session_steps WHERE taskId = :taskId AND stepIndex = :index LIMIT 1")
    suspend fun getAnswerForStep(taskId: Long, index: Int): String?

    @Query("DELETE FROM session_steps")
    suspend fun clearSessionSteps()


    @Query("DELETE FROM session_answers")
    suspend fun clearSessionAnswers()

    @Query("SELECT * FROM session_answers")
    suspend fun getAllSessionAnswers(): List<SessionAnswerEntity>

    @Query("SELECT * FROM session_answers WHERE isCompleted = 1")
    suspend fun getAllCompletedAnswers(): List<SessionAnswerEntity>

    // ───── Сессия экзамена ─────

    @Insert
    suspend fun insertSession(session: ExamSessionEntity): Long

    @Query("SELECT * FROM ExamSessionEntity WHERE sessionId = :id")
    suspend fun getSessionById(id: Long): ExamSessionEntity?

    @Update
    suspend fun updateSession(session: ExamSessionEntity)

    @Query("UPDATE ExamSessionEntity SET isCompleted = 1 WHERE sessionId = :id")
    suspend fun markSessionCompleted(id: Long)

    @Query("UPDATE ExamSessionEntity SET correctAnswersCount = :count WHERE sessionId = :id")
    suspend fun updateCorrectCount(id: Long, count: Int)

    @Query("UPDATE ExamSessionEntity SET remainingTimeSeconds = :seconds WHERE sessionId = :id")
    suspend fun updateRemainingTime(id: Long, seconds: Int)

    // ───── Список заданий сессии ─────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExamSession(session: ExamSessionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExamTasks(tasks: List<ExamTaskEntity>)

    @Query("SELECT * FROM ExamSessionEntity WHERE profile = :profile AND isCompleted = 0 ORDER BY sessionId DESC LIMIT 1")
    suspend fun getLastUnfinishedSession(profile: String): ExamSessionEntity?

    @Query("SELECT * FROM ExamTaskEntity WHERE sessionOwnerId = :sessionId")
    suspend fun getTasksForSession(sessionId: Long): List<ExamTaskEntity>

    @Query("DELETE FROM ExamTaskEntity WHERE sessionOwnerId = :sessionId")
    suspend fun clearExamTasks(sessionId: Long)

    @Query("DELETE FROM ExamTaskEntity WHERE sessionOwnerId = :sessionId")
    suspend fun deleteExamTasksForSession(sessionId: Long)

    @Query("""
    UPDATE ExamTaskEntity 
    SET userAnswer = :answer, isCorrect = :isCorrect 
    WHERE sessionOwnerId = :sessionId AND taskId = :taskId
""")
    suspend fun updateAnswer(sessionId: Long, taskId: Int, answer: String, isCorrect: Boolean)

    @Query("SELECT * FROM ExamTaskEntity WHERE sessionOwnerId = :sessionId")
    suspend fun getTasksBySessionId(sessionId: Long): List<ExamTaskEntity>

    @Query("SELECT * FROM ExamTaskEntity WHERE sessionOwnerId = :sessionId AND taskId = :taskId LIMIT 1")
    suspend fun getExamTask(sessionId: Long, taskId: Int): ExamTaskEntity?

    @Query("""
        UPDATE ExamTaskEntity 
        SET isCorrect = :isCorrect 
        WHERE sessionOwnerId = :sessionId AND taskId = :taskId
    """)
        suspend fun markIsCorrect(sessionId: Long, taskId: Int, isCorrect: Boolean)

        @Query("UPDATE ExamTaskEntity SET isCorrect = :isCorrect WHERE sessionOwnerId = :sessionId AND taskId = :taskId")
        suspend fun updateIsCorrect(sessionId: Long, taskId: Int, isCorrect: Boolean)

        @Query("UPDATE ExamSessionEntity SET isCompleted = 1, correctAnswersCount = :correctCount WHERE sessionId = :sessionId")
        suspend fun updateSessionCompletion(sessionId: Long, correctCount: Int)

    @Query("SELECT COUNT(*) FROM ExamSessionEntity")
    suspend fun getExamsTaken(): Int

    @Query("SELECT COUNT(*) FROM session_answers WHERE isCompleted = 1")
    suspend fun getTasksTaken(): Int

    @Query("SELECT MAX(correctAnswersCount) FROM ExamSessionEntity WHERE profile = :profile")
    suspend fun getBestScore(profile: String): Int?

    @Query("SELECT * FROM FormulaResultEntity")
    suspend fun getFormulaStats(): List<FormulaResultEntity>

    @Query("""
    SELECT CASE 
      WHEN COALESCE(SUM(correctCount),0) + COALESCE(SUM(wrongCount),0) = 0 
        THEN 0.0 
      ELSE CAST(COALESCE(SUM(correctCount),0) AS REAL)
           / (COALESCE(SUM(correctCount),0) + COALESCE(SUM(wrongCount),0))
    END 
    FROM FormulaResultEntity
  """)
    fun observeCorrectPercent(): Flow<Float>

}

