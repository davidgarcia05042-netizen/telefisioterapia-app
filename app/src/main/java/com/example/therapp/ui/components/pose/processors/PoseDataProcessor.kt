package com.example.therapp.ui.components.pose.processors

import androidx.compose.ui.geometry.Offset
import com.example.therapp.ui.components.pose.models.Joint
import com.example.therapp.ui.components.pose.builders.ArcDataBuilder
import com.example.therapp.ui.components.pose.models.AngleData
import com.example.therapp.ui.components.pose.models.ScaledCoordinates
import com.example.therapp.ui.components.pose.calculators.AngleCalculator
import com.example.therapp.ui.components.pose.calculators.RepetitionCounter
import com.example.therapp.ui.components.pose.calculators.VectorAngleCalculator
import com.example.therapp.ui.components.pose.models.RepetitionData
import com.example.therapp.ui.components.pose.transformers.CoordinateTransformer
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark


/**
 * @author Santiago Varela Daza
 * @email svarela03@uan.edu.co
 * @github https://github.com/sanvarela03
 * @since 10/17/2025
 * @version 1.0
 */
class PoseDataProcessor(
    private val transformer: CoordinateTransformer,
    private val angleCalculator: AngleCalculator = VectorAngleCalculator(),
    private val arcBuilder: ArcDataBuilder = ArcDataBuilder(transformer),
    private val repetitionCounter: RepetitionCounter
) {
    fun processAngle(
        p1: NormalizedLandmark,
        center: NormalizedLandmark,
        p2: NormalizedLandmark
    ): AngleData {
        val angle = angleCalculator.calculate(p1, center, p2)
        val centerCoords = transformer.transform(center)
        val arcData = arcBuilder.build(p1, center, p2)

        return AngleData(
            degrees = angle,
            position = ScaledCoordinates(
                x = centerCoords.x + 20f,
                y = centerCoords.y - 20f
            ),
            arcData = arcData
        )
    }

    fun isTrackedJoint(jointId: Int, trackedPoints: List<Joint>): Boolean {
        return trackedPoints.any { it.id == jointId }
    }

    fun processAngleAndReps(
        joint: Joint, // Pasamos el `Joint` completo
        start: NormalizedLandmark,
        center: NormalizedLandmark,
        end: NormalizedLandmark
    ) : Pair<AngleData, RepetitionData>{
        val angleData = processAngle(start, center, end)

        repetitionCounter.updateRepetition(joint, angleData.degrees)
        val repCount = repetitionCounter.getRepetitionsFor(joint)
        val repTextPosition =
            Offset(angleData.position.x, angleData.position.y + 40f) // Ajusta el '40f'
        val repetitionData = RepetitionData(repCount, repTextPosition)

        return Pair(angleData, repetitionData)
    }


}