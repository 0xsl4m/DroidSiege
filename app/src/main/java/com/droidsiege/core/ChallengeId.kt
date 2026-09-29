package com.droidsiege.core

data class ChallengeId(
    val category: String,
    val slug: String,
    val level: Difficulty,
) {
    val key: String
        get() = "$category/$slug/L${level.ordinal + 1}"
}
