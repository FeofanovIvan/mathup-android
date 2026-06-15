package com.feofanova.mathup.data.characters.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName


@Entity(tableName = "math_characters")
data class MathCharacterEntity(
    @PrimaryKey
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("achievement") val achievement: String,
    @SerializedName("info") val info: String,
    @SerializedName("imageName") val imageName: String
)
