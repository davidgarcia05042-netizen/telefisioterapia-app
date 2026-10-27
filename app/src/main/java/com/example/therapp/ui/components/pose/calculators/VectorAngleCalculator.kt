package com.example.therapp.ui.components.pose.calculators

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlin.math.abs
import kotlin.math.atan2


/**
 * @author Santiago Varela Daza
 * @email svarela03@uan.edu.co
 * @github https://github.com/sanvarela03
 * @since 10/17/2025
 * @version 1.0
 */
class VectorAngleCalculator : AngleCalculator {
    override fun calculate(p1: NormalizedLandmark, center: NormalizedLandmark, p2: NormalizedLandmark): Float {
        val abX = p1.x() - center.x()
        val abY = p1.y() - center.y()
        val cbX = p2.x() - center.x()
        val cbY = p2.y() - center.y()

        val angle1 = atan2(abY, abX)
        val angle2 = atan2(cbY, cbX)

        var angle = Math.toDegrees(abs(angle1 - angle2).toDouble()).toFloat()
        if (angle > 180) angle = 360 - angle
        return angle
    }
}