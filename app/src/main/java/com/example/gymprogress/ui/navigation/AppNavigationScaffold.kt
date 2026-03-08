package com.example.gymprogress.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.example.gymprogress.AppDestinations

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
                    Box {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Ещё"
                        )
                        DropdownMenu(
                            expanded = moreMenuExpanded,
                            onDismissRequest = onMoreMenuDismiss
                        ) {
                            DropdownMenuItem(
                                text = { Text("Настройки тренера") },
                                onClick = {
                                    onMoreMenuDismiss()
                                    onOpenTrainerSettings()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("История тренировок") },
                                onClick = {
                                    onMoreMenuDismiss()
                                    onOpenHistory()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Настройки") },
                                onClick = {
                                    onMoreMenuDismiss()
                                    onOpenSettings()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("О приложении") },
                                onClick = {
                                    onMoreMenuDismiss()
                                    onOpenAbout()
                                }
                            )
                        }
                    }
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
}
