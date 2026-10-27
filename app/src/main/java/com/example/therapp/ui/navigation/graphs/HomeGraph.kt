package com.example.therapp.ui.navigation.graphs

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import androidx.navigation.navigation
import com.example.therapp.ui.navigation.routes.DrawerRoutes
import com.example.therapp.ui.navigation.routes.HomeRoutes
import com.example.therapp.ui.presenter.change_password.ChangePasswordScreen
import com.example.therapp.ui.presenter.pose_camera.CameraScreen
import com.example.therapp.ui.presenter.pose_camera.ExerciseScreen
import com.example.therapp.ui.presenter.routines.RoutinesScreen
import com.example.therapp.ui.presenter.routines.detail.RoutineDetailScreen

/**
 * @author Santiago Varela Daza
 * @email svarela03@uan.edu.co
 * @github https://github.com/sanvarela03
 * @since 9/4/2025
 * @version 1.0
 */
@RequiresApi(Build.VERSION_CODES.O)
fun NavGraphBuilder.homeGraph(navController: NavController) {
    navigation<Graph.Home>(startDestination = HomeRoutes.RoutinesScreen) {
        composable<HomeRoutes.RoutinesScreen> {
            RoutinesScreen(
                onRoutineClick = { scheduledTrainingId ->
                    navController.navigate(HomeRoutes.RoutineDetailScreen(scheduledTrainingId))
                }
            )
        }
        
        composable<HomeRoutes.RoutineDetailScreen> {
            RoutineDetailScreen(
                onDoExercise = { scheduledTrainingId, exercise ->
                    navController.navigate(
                        HomeRoutes.ExerciseCamera(scheduledTrainingId, exercise.id, exercise.name)
                    )
                }
            )
        }

        composable<HomeRoutes.ExerciseCamera> { entry ->
            val target = entry.toRoute<HomeRoutes.ExerciseCamera>()
            ExerciseScreen(
                exerciseName = target.exerciseName,
                navController = navController
            )
        }

        composable<HomeRoutes.CameraScreen> {
            CameraScreen(navController)
        }
        composable<DrawerRoutes.ChangePasswordScreen> {
            ChangePasswordScreen()
        }
    }
}