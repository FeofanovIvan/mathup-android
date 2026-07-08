package com.feofanova.mathup.sync

import com.google.gson.annotations.SerializedName

data class SyncMetadata(
    @SerializedName("version_game")
    val versionGame: Int = 1,
    @SerializedName("version_oge")
    val versionOge: Int = 1,
    @SerializedName("version_ege")
    val versionEge: Int = 1,
) {
    val version_game: Int get() = versionGame
    val version_oge: Int get() = versionOge
    val version_ege: Int get() = versionEge
}
