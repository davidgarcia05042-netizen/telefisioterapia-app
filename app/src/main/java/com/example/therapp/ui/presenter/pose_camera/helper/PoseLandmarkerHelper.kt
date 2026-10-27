package com.example.therapp.ui.presenter.pose_camera.helper

/**
 * @author Santiago Varela Daza
 * @email svarela03@uan.edu.co
 * @github https://github.com/sanvarela03
 * @since 8/15/2025
 * @version 1.0
 */
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import android.util.Log
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PoseLandmarkerHelper(
    private val context: Context,
    private val runningMode: RunningMode = RunningMode.LIVE_STREAM,
    private val minPoseDetectionConfidence: Float = DEFAULT_POSE_DETECTION_CONFIDENCE,
    private val minPoseTrackingConfidence: Float = DEFAULT_POSE_TRACKING_CONFIDENCE,
    private val minPosePresenceConfidence: Float = DEFAULT_POSE_PRESENCE_CONFIDENCE,
    private val currentModel: Int = MODEL_POSE_LANDMARKER_FULL,
    private val currentDelegate: Int = DELEGATE_CPU
) {
    private var poseLandmarker: PoseLandmarker? = null

    // StateFlow para los resultados (Compose-friendly)
    private val _poseResults = MutableStateFlow<PoseLandmarkerResult?>(null)
    val poseResults: StateFlow<PoseLandmarkerResult?> = _poseResults.asStateFlow()

    // StateFlow para el tiempo de inferencia
    private val _inferenceTime = MutableStateFlow(0L)
    val inferenceTime: StateFlow<Long> = _inferenceTime.asStateFlow()

    // StateFlow para errores
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _inputImageWidth = MutableStateFlow(1)
    val inputImageWidth: StateFlow<Int> = _inputImageWidth.asStateFlow()

    private val _inputImageHeight = MutableStateFlow(1)
    val inputImageHeight: StateFlow<Int> = _inputImageHeight.asStateFlow()


    private val transformMatrix = Matrix()

    init {
        setupPoseLandmarker()
    }

    private fun setupPoseLandmarker() {
        val baseOptionBuilder = BaseOptions.builder()

        when (currentDelegate) {
            DELEGATE_CPU -> baseOptionBuilder.setDelegate(Delegate.CPU)
            DELEGATE_GPU -> baseOptionBuilder.setDelegate(Delegate.GPU)
        }

        val modelName = when (currentModel) {
            MODEL_POSE_LANDMARKER_FULL -> "pose_landmarker_full.task"
            MODEL_POSE_LANDMARKER_LITE -> "pose_landmarker_lite.task"
            MODEL_POSE_LANDMARKER_HEAVY -> "pose_landmarker_heavy.task"
            else -> "pose_landmarker_full.task"
        }

        baseOptionBuilder.setModelAssetPath(modelName)

        try {
            val baseOptions = baseOptionBuilder.build()
            val optionsBuilder = PoseLandmarker.PoseLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setMinPoseDetectionConfidence(minPoseDetectionConfidence)
                .setMinTrackingConfidence(minPoseTrackingConfidence)
                .setMinPosePresenceConfidence(minPosePresenceConfidence)
                .setRunningMode(runningMode)

            if (runningMode == RunningMode.LIVE_STREAM) {
                optionsBuilder
                    .setResultListener(this::onPoseLandmarkerResult)
                    .setErrorListener(this::onPoseLandmarkerError)
            }

            val options = optionsBuilder.build()
            poseLandmarker = PoseLandmarker.createFromOptions(context, options)

        } catch (e: Exception) {
            _error.value = "Error al inicializar PoseLandmarker: ${e.message}"
            Log.e(TAG, "Error al inicializar: ${e.message}")
        }
    }

    fun detectLiveStream(imageProxy: ImageProxy, isFrontCamera: Boolean) {
        if (runningMode != RunningMode.LIVE_STREAM) {
            _error.value = "Modo incorrecto para detección en tiempo real"
            return
        }

        val frameTime = SystemClock.uptimeMillis()

        try {
            // Convertir ImageProxy a Bitmap
            val bitmapBuffer = Bitmap.createBitmap(
                imageProxy.width,
                imageProxy.height,
                Bitmap.Config.ARGB_8888
            )

            imageProxy.use { proxy ->
                bitmapBuffer.copyPixelsFromBuffer(proxy.planes[0].buffer)
            }

            // Rotar y voltear la imagen si es necesario
            val matrix = transformMatrix(imageProxy, isFrontCamera)

            val rotatedBitmap = Bitmap.createBitmap(
                bitmapBuffer, 0, 0, bitmapBuffer.width, bitmapBuffer.height,
                matrix, true
            )

            // Convertir a MPImage y procesar
            val mpImage = BitmapImageBuilder(rotatedBitmap).build()
            detectAsync(mpImage, frameTime)

            // Limpiar bitmaps
            bitmapBuffer.recycle()
            rotatedBitmap.recycle()

        } catch (e: Exception) {
            _error.value = "Error procesando imagen: ${e.message}"
            Log.e(TAG, "Error procesando imagen: ${e.message}")
        } finally {
            imageProxy.close()
        }
    }

    private fun transformMatrix(
        imageProxy: ImageProxy,
        isFrontCamera: Boolean
    ): Matrix {
        transformMatrix.reset()
        transformMatrix.postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
        if (isFrontCamera) {
            transformMatrix.postScale(
                -1f,
                1f,
                imageProxy.width.toFloat(),
                imageProxy.height.toFloat()
            )
        }
        return transformMatrix
    }

    private fun detectAsync(mpImage: MPImage, frameTime: Long) {
        poseLandmarker?.detectAsync(mpImage, frameTime)
    }

    private fun onPoseLandmarkerResult(
        result: PoseLandmarkerResult,
        input: MPImage
    ) {
        val finishTimeMs = SystemClock.uptimeMillis()
        val inferenceTime = finishTimeMs - result.timestampMs()

        _poseResults.value = result
        _inferenceTime.value = inferenceTime

        // <- muy importante: pasar las dimensiones del MPImage (ya rotado)
        _inputImageWidth.value = input.width
        _inputImageHeight.value = input.height
    }

    private fun onPoseLandmarkerError(error: RuntimeException) {
        _error.value = error.message ?: "Error desconocido en PoseLandmarker"
        Log.e(TAG, "Error en PoseLandmarker: ${error.message}")
    }

    fun clearPoseLandmarker() {
        poseLandmarker?.close()
        poseLandmarker = null
    }

    fun isClose(): Boolean = poseLandmarker == null

    companion object {
        private const val TAG = "PoseLandmarkerHelper"

        const val DELEGATE_CPU = 0
        const val DELEGATE_GPU = 1
        const val DEFAULT_POSE_DETECTION_CONFIDENCE = 0.5f
        const val DEFAULT_POSE_TRACKING_CONFIDENCE = 0.5f
        const val DEFAULT_POSE_PRESENCE_CONFIDENCE = 0.5f
        const val MODEL_POSE_LANDMARKER_FULL = 0
        const val MODEL_POSE_LANDMARKER_LITE = 1
        const val MODEL_POSE_LANDMARKER_HEAVY = 2
    }
}