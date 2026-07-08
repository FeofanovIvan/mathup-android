package com.feofanova.mathup.domain.repository

import com.feofanova.mathup.data.local.entities.StepEntity
import com.feofanova.mathup.data.local.entities.TaskEntity
import com.feofanova.mathup.data.stats.entities.SessionAnswerEntity
import com.feofanova.mathup.data.stats.entities.SessionStepEntity

interface TaskRepository {
    suspend fun getTasksForBlock(blockId: Int): List<TaskEntity>
    suspend fun getStepsForTask(taskId: Int): List<StepEntity>
    suspend fun getCompletedTaskIds(): Set<Long>
    suspend fun getSavedStepsForTask(taskId: Long): List<SessionStepEntity>
    suspend fun saveStepAnswer(step: SessionStepEntity)
    suspend fun markTaskCompleted(taskId: Long)
    suspend fun clearTaskProgress(taskId: Long)
    suspend fun getSessionAnswer(taskId: Long): SessionAnswerEntity?
}
