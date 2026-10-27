package com.example.therapp.ui.components.pose.calculators

import com.example.therapp.ui.components.pose.models.Joint


/**
 * @author Santiago Varela Daza
 * @email svarela03@uan.edu.co
 * @github https://github.com/sanvarela03
 * @since 11/26/2025
 * @version 1.0
 */
class RepetitionCounter {
    private val jointStates = mutableMapOf<Joint, Boolean>()
    private val repetitionCounts = mutableMapOf<Joint, Int>()

    /**
     * Actualiza el contador de repeticiones para una articulación específica basado en el ángulo actual.
     * @param joint La articulación que se está evaluando.
     * @param angle El ángulo actual de la articulación.
     */
    fun updateRepetition(joint: Joint, angle: Float) {
        val currentState = jointStates.getOrPut(joint) { true }
        val currentCount = repetitionCounts.getOrPut(joint) { 0 }

        if (currentState) {
            if (angle >= joint.maxAngle) {
                jointStates[joint] = false
            }
        } else {
            if (angle <= joint.minAngle) {
                repetitionCounts[joint] = currentCount + 1
                jointStates[joint] = true
            }
        }
    }

    /**
     * Devuelve el número de repeticiones para una articulación dada.
     * @param joint La articulación de la que se quiere obtener el conteo.
     * @return El número de repeticiones.
     */
    fun getRepetitionsFor(joint: Joint): Int {
        return repetitionCounts.getOrDefault(joint, 0)
    }
}