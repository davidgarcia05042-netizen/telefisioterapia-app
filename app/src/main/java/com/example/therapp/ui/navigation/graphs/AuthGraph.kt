package com.example.therapp.ui.navigation.graphs

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.example.therapp.common.UserAuthState
import com.example.therapp.ui.navigation.routes.AuthRoutes
import com.example.therapp.ui.presenter.home.HomeScreen
import com.example.therapp.ui.presenter.sign_in.SignInScreen
import com.example.therapp.ui.presenter.splash.SplashScreen

/**
 * @author Santiago Varela Daza
 * @email svarela03@uan.edu.co
 * @github https://github.com/sanvarela03
 * @since 8/31/2025
 * @version 1.0
 */
@RequiresApi(Build.VERSION_CODES.O)
fun NavGraphBuilder.authGraph(
    userAuthState: UserAuthState,
    navController: NavHostController,
) {
    navigation<Graph.Auth>(
        startDestination =
        when (userAuthState) {
            UserAuthState.UNKNOWN -> {
                AuthRoutes.SplashScreen
            }

            UserAuthState.UNAUTHENTICATED -> {
                AuthRoutes.Login
            }

            UserAuthState.AUTHENTICATED -> {
                AuthRoutes.Home
            }
        }

    ) {
        composable<AuthRoutes.Login> {
            SignInScreen()
        }
        composable<AuthRoutes.SplashScreen> {
            SplashScreen(navController)
        }
        composable<AuthRoutes.Home> {
            HomeScreen()
        }
    }
}