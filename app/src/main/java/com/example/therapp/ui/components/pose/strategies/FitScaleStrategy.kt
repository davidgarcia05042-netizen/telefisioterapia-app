package com.example.therapp.ui.components.pose.strategies


/**
 * @author Santiago Varela Daza
 * @email svarela03@uan.edu.co
 * @github https://github.com/sanvarela03
 * @since 10/17/2025
 * @version 1.0
 */
class FitScaleStrategy : ScaleStrategy {
    override fun calculate(canvasW: Float, canvasH: Float, imageW: Int, imageH: Int): Float {
        return minOf(canvasW / imageW, canvasH / imageH)
    }
}