package com.example.therapp.ui.presenter.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SportsGymnastics
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.therapp.common.UserAuthState

@Composable
fun SplashScreen(
    navController: NavController
) {
    // Sin popBackStack(): la navegación ya cambia a Login o Inicio cuando se
    // sabe si hay sesión (authGraph). El pop a los 2 s podía caer mientras la
    // pantalla de inicio todavía salía con su animación y sacar la pantalla
    // NUEVA: la app quedaba en blanco en un arranque lento (pasó dos veces en
    // el emulador, justo después de instalar).
    Splash()
}

@Composable
fun Splash() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            imageVector = Icons.Default.SportsGymnastics,
            contentDescription = "Logo app",
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(120.dp)
        )
    }
}


@Composable
@Preview(showBackground = true)
fun SplashScreenPreview() {
    Splash()
}