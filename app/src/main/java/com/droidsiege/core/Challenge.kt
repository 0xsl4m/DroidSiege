package com.droidsiege.core

import androidx.compose.runtime.Composable

interface Challenge {
    val id: ChallengeId
    val title: String
    val brief: String
    val owaspRefs: List<String>
    val hints: List<String>
    val flag: String
    val learn: LearnContent

    fun validateFlag(input: String): Boolean = input.trim() == flag

    @Composable
    fun Screen(secureMode: Boolean)
}
