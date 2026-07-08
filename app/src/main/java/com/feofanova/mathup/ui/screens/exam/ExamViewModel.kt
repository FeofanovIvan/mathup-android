package com.feofanova.mathup.ui.screens.exam

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.feofanova.mathup.data.local.dao.AppDao
import com.feofanova.mathup.data.local.entities.TaskEntity
import com.feofanova.mathup.data.repository.RoomExamRepository
import com.feofanova.mathup.data.stats.dao.StatsDao
import com.feofanova.mathup.data.stats.entities.ExamSessionEntity
import com.feofanova.mathup.domain.repository.ExamAnswerCheckItem
import com.feofanova.mathup.domain.repository.ExamRepository
import kotlinx.coroutines.launch

data class ExamUiState(
    val tasks: List<TaskEntity> = emptyList(),
    val selectedTaskId: Long? = null,
    val selectedTab: TaskExamTab = TaskExamTab.Task,
    val sessionId: Long? = null
)

class ExamViewModelFactory(
    private val baseDao: AppDao,
    private val ogeDao: AppDao,
    private val statsDao: StatsDao,
    private val profile: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExamViewModel::class.java)) {
            val repository = RoomExamRepository(
                baseDao = baseDao,
                ogeDao = ogeDao,
                statsDao = statsDao,
                profile = profile
            )

            @Suppress("UNCHECKED_CAST")
            return ExamViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class ExamViewModel(
    private val examRepository: ExamRepository
) : ViewModel() {

    var uiState by mutableStateOf(ExamUiState())
        private set

    suspend fun getLastUnfinishedSession(profile: String): ExamSessionEntity? {
        return examRepository.getLastUnfinishedSession(profile)
    }

    fun loadExam(profile: String) {
        viewModelScope.launch {
            val exam = examRepository.createExam(profile)
            uiState = uiState.copy(
                sessionId = exam.sessionId,
                tasks = exam.tasks,
                selectedTaskId = exam.selectedTaskId,
                selectedTab = TaskExamTab.Task
            )
        }
    }

    fun restoreSession(session: ExamSessionEntity) {
        viewModelScope.launch {
            val exam = examRepository.restoreSession(session)
            uiState = uiState.copy(
                sessionId = exam.sessionId,
                tasks = exam.tasks,
                selectedTaskId = exam.selectedTaskId,
                selectedTab = TaskExamTab.Task
            )
        }
    }

    fun selectTask(taskId: Long?) {
        uiState = uiState.copy(selectedTaskId = taskId)
    }

    fun selectTab(tab: TaskExamTab) {
        uiState = uiState.copy(selectedTab = tab)
    }

    fun submitAnswer(answer: String) {
        val currentTaskId = uiState.selectedTaskId?.toInt() ?: return
        val currentSessionId = uiState.sessionId ?: return

        viewModelScope.launch {
            val nextTaskId = examRepository.submitAnswer(
                sessionId = currentSessionId,
                taskId = currentTaskId,
                answer = answer
            )
            uiState = uiState.copy(
                selectedTaskId = nextTaskId,
                selectedTab = TaskExamTab.Task
            )
        }
    }

    fun discardAndStartNew(profile: String) {
        viewModelScope.launch {
            uiState.sessionId?.let { examRepository.discardSession(it) }
            loadExam(profile)
        }
    }

    suspend fun getCompletedTaskIds(): Set<Long> {
        val currentSessionId = uiState.sessionId ?: return emptySet()
        return examRepository.getCompletedTaskIds(currentSessionId)
    }

    suspend fun getAnswersForCheck(): List<ExamAnswerCheckItem> {
        val currentSessionId = uiState.sessionId ?: return emptyList()
        return examRepository.getAnswersForCheck(currentSessionId)
    }

    fun finalizeExamResults(results: List<Pair<Int, Boolean>>) {
        viewModelScope.launch {
            val currentSessionId = uiState.sessionId ?: return@launch
            examRepository.finalizeResults(currentSessionId, results)
        }
    }
}
