package com.feofanova.mathup.ui.screens.preparation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.feofanova.mathup.data.local.entities.StepEntity
import android.util.Log
import androidx.compose.runtime.mutableIntStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.feofanova.mathup.data.local.dao.AppDao
import com.feofanova.mathup.data.local.entities.TaskEntity
import com.feofanova.mathup.data.stats.dao.StatsDao
import com.feofanova.mathup.data.stats.entities.SessionAnswerEntity
import com.feofanova.mathup.data.stats.entities.SessionStepEntity
import kotlinx.coroutines.launch

class TaskViewModelFactory(
    private val baseDao: AppDao,
    private val ogeDao: AppDao,
    private val statsDao: StatsDao,
    private val profile: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TaskViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TaskViewModel(baseDao, ogeDao, statsDao, profile) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}




class TaskViewModel(
    private val baseDao: AppDao,
    private val ogeDao: AppDao,
    private val statsDao: StatsDao,
    private val profile: String
) : ViewModel() {

    private val dao: AppDao
        get() = if (profile.lowercase() == "огэ") ogeDao else baseDao

    var currentStepIndex by mutableIntStateOf(0)
        private set

    var tasks by mutableStateOf<List<TaskEntity>>(emptyList())
        private set

    var selectedTaskID by mutableStateOf<Long?>(null)
        private set

    var steps by mutableStateOf<List<StepEntity>>(emptyList())
        private set

    var previousSteps by mutableStateOf<List<SessionStepEntity>>(emptyList())
        private set

    var completedTaskIDs by mutableStateOf<Set<Long>>(emptySet())
        private set

    fun loadTasks(blockId: Int) {
        viewModelScope.launch {
            val adjustedBlockId = when {
                profile.equals("огэ", ignoreCase = true) && blockId in 1..25 -> blockId
                profile.equals("профиль", ignoreCase = true) && blockId in 1..19 -> blockId + 21
                else -> blockId // база или fallback
            }

            val loaded = dao.getTasksByBlock(adjustedBlockId)
            Log.d("TaskViewModel", "getTasksByBlock($adjustedBlockId) -> ${loaded.size} задач")
            tasks = loaded

            if (loaded.isEmpty()) return@launch

            val completed = statsDao.getAllCompletedAnswers().map { it.taskId }.toSet()
            completedTaskIDs = completed

            val allTaskIds = loaded.map { it.taskID.toLong() }
            val firstUncompleted = allTaskIds.firstOrNull { it !in completed }

            val currentSelected = selectedTaskID
            if (currentSelected == null) {
                val selected = firstUncompleted ?: allTaskIds.firstOrNull()
                if (selected != null) {
                    selectedTaskID = selected
                    loadSteps(selected.toInt())
                }
            } else {
                if (allTaskIds.contains(currentSelected)) {
                    loadSteps(currentSelected.toInt())
                } else {
                    val fallback = firstUncompleted ?: allTaskIds.firstOrNull()
                    selectedTaskID = fallback
                    fallback?.let { loadSteps(it.toInt()) }
                }
            }
        }
    }


    // 2) Явно выбрать задачу (нажатие в SideMenu), сразу подгружаем шаги
    fun selectTask(id: Long) {
        selectedTaskID = id
        loadSteps(id.toInt())
    }

    // 3) Загружает шаги для конкретного taskId
    private fun loadSteps(taskId: Int) {
        viewModelScope.launch {
            val loadedSteps = dao.getStepsForTask(taskId)
            steps = loadedSteps.sortedBy { it.stepID }
            currentStepIndex = 0

            // После того, как шаги пришли, подтягиваем прогресс (сохранённые шаги) из БД
            loadStepProgress(taskId.toLong())
        }
    }

    private fun loadStepProgress(taskId: Long) {
        viewModelScope.launch {
            val saved = statsDao.getStepsForTask(taskId)
            previousSteps = saved.sortedBy { it.stepIndex }
            currentStepIndex = previousSteps.size
        }
    }

    fun saveCurrentStepAnswer(isCorrect: Boolean, answerLatex: String) {
        val currentStep = steps.getOrNull(currentStepIndex) ?: return
        val taskId = selectedTaskID ?: return

        val stepResult = SessionStepEntity(
            taskId = taskId,
            stepIndex = currentStepIndex,
            isCorrect = isCorrect,
            answerLatex = answerLatex
        )

        // Добавляем в локальный список, чтобы сразу отрисовать в UI
        previousSteps = previousSteps + stepResult

        Log.d("TaskViewModel", "Сохраняем шаг: stepIndex=$currentStepIndex, correct=$isCorrect, latex=$answerLatex")

        viewModelScope.launch {
            try {
                statsDao.insertSessionStep(stepResult)
                Log.d("TaskViewModel", "Успешно сохранено в БД: $stepResult")

                if (isCorrect) {
                    // Если это не последний шаг, просто переходим к следующему
                    if (currentStepIndex + 1 < steps.size) {
                        currentStepIndex++
                    }
                    // Если же это последняя задача, "отъезжаем" за конец списка
                    else {
                        currentStepIndex = steps.size
                        markTaskAsCompleted()
                    }
                }
            } catch (e: Exception) {
                Log.e("TaskViewModel", "Ошибка при сохранении в БД: ${e.localizedMessage}", e)
            }
        }
    }

    private fun markTaskAsCompleted() {
        val taskId = selectedTaskID ?: return
        viewModelScope.launch {
            try {
                statsDao.insertSessionAnswer(
                    SessionAnswerEntity(
                        taskId = taskId,
                        isCompleted = true
                    )
                )
                completedTaskIDs = completedTaskIDs + taskId
                Log.d("TaskViewModel", "Task $taskId marked as completed.")
            } catch (e: Exception) {
                Log.e("TaskViewModel", "Ошибка при сохранении завершения задачи: ${e.localizedMessage}", e)
            }
        }
    }
    fun resetTaskProgress() {
        val taskId = selectedTaskID ?: return

        // ――― Синхронно очищаем локальное состояние ―――
        previousSteps = emptyList()
        currentStepIndex = 0

        // После этого UI сразу увидит, что нет сохранённых шагов и текущий шаг — это шаг 0.
        // (Если steps ещё не загружены, сначала загрузка оставит старые steps,
        // но currentStepIndex=0 всё равно покажет первый шаг из них.)

        // ――― Асинхронно чистим БД и перезагружаем шаги ―――
        viewModelScope.launch {
            try {
                // 1) Удаляем сохранённый прогресс по шагам в БД
                statsDao.clearStepsForTask(taskId)
                // 2) Удаляем пометку о выполнении задачи
                statsDao.clearSessionAnswer(taskId)

                // 3) Сразу же перегружаем шаги из DAO (они не изменятся,
                //    потому что StepEntity лежат в другой таблице)
                loadSteps(taskId.toInt())

                // 4) Обновляем список выполненных задач (SessionAnswerEntity)
                //    чтобы сбросилась галочка «выполнено» в меню
                val updatedAnswers = tasks.mapNotNull { task ->
                    statsDao.getSessionAnswer(task.taskID.toLong())
                }.map { it.taskId }
                completedTaskIDs = updatedAnswers.toSet()

                Log.d("TaskViewModel", "Прогресс сброшен для taskId=$taskId")
            } catch (e: Exception) {
                Log.e("TaskViewModel", "Ошибка при сбросе прогресса: ${e.localizedMessage}", e)
            }
        }
    }
    fun goToNextTask() {
        val currentIndex = tasks.indexOfFirst { it.taskID.toLong() == selectedTaskID }
        val nextTask = tasks.getOrNull(currentIndex + 1)
        nextTask?.let {
            selectedTaskID = it.taskID.toLong()
            loadSteps(it.taskID)
        }
    }
    fun loadBlockProgress(blockIds: List<Int>, onResult: (Map<Int, Float>) -> Unit) {
        viewModelScope.launch {
            val completedAnswers = statsDao.getAllCompletedAnswers().map { it.taskId }.toSet()

            val progressMap = mutableMapOf<Int, Float>()

            for (blockId in blockIds) {
                val tasks = dao.getTasksByBlock(blockId)
                val total = tasks.size
                val completed = tasks.count { it.taskID.toLong() in completedAnswers }

                val progress = if (total > 0) completed.toFloat() / total else 0f
                progressMap[blockId] = progress
            }

            onResult(progressMap)
        }
    }

}
