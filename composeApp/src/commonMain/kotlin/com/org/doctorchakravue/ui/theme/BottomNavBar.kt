package com.org.doctorchakravue.ui.theme

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentTurnedIn
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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color

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
            route = "adherence",
            label = "Adherence",
            selectedIcon = Icons.Filled.AssignmentTurnedIn,
            unselectedIcon = Icons.Filled.AssignmentTurnedIn
        ),
        BottomNavItem(
            route = "notifications",
            label = "Notify",
            selectedIcon = Icons.Filled.Notifications,
            unselectedIcon = Icons.Outlined.Notifications
        )
    )

    NavigationBar(
        containerColor = Color(0xFF6A7FC0).copy(alpha = 0.95f),
        modifier = Modifier
            .height(90.dp)
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
    ) {
        items.forEachIndexed { index, item ->
            val isSelected = currentScreen == item.route

            // Scale animation - selected icon grows, unselected stay original
            val scale = animateFloatAsState(
                targetValue = if (isSelected) 1.5f else 1.0f,
                animationSpec = tween(durationMillis = 1000),
                label = "icon scale"
            )

            NavigationBarItem(
                icon = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.scale(scale.value)) {
                            Icon(
                                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.label,
                                modifier = Modifier.height(32.dp),
                                tint = if (isSelected) Color(0xFF334671) else Color.White
                            )
                        }
                        if (isSelected) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                item.label,
                                color = Color(0xFF334671),
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                label = null,
                selected = isSelected,
                onClick = {
                    if (!isSelected) {
                        onNavigate(item.route)
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color.White
                ),
                modifier = Modifier.padding(
                    start = if (index == 0) 8.dp else 0.dp,
                    end = if (index == items.size - 1) 8.dp else 0.dp
                )
            )
        }
    }
}
