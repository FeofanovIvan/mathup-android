package com.feofanova.mathup.ui.screens.preparation

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.feofanova.mathup.data.local.dao.AppDao
import com.feofanova.mathup.data.local.entities.StepEntity
import com.feofanova.mathup.data.local.entities.TaskEntity
import com.feofanova.mathup.data.repository.RoomTaskRepository
import com.feofanova.mathup.data.stats.dao.StatsDao
import com.feofanova.mathup.data.stats.entities.SessionStepEntity
import com.feofanova.mathup.domain.repository.TaskRepository
import kotlinx.coroutines.launch

data class TaskUiState(
    val tasks: List<TaskEntity> = emptyList(),
    val selectedTaskId: Long? = null,
    val steps: List<StepEntity> = emptyList(),
    val previousSteps: List<SessionStepEntity> = emptyList(),
    val completedTaskIds: Set<Long> = emptySet(),
    val currentStepIndex: Int = 0
)

class TaskViewModelFactory(
    private val baseDao: AppDao,
    private val ogeDao: AppDao,
    private val statsDao: StatsDao,
    private val profile: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TaskViewModel::class.java)) {
            val repository = RoomTaskRepository(
                baseDao = baseDao,
                ogeDao = ogeDao,
                statsDao = statsDao,
                profile = profile
            )
            @Suppress("UNCHECKED_CAST")
            return TaskViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class TaskViewModel(
    private val taskRepository: TaskRepository
) : ViewModel() {

    var uiState by mutableStateOf(TaskUiState())
        private set

    fun loadTasks(blockId: Int) {
        viewModelScope.launch {
            val loadedTasks = taskRepository.getTasksForBlock(blockId)
            if (loadedTasks.isEmpty()) {
                uiState = uiState.copy(tasks = emptyList())
                return@launch
            }

            val completedTaskIds = taskRepository.getCompletedTaskIds()
            val taskIds = loadedTasks.map { it.taskID.toLong() }
            val firstUncompleted = taskIds.firstOrNull { it !in completedTaskIds }
            val currentSelected = uiState.selectedTaskId
            val nextSelected = when {
                currentSelected == null -> firstUncompleted ?: taskIds.firstOrNull()
                currentSelected in taskIds -> currentSelected
                else -> firstUncompleted ?: taskIds.firstOrNull()
            }

            uiState = uiState.copy(
                tasks = loadedTasks,
                completedTaskIds = completedTaskIds,
                selectedTaskId = nextSelected
            )

            nextSelected?.let { loadSteps(it.toInt()) }
        }
    }

    fun selectTask(id: Long) {
        uiState = uiState.copy(selectedTaskId = id)
        loadSteps(id.toInt())
    }

    private fun loadSteps(taskId: Int) {
        viewModelScope.launch {
            val loadedSteps = taskRepository.getStepsForTask(taskId)
                .sortedBy { it.stepID }

            uiState = uiState.copy(
                steps = loadedSteps,
                currentStepIndex = 0
            )
            loadStepProgress(taskId.toLong())
        }
    }

    private fun loadStepProgress(taskId: Long) {
        viewModelScope.launch {
            val savedSteps = taskRepository.getSavedStepsForTask(taskId)
            uiState = uiState.copy(
                previousSteps = savedSteps,
                currentStepIndex = savedSteps.size
            )
        }
    }

    fun saveCurrentStepAnswer(isCorrect: Boolean, answerLatex: String) {
        val state = uiState
        val currentStep = state.steps.getOrNull(state.currentStepIndex) ?: return
        val taskId = state.selectedTaskId ?: return

        val stepResult = SessionStepEntity(
            taskId = taskId,
            stepIndex = state.currentStepIndex,
            isCorrect = isCorrect,
            answerLatex = answerLatex
        )

        uiState = state.copy(previousSteps = state.previousSteps + stepResult)

        viewModelScope.launch {
            try {
                taskRepository.saveStepAnswer(stepResult)

                if (isCorrect) {
                    advanceAfterCorrectStep()
                }
            } catch (e: Exception) {
                Log.e("TaskViewModel", "Ошибка при сохранении в БД: ${e.localizedMessage}", e)
            }
        }
    }

    private fun advanceAfterCorrectStep() {
        val state = uiState
        if (state.currentStepIndex + 1 < state.steps.size) {
            uiState = state.copy(currentStepIndex = state.currentStepIndex + 1)
        } else {
            uiState = state.copy(currentStepIndex = state.steps.size)
            markTaskAsCompleted()
        }
    }

    private fun markTaskAsCompleted() {
        val taskId = uiState.selectedTaskId ?: return
        viewModelScope.launch {
            try {
                taskRepository.markTaskCompleted(taskId)
                uiState = uiState.copy(completedTaskIds = uiState.completedTaskIds + taskId)
            } catch (e: Exception) {
                Log.e("TaskViewModel", "Ошибка при сохранении завершения задачи: ${e.localizedMessage}", e)
            }
        }
    }

    fun resetTaskProgress() {
        val taskId = uiState.selectedTaskId ?: return
        uiState = uiState.copy(
            previousSteps = emptyList(),
            currentStepIndex = 0
        )

        viewModelScope.launch {
            try {
                taskRepository.clearTaskProgress(taskId)
                loadSteps(taskId.toInt())

                val updatedAnswers = uiState.tasks.mapNotNull { task ->
                    taskRepository.getSessionAnswer(task.taskID.toLong())
                }.map { it.taskId }

                uiState = uiState.copy(completedTaskIds = updatedAnswers.toSet())
            } catch (e: Exception) {
                Log.e("TaskViewModel", "Ошибка при сбросе прогресса: ${e.localizedMessage}", e)
            }
        }
    }

    fun goToNextTask() {
        val state = uiState
        val currentIndex = state.tasks.indexOfFirst { it.taskID.toLong() == state.selectedTaskId }
        val nextTask = state.tasks.getOrNull(currentIndex + 1)

        nextTask?.let {
            uiState = state.copy(selectedTaskId = it.taskID.toLong())
            loadSteps(it.taskID)
        }
    }

    fun loadBlockProgress(blockIds: List<Int>, onResult: (Map<Int, Float>) -> Unit) {
        viewModelScope.launch {
            val completedAnswers = taskRepository.getCompletedTaskIds()
            val progressMap = mutableMapOf<Int, Float>()

            for (blockId in blockIds) {
                val blockTasks = taskRepository.getTasksForBlock(blockId)
                val total = blockTasks.size
                val completed = blockTasks.count { it.taskID.toLong() in completedAnswers }
                progressMap[blockId] = if (total > 0) completed.toFloat() / total else 0f
            }

            onResult(progressMap)
        }
    }
}
