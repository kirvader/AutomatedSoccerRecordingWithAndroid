package app.hawkeye.balltracker.processors.interfaces

import app.hawkeye.balltracker.utils.AdaptiveRect
import app.hawkeye.balltracker.utils.ScreenPoint

interface TrackingStrategy {

    fun getCurrentFrameAreaOfDetection() : List<AdaptiveRect>

    object Default : TrackingStrategy {
        override fun getCurrentFrameAreaOfDetection(): List<AdaptiveRect> {
            return listOf(
                AdaptiveRect(
                    ScreenPoint(0.5f, 0.5f),
                    1.0f, 1.0f
                )
            )
        }
    }

}