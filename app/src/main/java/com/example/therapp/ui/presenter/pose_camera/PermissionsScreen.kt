package com.example.therapp.ui.presenter.pose_camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import com.example.therapp.ui.navigation.routes.CameraRoutes

/**
 * @author Santiago Varela Daza
 * @email svarela03@uan.edu.co
 * @github https://github.com/sanvarela03
 * @since 8/15/2025
 * @version 1.0
 */
@Composable
fun PermissionsScreen(navController: NavHostController) {
    val context = LocalContext.current

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            navController.navigate(CameraRoutes.Camera) {
                popUpTo(CameraRoutes.Permissions) { inclusive = true }
            }
        }
    }

    LaunchedEffect(Unit) {
        if (checkCameraPermission(context)) {
            navController.navigate(CameraRoutes.Camera) {
                popUpTo(CameraRoutes.Permissions) { inclusive = true }
            }
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
}

// Función para verificar si ya tenemos el permiso
private fun checkCameraPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED
}