package com.example.therapp.ui.components.pose.models

import androidx.compose.ui.geometry.Offset


/**
 * @author Santiago Varela Daza
 * @email svarela03@uan.edu.co
 * @github https://github.com/sanvarela03
 * @since 11/26/2025
 * @version 1.0
 */
data class RepetitionData(
    val count: Int,
    val position: Offset // Posición donde dibujar el texto del contador
)