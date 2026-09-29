package com.droidsiege.ui.navigation

import android.net.Uri

object Routes {
    const val CATEGORY = "category/{categoryId}"
    const val CHALLENGE = "challenge?idKey={idKey}"

    fun category(categoryId: String): String = "category/$categoryId"

    fun challenge(idKey: String): String = "challenge?idKey=${Uri.encode(idKey)}"
}
