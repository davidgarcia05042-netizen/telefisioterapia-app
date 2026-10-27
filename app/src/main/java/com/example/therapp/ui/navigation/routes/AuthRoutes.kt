package com.example.therapp.ui.navigation.routes

import kotlinx.serialization.Serializable


/**
 * @author Santiago Varela Daza
 * @email svarela03@uan.edu.co
 * @github https://github.com/sanvarela03
 * @since 8/31/2025
 * @version 1.0
 */
@Serializable
sealed class AuthRoutes {
    @Serializable
    object Login : AuthRoutes()

    @Serializable
    object SplashScreen : AuthRoutes()

    @Serializable
    object Register : AuthRoutes()

    @Serializable
    object ForgotPassword : AuthRoutes()

    @Serializable
    object Home : AuthRoutes()
}