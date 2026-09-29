package com.droidsiege.challenges.common

import android.content.Context

/** One interactive step of a challenge screen. [run] returns a status line to display. */
data class KitAction(
    val label: String,
    val run: (context: Context, secureMode: Boolean) -> String,
)
