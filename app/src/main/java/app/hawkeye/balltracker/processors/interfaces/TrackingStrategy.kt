package app.hawkeye.balltracker.processors.interfaces

import app.hawkeye.balltracker.utils.AdaptiveRect
import app.hawkeye.balltracker.utils.AdaptiveScreenPoint

interface TrackingStrategy {

    fun getAreaOfDetectionAtTime(absTime_ms: Long) : List<AdaptiveRect>

    fun updateLastDetectedObjectPositionAtTime(adaptiveRect: AdaptiveRect?, absTime_ms: Long)

    object Default : TrackingStrategy {
        override fun getAreaOfDetectionAtTime(absTime_ms: Long): List<AdaptiveRect> {
            return listOf(
                AdaptiveRect(
                    AdaptiveScreenPoint(0.5f, 0.5f),
                    1.0f, 1.0f
                )
            )
        }

        override fun updateLastDetectedObjectPositionAtTime(adaptiveRect: AdaptiveRect?, absTime_ms: Long) {}
    }

}