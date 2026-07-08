package com.feofanova.mathup.data.repository

import com.feofanova.mathup.data.local.dao.AppDao
import com.feofanova.mathup.data.local.entities.TaskEntity
import com.feofanova.mathup.data.stats.dao.StatsDao
import com.feofanova.mathup.data.stats.entities.ExamSessionEntity
import com.feofanova.mathup.data.stats.entities.ExamTaskEntity
import com.feofanova.mathup.domain.repository.ExamAnswerCheckItem
import com.feofanova.mathup.domain.repository.ExamRepository
import com.feofanova.mathup.domain.repository.ExamSessionData

class RoomExamRepository(
    private val baseDao: AppDao,
    private val ogeDao: AppDao,
    private val statsDao: StatsDao,
    private val profile: String
) : ExamRepository {

    override suspend fun getLastUnfinishedSession(profile: String): ExamSessionEntity? {
        return statsDao.getLastUnfinishedSession(profile)
    }

    override suspend fun createExam(profile: String): ExamSessionData {
        val tasks = selectTasks(profile)
        val sessionId = createExamSession(profile, tasks)


        return ExamSessionData(
            sessionId = sessionId,
            tasks = tasks,
            selectedTaskId = tasks.firstOrNull()?.taskID?.toLong()
        )
    }

    override suspend fun restoreSession(session: ExamSessionEntity): ExamSessionData {
        val examTaskEntities = statsDao.getTasksBySessionId(session.sessionId)
            .sortedBy { it.orderIndex }
        val orderedTasks = loadOrderedTasks(examTaskEntities)
        val selectedTaskId = examTaskEntities.firstOrNull { it.userAnswer == null }?.taskId?.toLong()
            ?: examTaskEntities.firstOrNull()?.taskId?.toLong()

        return ExamSessionData(
            sessionId = session.sessionId,
            tasks = orderedTasks,
            selectedTaskId = selectedTaskId
        )
    }

    override suspend fun submitAnswer(sessionId: Long, taskId: Int, answer: String): Long? {
        statsDao.updateAnswer(sessionId, taskId, answer, isCorrect = false)

        return statsDao.getTasksBySessionId(sessionId)
            .sortedBy { it.orderIndex }
            .firstOrNull { it.userAnswer == null }
            ?.taskId
            ?.toLong()
    }

    override suspend fun discardSession(sessionId: Long) {
        statsDao.markSessionCompleted(sessionId)
        statsDao.deleteExamTasksForSession(sessionId)
    }

    override suspend fun getCompletedTaskIds(sessionId: Long): Set<Long> {
        return statsDao.getTasksBySessionId(sessionId)
            .filter { !it.userAnswer.isNullOrBlank() }
            .map { it.taskId.toLong() }
            .toSet()
    }

    override suspend fun getAnswersForCheck(sessionId: Long): List<ExamAnswerCheckItem> {
        val examTasks = statsDao.getTasksBySessionId(sessionId).sortedBy { it.orderIndex }
        val orderedTasks = loadOrderedTasks(examTasks)
        val correctAnswers = orderedTasks.associateBy({ it.taskID }, { it.answer })

        return examTasks.map { examTask ->
            ExamAnswerCheckItem(
                taskId = examTask.taskId,
                userAnswer = examTask.userAnswer ?: EMPTY_ANSWER,
                correctAnswer = correctAnswers[examTask.taskId] ?: EMPTY_ANSWER
            )
        }
    }

    override suspend fun finalizeResults(sessionId: Long, results: List<Pair<Int, Boolean>>) {
        results.forEach { (taskId, isCorrect) ->
            statsDao.updateIsCorrect(sessionId = sessionId, taskId = taskId, isCorrect = isCorrect)
        }

        statsDao.updateSessionCompletion(sessionId, results.count { it.second })
    }

    private suspend fun selectTasks(profile: String): List<TaskEntity> {
        val blockRange = when (profile) {
            "База" -> 1..21
            "Профиль" -> 22..40
            "ОГЭ" -> 1..25
            else -> emptyList()
        }

        return blockRange.mapNotNull { blockId ->
            contentDao().getTasksByBlock(blockId).randomOrNull()
        }
    }

    private suspend fun createExamSession(profile: String, tasks: List<TaskEntity>): Long {
        val sessionId = statsDao.insertSession(
            ExamSessionEntity(
                profile = profile,
                remainingTimeSeconds = EXAM_DURATION_SECONDS
            )
        )

        statsDao.insertExamTasks(
            tasks.mapIndexed { index, task ->
                ExamTaskEntity(
                    sessionOwnerId = sessionId,
                    taskId = task.taskID,
                    orderIndex = index
                )
            }
        )

        return sessionId
    }

    private suspend fun loadOrderedTasks(examTasks: List<ExamTaskEntity>): List<TaskEntity> {
        val taskIds = examTasks.map { it.taskId }
        val taskMap = contentDao().getTasksByIds(taskIds).associateBy { it.taskID }

        return examTasks.mapNotNull { taskMap[it.taskId] }
    }

    private fun contentDao(): AppDao {
        return if (profile.equals("огэ", ignoreCase = true)) ogeDao else baseDao
    }

    private companion object {
        const val EXAM_DURATION_SECONDS = 3 * 3600 + 55 * 60
        const val EMPTY_ANSWER = "—"
    }
}
