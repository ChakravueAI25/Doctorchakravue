package com.org.doctorchakravue.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.People
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.org.doctorchakravue.core.ui.theme.DoctorGreen

/**
 * Represents a bottom navigation item.
 */
data class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

/**
 * Bottom navigation bar for the Doctor app.
 * Stateless composable that accepts current screen and navigation callback.
 */
@Composable
fun BottomNavBar(
    currentScreen: String,
    onNavigate: (String) -> Unit
) {
    val items = listOf(
        BottomNavItem(
            route = "dashboard",
            label = "Home",
            selectedIcon = Icons.Filled.Home,
            unselectedIcon = Icons.Outlined.Home
        ),
        BottomNavItem(
            route = "patients",
            label = "Patients",
            selectedIcon = Icons.Filled.People,
            unselectedIcon = Icons.Outlined.People
        ),
        BottomNavItem(
            route = "pain_scale_history",
            label = "Reviews",
            selectedIcon = Icons.Filled.Home, // Will use a custom icon or emoji
            unselectedIcon = Icons.Outlined.Home
        ),
        BottomNavItem(
            route = "notifications",
            label = "Notify",
            selectedIcon = Icons.Filled.Notifications,
            unselectedIcon = Icons.Outlined.Notifications
        )
    )

    NavigationBar(containerColor = Color.White) {
        items.forEach { item ->
            val isSelected = currentScreen == item.route

            NavigationBarItem(
                icon = {
                    when (item.route) {
                        "pain_scale_history" -> Text(if (isSelected) "📊" else "📈")
                        else -> Icon(
                            imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                            contentDescription = item.label
                        )
                    }
                },
                label = { Text(item.label) },
                selected = isSelected,
                onClick = {
                    if (!isSelected) {
                        onNavigate(item.route)
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = DoctorGreen,
                    selectedTextColor = DoctorGreen,
                    indicatorColor = DoctorGreen.copy(alpha = 0.1f),
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray
                )
            )
        }
    }
}
