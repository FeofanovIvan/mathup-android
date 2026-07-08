package com.feofanova.mathup.data.stats.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "session_steps")
data class SessionStepEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long,
    val stepIndex: Int,
    val isCorrect: Boolean,
    val answerLatex: String
)
