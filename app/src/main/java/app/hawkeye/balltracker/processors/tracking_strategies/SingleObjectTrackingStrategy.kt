package app.hawkeye.balltracker.processors.tracking_strategies

import app.hawkeye.balltracker.processors.utils.AdaptiveScreenRect


interface SingleObjectTrackingStrategy {

    fun getAreaOfDetectionAtTime(absTime_ms: Long): AdaptiveScreenRect

    fun updateLastDetectedObjectPositionAtTime(adaptiveRect: AdaptiveScreenRect?, absTime_ms: Long)

    object Default : SingleObjectTrackingStrategy {
        override fun getAreaOfDetectionAtTime(absTime_ms: Long): AdaptiveScreenRect {
            return AdaptiveScreenRect(
                0f, 0f,
                1.0f, 1.0f
            )
        }

        override fun updateLastDetectedObjectPositionAtTime(
            adaptiveRect: AdaptiveScreenRect?,
            absTime_ms: Long
        ) {
        }
    }

}