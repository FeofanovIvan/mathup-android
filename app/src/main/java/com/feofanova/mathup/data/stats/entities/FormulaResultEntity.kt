package com.feofanova.mathup.data.stats.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class FormulaResultEntity(
    @PrimaryKey val formulaID: Int,
    val correctCount: Int = 0,
    val wrongCount: Int = 0
)
