package com.feofanova.mathup.ui.screens.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.feofanova.mathup.data.stats.dao.StatsDao
import kotlinx.coroutines.launch


class SettingsViewModelFactory(private val statsDao: StatsDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(statsDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class SettingsViewModel(private val statsDao: StatsDao) : ViewModel() {
    var examsTaken by mutableIntStateOf(0)
    var taskTaken by mutableIntStateOf(0)
    var bestScoreBase by mutableIntStateOf(0)
    var bestScoreProfile by mutableIntStateOf(0)
    var formulaAccuracy by mutableIntStateOf(0)

    fun loadStats() {
        viewModelScope.launch {
            examsTaken = statsDao.getExamsTaken()
            taskTaken = statsDao.getTasksTaken()
            bestScoreBase = statsDao.getBestScore("База") ?: 0
            bestScoreProfile = statsDao.getBestScore("Профиль") ?: 0

            val formulaStats = statsDao.getFormulaStats()
            val correct = formulaStats.sumOf { it.correctCount }
            val wrong = formulaStats.sumOf { it.wrongCount }
            val total = correct + wrong
            formulaAccuracy = if (total > 0) (correct * 100) / total else 0
        }
    }
}
