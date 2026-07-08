package com.feofanova.mathup.ui.screens.exam

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.feofanova.mathup.data.local.dao.AppDao
import com.feofanova.mathup.data.stats.entities.ExamSessionEntity
import com.feofanova.mathup.data.stats.entities.ExamTaskEntity
import com.feofanova.mathup.data.local.entities.TaskEntity
import com.feofanova.mathup.data.stats.dao.StatsDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class ExamViewModelFactory(
    private val baseDao: AppDao,
    private val ogeDao: AppDao,
    private val statsDao: StatsDao,
    private val profile: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExamViewModel::class.java)) {
            val dao = if (profile.lowercase() == "огэ") ogeDao else baseDao
            @Suppress("UNCHECKED_CAST")
            return ExamViewModel(dao, statsDao, profile) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}




class ExamViewModel(
    private val dao: AppDao,
    private val statsDao: StatsDao,
    private val profile: String
) : ViewModel() {

    private val _examTasks = MutableStateFlow<List<TaskEntity>>(emptyList())
    val examTasks: StateFlow<List<TaskEntity>> = _examTasks

    var selectedTaskId by mutableStateOf<Long?>(null)
    var selectedTab by mutableStateOf(TaskExamTab.Task)
    var sessionId: Long? = null

    private suspend fun createExamSession(profile: String, tasks: List<TaskEntity>): Long {
        val session = ExamSessionEntity(
            profile = profile,
            remainingTimeSeconds = 3 * 3600 + 55 * 60
        )
        val id = statsDao.insertSession(session)

        statsDao.insertExamTasks(
            tasks.mapIndexed { index, task ->
                ExamTaskEntity(
                    sessionOwnerId = id,
                    taskId = task.taskID,
                    orderIndex = index // ← сохраняем порядок
                )
            }
        )

        return id
    }


    fun loadExam(profile: String) {
        viewModelScope.launch {
            val tasks = withContext(Dispatchers.IO) {
                val blockRange = when (profile) {
                    "База" -> 1..21
                    "Профиль" -> 22..40
                    "ОГЭ" -> 1..25
                    else -> emptyList()
                }
                val selected = blockRange.mapNotNull { id -> dao.getTasksByBlock(id).randomOrNull() }
                Log.d("ExamViewModel", "Выбрано ${selected.size} задач для профиля $profile")
                selected
            }

            _examTasks.value = tasks

            // Сохраняем сессию и ID
            sessionId = withContext(Dispatchers.IO) {
                val newSessionId = createExamSession(profile, tasks)
                Log.d("ExamViewModel", "Создана сессия sessionId=$newSessionId")
                newSessionId
            }

            selectedTaskId = tasks.firstOrNull()?.taskID?.toLong()
            Log.d("ExamViewModel", "Установлен selectedTaskId=$selectedTaskId")
        }
    }
    fun restoreSession(session: ExamSessionEntity) {
        viewModelScope.launch {
            sessionId = session.sessionId

            val examTaskEntities = statsDao.getTasksBySessionId(session.sessionId)
                .sortedBy { it.orderIndex }

            val taskIds = examTaskEntities.map { it.taskId }
            val tasks = dao.getTasksByIds(taskIds)

            // ✨ Восстановим точный порядок через map
            val taskMap = tasks.associateBy { it.taskID }
            val orderedTasks = examTaskEntities.mapNotNull { taskMap[it.taskId] }

            _examTasks.value = orderedTasks

            selectedTaskId = examTaskEntities.firstOrNull { it.userAnswer == null }?.taskId?.toLong()
                ?: examTaskEntities.firstOrNull()?.taskId?.toLong()
        }
    }
    fun submitAnswer(answer: String) {
        val currentTaskId = selectedTaskId?.toInt() ?: return
        val session = sessionId ?: return

        viewModelScope.launch {
            statsDao.updateAnswer(session, currentTaskId, answer, isCorrect = false) // временно false

            val examTaskEntities = statsDao.getTasksBySessionId(session).sortedBy { it.orderIndex }
            val nextUnanswered = examTaskEntities.firstOrNull { it.userAnswer == null }

            selectedTaskId = nextUnanswered?.taskId?.toLong()
        }
    }


    fun discardAndStartNew(profile: String) {
        viewModelScope.launch {
            sessionId?.let {
                statsDao.markSessionCompleted(it)
                statsDao.deleteExamTasksForSession(it)
            }
            loadExam(profile)
        }
    }
    suspend fun getCompletedTaskIds(): Set<Long> {
        val session = sessionId ?: return emptySet()
        return statsDao.getTasksBySessionId(session)
            .filter { !it.userAnswer.isNullOrBlank() }
            .map { it.taskId.toLong() }
            .toSet()
    }
    suspend fun getAnswersForCheck(): List<AnswerCheckItem> {
        val session = sessionId ?: return emptyList()

        return withContext(Dispatchers.IO) {
            val examTasks = statsDao.getTasksBySessionId(session).sortedBy { it.orderIndex }
            val taskIds = examTasks.map { it.taskId }
            val tasks = dao.getTasksByIds(taskIds)
            val correctMap = tasks.associateBy({ it.taskID }, { it.answer })

            examTasks.map { examTask ->
                val user = examTask.userAnswer ?: "—"
                val correct = correctMap[examTask.taskId] ?: "—"

                Log.d(
                    "AnswerCheck",
                    "📝 taskId=${examTask.taskId}, userAnswer=\"$user\", correctAnswer=\"$correct\""
                )

                AnswerCheckItem(
                    taskId = examTask.taskId,
                    userAnswer = user,
                    correctAnswer = correct
                )
            }
        }
    }
    data class AnswerCheckItem(
        val taskId: Int,
        val userAnswer: String,
        val correctAnswer: String,
        val isCorrect: Boolean? = null
    )
    fun finalizeExamResults(results: List<Pair<Int, Boolean>>) {
        viewModelScope.launch {
            val session = sessionId ?: return@launch

            // Сохраняем isCorrect для каждой задачи
            results.forEach { (taskId, isCorrect) ->
                statsDao.updateIsCorrect(sessionId = session, taskId = taskId, isCorrect = isCorrect)
                Log.d("FinalCheck", "💾 taskId=$taskId → isCorrect=$isCorrect")
            }

            val correctCount = results.count { it.second }

            // Обновляем сессию: завершена и подсчитаны правильные
            statsDao.updateSessionCompletion(session, correctCount)

            Log.d("FinalCheck", "🎓 Экзамен завершён. Правильных ответов: $correctCount из ${results.size}")
        }
    }

}
