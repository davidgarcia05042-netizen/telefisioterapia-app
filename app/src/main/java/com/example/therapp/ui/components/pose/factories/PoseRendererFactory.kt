package com.example.therapp.ui.components.pose.factories

import android.graphics.Paint
import androidx.compose.ui.graphics.Color
import com.example.therapp.ui.components.pose.models.Joint
import com.example.therapp.ui.components.pose.processors.PoseDataProcessor
import com.example.therapp.ui.components.pose.renderers.AngleRenderer
import com.example.therapp.ui.components.pose.renderers.CompositePoseRenderer
import com.example.therapp.ui.components.pose.renderers.ConnectionRenderer
import com.example.therapp.ui.components.pose.renderers.LandmarkRenderer
import com.example.therapp.ui.components.pose.renderers.PoseRenderer
import com.example.therapp.ui.components.pose.transformers.CoordinateTransformer
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker


/**
 * @author Santiago Varela Daza
 * @email svarela03@uan.edu.co
 * @github https://github.com/sanvarela03
 * @since 10/17/2025
 * @version 1.0
 */
class PoseRendererFactory(
    private val transformer: CoordinateTransformer,
    private val pointColor: Color,
    private val lineColor: Color,
    private val strokePx: Float,
    private val radiusPx: Float,
    private val paint: Paint,
    private val repPaint: Paint
) {
    fun createRenderer(
        landmarks: List<NormalizedLandmark>,
        trackedPoints: List<Joint>,
        processor: PoseDataProcessor,
        showAngle: Boolean,
        showRepetitions: Boolean
    ): PoseRenderer {
        return CompositePoseRenderer(
            listOf(
                ConnectionRenderer(
                    landmarks,
                    PoseLandmarker.POSE_LANDMARKS.toList(),
                    transformer,
                    lineColor,
                    strokePx
                ),
                LandmarkRenderer(
                    landmarks,
                    transformer,
                    trackedPoints,
                    pointColor,
                    radiusPx
                ),
                AngleRenderer(
                    landmarks,
                    trackedPoints,
                    processor,
                    paint,
                    repPaint,
                    showAngle,
                    showRepetitions
                )
            )
        )
    }
}