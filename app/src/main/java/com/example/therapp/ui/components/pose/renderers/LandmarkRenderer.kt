package com.example.therapp.ui.components.pose.renderers


import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.therapp.ui.components.pose.models.Joint
import com.example.therapp.ui.components.pose.transformers.CoordinateTransformer
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark


/**
 * @author Santiago Varela Daza
 * @email svarela03@uan.edu.co
 * @github https://github.com/sanvarela03
 * @since 10/17/2025
 * @version 1.0
 */
class LandmarkRenderer(
    private val landmarks: List<NormalizedLandmark>,
    private val transformer: CoordinateTransformer,
    private val trackedPoints: List<Joint>,
    private val pointColor: Color,
    private val radiusPx: Float
) : PoseRenderer {
    override fun DrawScope.render() {
        landmarks.forEachIndexed { index, landmark ->
            val coords = transformer.transformOffset(landmark)
            val isTracked = trackedPoints.any { it.id == index }
            val color = if (isTracked) Color.Red else pointColor

            drawCircle(
                color = color,
                radius = radiusPx * 2,
                center = coords
            )
        }
    }
}