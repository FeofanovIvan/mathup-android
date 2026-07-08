package com.feofanova.mathup.data.stats.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class ExamSessionEntity(
    @PrimaryKey(autoGenerate = true) val sessionId: Long = 0,
    val profile: String,
    val remainingTimeSeconds: Int,
    val isCompleted: Boolean = false,
    val correctAnswersCount: Int = 0
)