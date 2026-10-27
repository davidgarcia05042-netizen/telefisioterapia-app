package com.example.therapp.ui.components.pose.calculators

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark

/**
 * @author Santiago Varela Daza
 * @email svarela03@uan.edu.co
 * @github https://github.com/sanvarela03
 * @since 10/17/2025
 * @version 1.0
 */
interface AngleCalculator {
    fun calculate(p1: NormalizedLandmark, center: NormalizedLandmark, p2: NormalizedLandmark): Float
}