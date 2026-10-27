package com.example.therapp.ui.navigation.routes

import kotlinx.serialization.Serializable


/**
 * @author Santiago Varela Daza
 * @email svarela03@uan.edu.co
 * @github https://github.com/sanvarela03
 * @since 8/15/2025
 * @version 1.0
 */
@Serializable
sealed class CameraRoutes {
    @Serializable
    object Permissions : CameraRoutes()

    @Serializable
    object Camera : CameraRoutes()

    @Serializable
    object Gallery : CameraRoutes()
}