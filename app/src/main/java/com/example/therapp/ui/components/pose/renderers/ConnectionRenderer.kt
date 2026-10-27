package com.example.therapp.ui.components.pose.renderers

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.therapp.ui.components.pose.transformers.CoordinateTransformer
import com.google.mediapipe.tasks.components.containers.Connection
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark


/**
 * @author Santiago Varela Daza
 * @email svarela03@uan.edu.co
 * @github https://github.com/sanvarela03
 * @since 10/17/2025
 * @version 1.0
 */
class ConnectionRenderer(
    private val landmarks: List<NormalizedLandmark>,
    private val connections: List<Connection>,
    private val transformer: CoordinateTransformer,
    private val lineColor: Color,
    private val strokePx: Float
) : PoseRenderer {
    override fun DrawScope.render() {
        connections.forEach { connection ->
            val start = landmarks[connection.start()]
            val end = landmarks[connection.end()]

            drawLine(
                color = lineColor,
                start = transformer.transformOffset(start),
                end = transformer.transformOffset(end),
                strokeWidth = strokePx
            )
        }
    }
}