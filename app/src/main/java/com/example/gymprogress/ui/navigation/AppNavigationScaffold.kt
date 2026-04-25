package com.example.gymprogress.ui.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import com.example.gymprogress.AppDestinations
import com.example.gymprogress.ui.theme.ComponentSize
import com.example.gymprogress.ui.theme.Spacing
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigationScaffold(
    currentDestination: AppDestinations,
    onDestinationChange: (AppDestinations) -> Unit,
    moreMenuExpanded: Boolean,
    onMoreMenuDismiss: () -> Unit,
    onMoreMenuToggle: () -> Unit,
    onOpenTrainerSettings: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAbout: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (AppDestinations) -> Unit,
) {
    NavigationSuiteScaffold(
        modifier = modifier,
        navigationSuiteItems = {
            AppDestinations.entries.forEach { destination ->
                item(
                    icon = {
                        Icon(
                            destination.icon,
                            contentDescription = destination.label
                        )
                    },
                    label = {
                        Text(
                            destination.label,
                            fontWeight = if (destination == currentDestination) FontWeight.Bold
                            else FontWeight.Normal
                        )
                    },
                    selected = destination == currentDestination,
                    onClick = { onDestinationChange(destination) }
                )
            }
            item(
                icon = {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Ещё"
                    )
                },
                label = {
                    Text(
                        "Ещё",
                        fontWeight = FontWeight.Normal
                    )
                },
                selected = false,
                onClick = onMoreMenuToggle
            )
        },
        navigationSuiteColors = NavigationSuiteDefaults.colors(
            navigationBarContainerColor = MaterialTheme.colorScheme.surface,
            navigationBarContentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        content(currentDestination)
    }

    if (moreMenuExpanded) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val scope = rememberCoroutineScope()

        fun dismissThen(action: () -> Unit) {
            scope.launch {
                sheetState.hide()
                onMoreMenuDismiss()
                action()
            }
        }

        ModalBottomSheet(
            onDismissRequest = onMoreMenuDismiss,
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(vertical = Spacing.xs)
            ) {
                MoreSheetItem(
                    icon = Icons.Default.Tune,
                    label = "Настройки тренера",
                    onClick = { dismissThen(onOpenTrainerSettings) }
                )
                MoreSheetItem(
                    icon = Icons.Outlined.History,
                    label = "История тренировок",
                    onClick = { dismissThen(onOpenHistory) }
                )
                MoreSheetItem(
                    icon = Icons.Default.Settings,
                    label = "Настройки",
                    onClick = { dismissThen(onOpenSettings) }
                )
                MoreSheetItem(
                    icon = Icons.Default.Info,
                    label = "О приложении",
                    onClick = { dismissThen(onOpenAbout) }
                )
            }
        }
    }
}

@Composable
private fun MoreSheetItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Spacing.huge) // 56dp — стандартный тач-таргет
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(ComponentSize.iconMd)
        )
        Spacer(modifier = Modifier.width(Spacing.md))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
