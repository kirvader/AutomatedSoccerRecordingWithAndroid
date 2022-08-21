package app.hawkeye.balltracker.processors.tracking_strategies

import app.hawkeye.balltracker.processors.utils.AdaptiveScreenRect


interface TrackingStrategy {

    fun getAreaOfDetectionAtTime(absTime_ms: Long) : List<AdaptiveScreenRect>

    fun updateLastDetectedObjectPositionAtTime(adaptiveRect: AdaptiveScreenRect?, absTime_ms: Long)

    object Default : TrackingStrategy {
        override fun getAreaOfDetectionAtTime(absTime_ms: Long): List<AdaptiveScreenRect> {
            return listOf(
                AdaptiveScreenRect(
                    0f, 0f,
                    1.0f, 1.0f
                )
            )
        }

        override fun updateLastDetectedObjectPositionAtTime(adaptiveRect: AdaptiveScreenRect?, absTime_ms: Long) {}
    }

}