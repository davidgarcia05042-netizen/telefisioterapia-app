package com.example.therapp.ui.components.pose.builders

import com.example.therapp.ui.components.pose.models.ArcData
import com.example.therapp.ui.components.pose.transformers.CoordinateTransformer
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import java.lang.Math.toDegrees
import kotlin.math.atan2


/**
 * @author Santiago Varela Daza
 * @email svarela03@uan.edu.co
 * @github https://github.com/sanvarela03
 * @since 10/17/2025
 * @version 1.0
 */
class ArcDataBuilder(
    private val transformer: CoordinateTransformer,
    private val radius: Float = 25f
) {
    fun build(p1: NormalizedLandmark, center: NormalizedLandmark, p2: NormalizedLandmark): ArcData {
        val p1Coords = transformer.transform(p1)
        val centerCoords = transformer.transform(center)
        val p2Coords = transformer.transform(p2)

        val p1Angle = toDegrees(atan2(p1Coords.y - centerCoords.y, p1Coords.x - centerCoords.x).toDouble()).toFloat()
        val p2Angle = toDegrees(atan2(p2Coords.y - centerCoords.y, p2Coords.x - centerCoords.x).toDouble()).toFloat()

        var sweepAngle = p2Angle - p1Angle
        if (sweepAngle > 180f) sweepAngle -= 360f
        if (sweepAngle < -180f) sweepAngle += 360f - sweepAngle

        return ArcData(
            centerX = centerCoords.x,
            centerY = centerCoords.y,
            startAngle = p1Angle,
            sweepAngle = sweepAngle,
            radius = radius
        )
    }
}