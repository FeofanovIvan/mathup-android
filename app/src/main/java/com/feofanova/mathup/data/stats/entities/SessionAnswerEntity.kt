package com.feofanova.mathup.data.stats.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "session_answers")
data class SessionAnswerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long,
    val isCompleted: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
