package com.feofanova.mathup.domain.repository

import com.feofanova.mathup.data.local.entities.TaskEntity
import com.feofanova.mathup.data.stats.entities.ExamSessionEntity

data class ExamSessionData(
    val sessionId: Long,
    val tasks: List<TaskEntity>,
    val selectedTaskId: Long?
)

data class ExamAnswerCheckItem(
    val taskId: Int,
    val userAnswer: String,
    val correctAnswer: String,
    val isCorrect: Boolean? = null
)

interface ExamRepository {
    suspend fun getLastUnfinishedSession(profile: String): ExamSessionEntity?
    suspend fun createExam(profile: String): ExamSessionData
    suspend fun restoreSession(session: ExamSessionEntity): ExamSessionData
    suspend fun submitAnswer(sessionId: Long, taskId: Int, answer: String): Long?
    suspend fun discardSession(sessionId: Long)
    suspend fun getCompletedTaskIds(sessionId: Long): Set<Long>
    suspend fun getAnswersForCheck(sessionId: Long): List<ExamAnswerCheckItem>
    suspend fun finalizeResults(sessionId: Long, results: List<Pair<Int, Boolean>>)
}
