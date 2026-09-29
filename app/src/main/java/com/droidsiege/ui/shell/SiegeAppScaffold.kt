package com.droidsiege.ui.shell

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.droidsiege.R
import com.droidsiege.ui.components.ScoreBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SiegeAppScaffold(
    module: SiegeModule,
    onMenuClick: () -> Unit,
    totalScore: Int,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(module.label) },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = stringResource(R.string.cd_open_menu),
                        )
                    }
                },
                actions = {
                    ScoreBadge(score = totalScore, modifier = Modifier.padding(end = 16.dp))
                },
            )
        },
        content = content,
    )
}

@Composable
fun SiegeAppDrawer(
    currentRoute: String?,
    onDestinationClick: (SiegeModule) -> Unit,
) {
    ModalDrawerSheet {
        Text(
            text = stringResource(R.string.drawer_brand),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(start = 28.dp, top = 24.dp, bottom = 12.dp),
        )
        SiegeModule.topLevel.forEach { module ->
            NavigationDrawerItem(
                label = { Text(module.label) },
                icon = {
                    Icon(imageVector = module.icon, contentDescription = null)
                },
                selected = module.route == currentRoute,
                onClick = { onDestinationClick(module) },
            )
        }
    }
}
