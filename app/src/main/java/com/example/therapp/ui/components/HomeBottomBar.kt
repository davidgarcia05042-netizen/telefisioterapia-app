package com.example.therapp.ui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import com.example.therapp.ui.navigation.routes.HomeRoutes
import com.example.therapp.ui.presenter.home.BottomNavigation

@Composable
fun HomeBottomBar(
    navigationItems: List<BottomNavigation>,
    currentDestination: NavDestination?,
    navigateTo: (HomeRoutes) -> Unit
) {
    NavigationBar(
        modifier = Modifier
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
            )
            .height(70.dp),
    ) {
        navigationItems.forEach { navItem: BottomNavigation ->
            val selected = currentDestination?.hierarchy?.any {
                it.hasRoute(navItem.route::class)
            } == true

            NavigationBarItem(
                selected = selected,
                onClick = { navigateTo(navItem.route) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF007FA2),
                    unselectedIconColor = Color.Gray,
                    indicatorColor = Color.Transparent
                ),
                icon = {
                    val icon = if (selected) {
                        navItem.selectedIcon
                    } else {
                        navItem.unselectedIcon
                    }

                    if (icon != null) {
                        Icon(
                            imageVector = icon,
                            modifier = Modifier.size(30.dp),
                            contentDescription = "",
                        )
                    }
                },
                label = {
                    Text(
                        text = navItem.title,
                        maxLines = 1,
                        fontWeight = FontWeight.Light,
                        fontSize = 10.sp
                    )
                }
            )
        }
    }
}