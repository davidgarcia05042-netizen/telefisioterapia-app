package com.example.therapp.ui.components.pose.strategies

import com.google.mediapipe.tasks.vision.core.RunningMode

class ScaleStrategyFactory {
    companion object {
        fun create(runningMode: RunningMode): ScaleStrategy {
            return when (runningMode) {
                RunningMode.IMAGE, RunningMode.VIDEO -> FitScaleStrategy()
                RunningMode.LIVE_STREAM -> FillScaleStrategy()
            }
        }
    }
}