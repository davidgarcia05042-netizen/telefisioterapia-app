package com.example.therapp.ui.presenter.pose_camera

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject


/**
 * @author Santiago Varela Daza
 * @email svarela03@uan.edu.co
 * @github https://github.com/sanvarela03
 * @since 8/15/2025
 * @version 1.0
 */
@HiltViewModel
class MainViewModel
@Inject constructor() : ViewModel() {

    // Delegado (CPU/GPU)
    private val _currentDelegate = MutableStateFlow(DELEGATE_CPU)
    val currentDelegate: StateFlow<Int> = _currentDelegate.asStateFlow()

    // Modelo (Full/Lite/Heavy)
    private val _currentModel = MutableStateFlow(MODEL_POSE_LANDMARKER_LITE)
    val currentModel: StateFlow<Int> = _currentModel.asStateFlow()

    // Confianza mínima para detección de poses
    private val _minPoseDetectionConfidence = MutableStateFlow(DEFAULT_POSE_DETECTION_CONFIDENCE)
    val currentMinPoseDetectionConfidence: StateFlow<Float> =
        _minPoseDetectionConfidence.asStateFlow()

    // Confianza mínima para tracking de poses
    private val _minPoseTrackingConfidence = MutableStateFlow(DEFAULT_POSE_TRACKING_CONFIDENCE)
    val currentMinPoseTrackingConfidence: StateFlow<Float> =
        _minPoseTrackingConfidence.asStateFlow()

    // Confianza mínima para presencia de poses
    private val _minPosePresenceConfidence = MutableStateFlow(DEFAULT_POSE_PRESENCE_CONFIDENCE)
    val currentMinPosePresenceConfidence: StateFlow<Float> =
        _minPosePresenceConfidence.asStateFlow()

    // Función para cambiar el delegado (CPU/GPU)
    fun setDelegate(delegate: Int) {
        _currentDelegate.value = delegate
    }

    // Función para cambiar el modelo
    fun setModel(model: Int) {
        _currentModel.value = model
    }

    // Función para cambiar la confianza mínima de detección
    fun setMinPoseDetectionConfidence(confidence: Float) {
        _minPoseDetectionConfidence.value = confidence.coerceIn(0.1f, 1.0f)  // ← CORREGIDO
    }

    // Función para cambiar la confianza mínima de tracking
    fun setMinPoseTrackingConfidence(confidence: Float) {
        _minPoseTrackingConfidence.value = confidence.coerceIn(0.1f, 1.0f)
    }

    // Función para cambiar la confianza mínima de presencia
    fun setMinPosePresenceConfidence(confidence: Float) {
        _minPosePresenceConfidence.value = confidence.coerceIn(0.1f, 1.0f)
    }

    // Función para obtener valores actuales como Int (para compatibilidad)
    fun getCurrentDelegateAsInt(): Int = _currentDelegate.value
    fun getCurrentModelAsInt(): Int = _currentModel.value
    fun getCurrentMinPoseDetectionConfidenceAsFloat(): Float = _minPoseDetectionConfidence.value
    fun getCurrentMinPoseTrackingConfidenceAsFloat(): Float = _minPoseTrackingConfidence.value
    fun getCurrentMinPosePresenceConfidenceAsFloat(): Float = _minPosePresenceConfidence.value

    companion object {
        // Delegados disponibles
        const val DELEGATE_CPU = 0
        const val DELEGATE_GPU = 1

        // Modelos disponibles
        const val MODEL_POSE_LANDMARKER_FULL = 0
        const val MODEL_POSE_LANDMARKER_LITE = 1
        const val MODEL_POSE_LANDMARKER_HEAVY = 2

        // Valores por defecto
        const val DEFAULT_POSE_DETECTION_CONFIDENCE = 0.5f
        const val DEFAULT_POSE_TRACKING_CONFIDENCE = 0.5f
        const val DEFAULT_POSE_PRESENCE_CONFIDENCE = 0.5f
    }
}
