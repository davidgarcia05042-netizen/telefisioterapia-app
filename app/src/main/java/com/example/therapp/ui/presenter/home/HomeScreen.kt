package com.example.therapp.ui.presenter.home

import android.content.res.Configuration
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.therapp.ui.components.AppToolBar
import com.example.therapp.ui.components.HomeBottomBar
import com.example.therapp.ui.components.NavigationDrawerBody
import com.example.therapp.ui.components.NavigationDrawerHeader
import com.example.therapp.ui.navigation.graphs.Graph
import com.example.therapp.ui.navigation.graphs.homeGraph
import com.example.therapp.ui.navigation.routes.DrawerRoutes
import com.example.therapp.ui.navigation.routes.HomeRoutes
import kotlinx.coroutines.launch

/**
 * @author Santiago Varela Daza
 * @email svarela03@uan.edu.co
 * @github https://github.com/sanvarela03
 * @since 9/3/2025
 * @version 1.0
 */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel()
) {
    val user by viewModel.user.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val homeNavController = rememberNavController()

    val navBackStackEntry = homeNavController.currentBackStackEntryAsState().value
    val currentDestination = navBackStackEntry?.destination

    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT

    val screens = listOf(
        HomeRoutes.RoutinesScreen,
        DrawerRoutes.ChangePasswordScreen,
    )
    val showTopBar = screens.any { destination ->
        currentDestination?.hierarchy?.any {
            it.hasRoute(destination::class)
        } == true
    } && isPortrait


    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = showTopBar,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(200.dp)
            ) {
                NavigationDrawerHeader(
                    value = listOfNotNull(user?.firstName, user?.lastName).joinToString(" "),
                    name = user?.email ?: ""
                )
                NavigationDrawerBody(DrawerNavigation.entries, navigateTo = {
                    scope.launch { drawerState.close() }
                    homeNavController.navigate(it) {
                        popUpTo(homeNavController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                })
            }
        }
    ) {
        Scaffold(
            topBar = {
                if (showTopBar) {
                    AppToolBar(
                        toolbarTitle = "TherApp",
                        signOutButtonClicked = {
                            viewModel.signOut()
                        },
                        navButtonClicked = {
                            scope.launch {
                                if (drawerState.isClosed) drawerState.open() else drawerState.close()
                            }
                        }
                    )
                }
            },
            bottomBar = {
                HomeBottomBar(
                    navigationItems = BottomNavigation.entries,
                    currentDestination = currentDestination,
                    navigateTo = {
                        homeNavController.navigate(it) {
                            popUpTo(homeNavController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            restoreState = true
                            launchSingleTop = true
                        }
                    }
                )
            },
        ) { contentPadding ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
                    .background(Color(0xFF3B4048)),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            ) {
                NavHost(
                    navController = homeNavController,
                    startDestination = Graph.Home,
                    modifier = Modifier.padding(16.dp)
                ) {
                    homeGraph(navController = homeNavController)
                }
            }
        }
    }

}
