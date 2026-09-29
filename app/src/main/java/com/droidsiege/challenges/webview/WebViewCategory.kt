package com.droidsiege.challenges.webview

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Web
import com.droidsiege.core.CategoryContributor
import com.droidsiege.core.CategoryMeta
import com.droidsiege.core.Challenge

object WebViewCategory : CategoryContributor {
    override val category: CategoryMeta = CategoryMeta(
        id = "webview",
        title = "WebView & Input",
        owasp = "M4",
        icon = Icons.Outlined.Web,
    )

    override val challenges: List<Challenge> = listOf(
        JsBridgeL1Challenge(),
        JsBridgeL2Challenge(),
        JsBridgeL3Challenge(),
        JsBridgeL4Challenge(),
        FileAccessL1Challenge(),
        FileAccessL2Challenge(),
        FileAccessL3Challenge(),
        FileAccessL4Challenge(),
        XssL1Challenge(),
        XssL2Challenge(),
        XssL3Challenge(),
        XssL4Challenge(),
        InjectL1Challenge(),
        InjectL2Challenge(),
        InjectL3Challenge(),
        InjectL4Challenge(),
    )
}
