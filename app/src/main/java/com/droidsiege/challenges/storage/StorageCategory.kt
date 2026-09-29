package com.droidsiege.challenges.storage

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderOff
import com.droidsiege.core.CategoryContributor
import com.droidsiege.core.CategoryMeta
import com.droidsiege.core.Challenge

object StorageCategory : CategoryContributor {
    override val category: CategoryMeta = CategoryMeta(
        id = "storage",
        title = "Insecure Storage",
        owasp = "M9",
        icon = Icons.Outlined.FolderOff,
    )

    override val challenges: List<Challenge> = listOf(
        PrefsL1Challenge(),
        PrefsL2Challenge(),
        PrefsL3Challenge(),
        PrefsL4Challenge(),
        SqliteL1Challenge(),
        SqliteL2Challenge(),
        SqliteL3Challenge(),
        SqliteL4Challenge(),
        ExtStorageL1Challenge(),
        ExtStorageL2Challenge(),
        ExtStorageL3Challenge(),
        ExtStorageL4Challenge(),
        LogsL1Challenge(),
        LogsL2Challenge(),
        LogsL3Challenge(),
        LogsL4Challenge(),
        BackupL1Challenge(),
        BackupL2Challenge(),
        BackupL3Challenge(),
        BackupL4Challenge(),
        ClipboardL1Challenge(),
        ClipboardL2Challenge(),
        ClipboardL3Challenge(),
        ClipboardL4Challenge(),
        ScreensL1Challenge(),
        ScreensL2Challenge(),
        ScreensL3Challenge(),
        ScreensL4Challenge(),
    )
}
