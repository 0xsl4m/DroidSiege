package com.droidsiege.core

data class LearnContent(
    val theory: String,
    val mastgRefs: List<String>,
    val vulnerableSnippet: String,
    val fixSnippet: String,
    val takeaway: String,
)
