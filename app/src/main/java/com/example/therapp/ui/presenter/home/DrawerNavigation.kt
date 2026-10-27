package com.example.therapp.ui.presenter.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.therapp.ui.navigation.routes.DrawerRoutes

/**
 * @author Santiago Varela Daza
 * @email svarela03@uan.edu.co
 * @github https://github.com/sanvarela03
 * @since 8/5/2025
 * @version 1.0
 */
// "Mi Perfil" y "Ajustes" eran pantallas de relleno del original: el nombre y
// el correo del paciente ya están en la cabecera del menú.
enum class DrawerNavigation(
    val title: String,
    val description: String,
    val route: DrawerRoutes,
    val icon: ImageVector,
    val selectedIcon: ImageVector?,
    val unselectedIcon: ImageVector?,
) {
    CHANGE_PASSWORD(
        title = "Cambiar contraseña",
        description = "Cambiar contraseña",
        route = DrawerRoutes.ChangePasswordScreen,
        icon = Icons.Outlined.Lock,
        selectedIcon = Icons.Filled.Lock,
        unselectedIcon = Icons.Outlined.Lock,
    ),
}