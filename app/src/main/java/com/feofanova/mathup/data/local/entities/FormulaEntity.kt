package com.feofanova.mathup.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

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
data class FormulaEntity(
    @PrimaryKey val formulaID: Int,
    val name: String?,
    val formula: String?,
    val blockOwnerID: Int
)
