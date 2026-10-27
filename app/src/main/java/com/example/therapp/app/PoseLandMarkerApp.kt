package com.example.therapp.app

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.therapp.common.UserAuthState
import com.example.therapp.ui.global_viewmodels.AuthViewModel
import com.example.therapp.ui.navigation.graphs.Graph
import com.example.therapp.ui.navigation.graphs.authGraph
import com.example.therapp.ui.navigation.routes.CameraRoutes
import com.example.therapp.ui.presenter.pose_camera.CameraScreen
import com.example.therapp.ui.presenter.pose_camera.MainViewModel
import com.example.therapp.ui.presenter.pose_camera.PermissionsScreen
import com.example.therapp.ui.presenter.splash.SplashEvent

/**
 * @author Santiago Varela Daza
 * @email svarela03@uan.edu.co
 * @github https://github.com/sanvarela03
 * @since 8/15/2025
 * @version 1.0
 */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun PoseLandMarkerApp(
    viewModel: MainViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val userState = authViewModel.isAuthenticated.collectAsState().value

    LaunchedEffect(userState) {
        authViewModel.onEvent(SplashEvent.CheckAuthentication)
    }

    Log.d("UserState", "UserState: $userState")

    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Graph.Auth
    ) {
        authGraph(
            userAuthState = userState,
            navController = navController
        )
        composable<CameraRoutes.Permissions> {
            PermissionsScreen(
                navController = navController
            )
        }
        composable<CameraRoutes.Camera> {
            CameraScreen(
                navController = navController,
                viewModel = viewModel
            )
        }
        composable<CameraRoutes.Gallery> {
//            GalleryScreen(
//                navController = navController,
//                viewModel = viewModel
//            )
            Column {
                Text("Gallery")
            }
        }
    }
}