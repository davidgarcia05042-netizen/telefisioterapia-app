package com.example.therapp.ui.components.pose.transformers

import androidx.compose.ui.geometry.Offset
import com.example.therapp.ui.components.pose.models.ScaledCoordinates
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark


/**
 * @author Santiago Varela Daza
 * @email svarela03@uan.edu.co
 * @github https://github.com/sanvarela03
 * @since 10/17/2025
 * @version 1.0
 */
class CoordinateTransformer(
    private val imageWidth: Int,
    private val imageHeight: Int,
    private val scaleFactor: Float
) {
    fun transform(landmark: NormalizedLandmark): ScaledCoordinates {
        return ScaledCoordinates(
            x = landmark.x() * imageWidth * scaleFactor,
            y = landmark.y() * imageHeight * scaleFactor
        )
    }

    fun transformOffset(landmark: NormalizedLandmark): Offset {
        val coords = transform(landmark)
        return Offset(coords.x, coords.y)
    }
}