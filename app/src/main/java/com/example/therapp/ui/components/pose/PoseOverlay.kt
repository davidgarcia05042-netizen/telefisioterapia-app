package com.example.therapp.ui.components.pose

/**
 * @author Santiago Varela Daza
 * @email svarela03@uan.edu.co
 * @github https://github.com/sanvarela03
 * @since 8/15/2025
 * @version 1.0
 */
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.therapp.ui.components.pose.calculators.RepetitionCounter
import com.example.therapp.ui.components.pose.models.Joint
import com.example.therapp.ui.components.pose.processors.PoseDataProcessor
import com.example.therapp.ui.components.pose.factories.PoseRendererFactory
import com.example.therapp.ui.components.pose.strategies.ScaleStrategyFactory
import com.example.therapp.ui.components.pose.transformers.CoordinateTransformer
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult

private const val TAG = "PoseOverlay"
private const val LANDMARK_STROKE_DP = 3f

/**
 * This composable is used to draw the pose landmarks on the screen.
 * It takes the results from the PoseLandmarker and draws the landmarks and connections on a Canvas.
 */
@Composable
fun PoseOverlay(
    modifier: Modifier = Modifier,
    runningMode: RunningMode = RunningMode.LIVE_STREAM,
    imageWidth: Int,
    imageHeight: Int,
    pointColor: Color = Color.Cyan,
    lineColor: Color = Color.LightGray,
    results: PoseLandmarkerResult?,
    trackedPoints: List<Joint> = emptyList(),
    showAngle: Boolean = true,
    showRepetitions: Boolean = true
) {
    val density = LocalDensity.current
    val strokePx = remember { with(density) { LANDMARK_STROKE_DP.dp.toPx() } }
    val radiusPx = strokePx / 2f
    val repetitionCounter = remember { RepetitionCounter() }

    Canvas(modifier = modifier) {
        val paint = Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 40f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val repPaint = Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 30f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        if (results == null) return@Canvas

        // Escalado automático según el modo
        val scaleStrategy = ScaleStrategyFactory.create(runningMode)
        val scaleFactor = scaleStrategy.calculate(size.width, size.height, imageWidth, imageHeight)

        val transformer = CoordinateTransformer(imageWidth, imageHeight, scaleFactor)
        val processor = PoseDataProcessor(transformer, repetitionCounter = repetitionCounter)

        val rendererFactory = PoseRendererFactory(
            transformer,
            pointColor,
            lineColor,
            strokePx,
            radiusPx,
            paint,
            repPaint = repPaint
        )
        results.landmarks().forEach { personLandmarks ->
            val renderer = rendererFactory.createRenderer(
                personLandmarks,
                trackedPoints,
                processor,
                showAngle,
                showRepetitions
            )
            with(renderer) { render() }
        }
    }
}
