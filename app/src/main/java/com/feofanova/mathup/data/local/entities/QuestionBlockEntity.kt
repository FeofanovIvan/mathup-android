package com.feofanova.mathup.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity
data class QuestionBlockEntity(
    @PrimaryKey val blockID: Int,
    val name: String?,
    val videoLink: String?,
    val referenceMaterial: String?
)