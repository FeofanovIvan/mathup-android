package com.feofanova.mathup.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = QuestionBlockEntity::class,
            parentColumns = ["blockID"],
            childColumns = ["blockOwnerID"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class TaskEntity(
    @PrimaryKey val taskID: Int,
    @SerializedName("taskText") val description: String?,
    val drawingLink: String?,
    val answer: String?,
    val hint: String?,
    val given: String?,
    val blockOwnerID: Int
)