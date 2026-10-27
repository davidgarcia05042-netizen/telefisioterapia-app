package com.example.therapp.ui.navigation.graphs

import kotlinx.serialization.Serializable


/**
 * @author Santiago Varela Daza
 * @email svarela03@uan.edu.co
 * @github https://github.com/sanvarela03
 * @since 8/31/2025
 * @version 1.0
 */
@Serializable
sealed class Graph {
    @Serializable
    data object Auth : Graph()

    @Serializable
    data object Home : Graph()
}