package com.example.therapp.ui.components.pose.models

/**
 * @author Santiago Varela Daza
 * @email svarela03@uan.edu.co
 * @github https://github.com/sanvarela03
 * @since 10/3/2025
 * @version 1.0
 */

// Índices de los puntos del cuerpo en MediaPipe Pose (33 puntos). Antes venían
// de com.google.mlkit.vision.pose.PoseLandmark, una dependencia de ML Kit que
// solo se usaba por estas constantes; los valores son los mismos.
object PoseLandmark {
    const val LEFT_SHOULDER = 11
    const val RIGHT_SHOULDER = 12
    const val LEFT_ELBOW = 13
    const val RIGHT_ELBOW = 14
    const val LEFT_WRIST = 15
    const val RIGHT_WRIST = 16
    const val LEFT_HIP = 23
    const val RIGHT_HIP = 24
    const val LEFT_KNEE = 25
    const val RIGHT_KNEE = 26
    const val LEFT_ANKLE = 27
    const val RIGHT_ANKLE = 28
}

enum class Joint(val id: Int, val jointName: String, val minAngle: Double, val maxAngle: Double) {
    LEFT_ELBOW(PoseLandmark.LEFT_ELBOW, minAngle = 50.0, maxAngle = 165.0 , jointName ="Codo Izquierdo"),
    RIGHT_ELBOW(PoseLandmark.RIGHT_ELBOW, minAngle = 50.0, maxAngle = 165.0, jointName = "Codo Derecho"),
    LEFT_KNEE(PoseLandmark.LEFT_KNEE, minAngle = 0.0, maxAngle = 180.0, jointName = "Rodilla Izquierda"),
    RIGHT_KNEE(PoseLandmark.RIGHT_KNEE, minAngle = 0.0, maxAngle = 180.0, jointName = "Rodilla Derecha"),
    LEFT_SHOULDER(PoseLandmark.LEFT_SHOULDER, minAngle = 0.0, maxAngle = 180.0, jointName = "Hombro Izquierdo"),
    RIGHT_SHOULDER(PoseLandmark.RIGHT_SHOULDER, minAngle = 0.0, maxAngle = 180.0, jointName = "Hombro Derecho"),
    LEFT_HIP(PoseLandmark.LEFT_HIP, minAngle = 0.0, maxAngle = 180.0, jointName = "Cadera Izquierda"),
    RIGHT_HIP(PoseLandmark.RIGHT_HIP, minAngle = 0.0, maxAngle = 180.0, jointName = "Cadera Derecha")
}

// Definimos qué puntos forman cada ángulo
val jointDependencies = mapOf(
    Joint.LEFT_ELBOW to Pair(PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_WRIST),
    Joint.RIGHT_ELBOW to Pair(PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_WRIST),
    Joint.LEFT_KNEE to Pair(PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_ANKLE),
    Joint.RIGHT_KNEE to Pair(PoseLandmark.RIGHT_HIP, PoseLandmark.RIGHT_ANKLE),
    Joint.LEFT_SHOULDER to Pair(PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_ELBOW),
    Joint.RIGHT_SHOULDER to Pair(PoseLandmark.RIGHT_HIP, PoseLandmark.RIGHT_ELBOW),
    Joint.LEFT_HIP to Pair(PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_KNEE),
    Joint.RIGHT_HIP to Pair(PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_KNEE)
)