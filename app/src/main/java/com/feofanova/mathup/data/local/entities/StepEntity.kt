package com.feofanova.mathup.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["taskID"],
            childColumns = ["taskOwnerID"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["taskOwnerID"])]
)
data class StepEntity(
    @PrimaryKey val stepID: Int,
    val solutionVariant: String?,
    @SerializedName("stepText") val stepDescription: String?,
    val stepAction: String?,
    val taskOwnerID: Int
)
