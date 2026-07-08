package com.feofanova.mathup.data.repository

import com.feofanova.mathup.data.local.dao.AppDao
import com.feofanova.mathup.data.local.entities.StepEntity
import com.feofanova.mathup.data.local.entities.TaskEntity
import com.feofanova.mathup.data.stats.dao.StatsDao
import com.feofanova.mathup.data.stats.entities.SessionAnswerEntity
import com.feofanova.mathup.data.stats.entities.SessionStepEntity
import com.feofanova.mathup.domain.repository.TaskRepository

class RoomTaskRepository(
    private val baseDao: AppDao,
    private val ogeDao: AppDao,
    private val statsDao: StatsDao,
    private val profile: String
) : TaskRepository {

    override suspend fun getTasksForBlock(blockId: Int): List<TaskEntity> {
        val dao = contentDao()
        val adjustedBlockId = when {
            profile.equals("огэ", ignoreCase = true) && blockId in 1..25 -> blockId
            profile.equals("профиль", ignoreCase = true) && blockId in 1..19 -> blockId + PROFILE_BLOCK_OFFSET
            else -> blockId
        }

        return dao.getTasksByBlock(adjustedBlockId)
    }

    override suspend fun getStepsForTask(taskId: Int): List<StepEntity> {
        return contentDao().getStepsForTask(taskId)
    }

    override suspend fun getCompletedTaskIds(): Set<Long> {
        return statsDao.getAllCompletedAnswers()
            .map { it.taskId }
            .toSet()
    }

    override suspend fun getSavedStepsForTask(taskId: Long): List<SessionStepEntity> {
        return statsDao.getStepsForTask(taskId)
            .sortedBy { it.stepIndex }
    }

    override suspend fun saveStepAnswer(step: SessionStepEntity) {
        statsDao.insertSessionStep(step)
    }

    override suspend fun markTaskCompleted(taskId: Long) {
        statsDao.insertSessionAnswer(
            SessionAnswerEntity(
                taskId = taskId,
                isCompleted = true
            )
        )
    }

    override suspend fun clearTaskProgress(taskId: Long) {
        statsDao.clearStepsForTask(taskId)
        statsDao.clearSessionAnswer(taskId)
    }

    override suspend fun getSessionAnswer(taskId: Long): SessionAnswerEntity? {
        return statsDao.getSessionAnswer(taskId)
    }

    private fun contentDao(): AppDao {
        return if (profile.equals("огэ", ignoreCase = true)) ogeDao else baseDao
    }

    private companion object {
        const val PROFILE_BLOCK_OFFSET = 21
    }
}
