package com.feofanova.mathup.data.stats.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = ExamSessionEntity::class,
            parentColumns = ["sessionId"],
            childColumns = ["sessionOwnerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionOwnerId")]
)
data class ExamTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionOwnerId: Long,
    val taskId: Int,
    val orderIndex: Int, // <--- ДОБАВЛЕНО
    val userAnswer: String? = null,
    val isCorrect: Boolean? = null
)