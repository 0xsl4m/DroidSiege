package com.droidsiege.core

object Flag {
    private val PATTERN = Regex("^DS\\{[a-z0-9]+(_[a-z0-9]+)*_L[1-4]_[0-9a-f]{6}\\}$")

    fun format(
        category: String,
        slug: String,
        level: Int,
        token: String,
    ): String = "DS{${category}_${slug}_L${level}_$token}"

    fun matchesFormat(candidate: String): Boolean = PATTERN.matches(candidate.trim())
}
