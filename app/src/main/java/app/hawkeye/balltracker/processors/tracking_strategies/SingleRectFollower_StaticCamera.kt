package app.hawkeye.balltracker.processors.tracking_strategies

import app.hawkeye.balltracker.processors.interfaces.TrackingStrategy
import app.hawkeye.balltracker.utils.AdaptiveRect
import app.hawkeye.balltracker.utils.AdaptiveScreenPoint
import app.hawkeye.balltracker.utils.createLogger
import kotlin.math.*

private val LOG = createLogger<SingleRectFollower_StaticCamera>()

class SingleRectFollower_StaticCamera(
    private val getBallScreenPositionAtTime: (Long) -> AdaptiveScreenPoint?
) : TrackingStrategy {
    private var lastDetectedObjectRect: AdaptiveRect = AdaptiveRect(AdaptiveScreenPoint(0.5f, 0.5f), 0.1f, 0.1f)
    private var lastTimeOfUpdate_ms: Long = 0L

    private var adaptiveRectGrowthFactor: Float = -log(0.1f, exp(1.0f))

    private fun getSizeAtTime(baseSize: Float, absTime_ms: Long): Float {
        val deltaTime = (absTime_ms - lastTimeOfUpdate_ms).toFloat() / 1000

        return min(1.0f, baseSize * exp(adaptiveRectGrowthFactor * min(maxTimeOfRelevance, deltaTime)))
    }

    private fun updateGrowthFactor(adaptiveRect: AdaptiveRect) {
        adaptiveRectGrowthFactor = -log(max(adaptiveRect.height, adaptiveRect.width), exp(1.0f)) / maxTimeOfRelevance
    }

    override fun getAreaOfDetectionAtTime(absTime_ms: Long): List<AdaptiveRect> {
//        val ballScreenPoint = getBallScreenPositionAtTime(absTime_ms)
//            ?: return listOf(
//                AdaptiveRect(ScreenPoint(0.5f, 0.5f), 1.0f, 1.0f)
//            )
        val currentAreaOfDetectionSize = getSizeAtTime(max(lastDetectedObjectRect.height, lastDetectedObjectRect.width), absTime_ms)


        LOG.i("get ${lastDetectedObjectRect.center.y}")

        val movedArea = AdaptiveRect(lastDetectedObjectRect.center, currentAreaOfDetectionSize, currentAreaOfDetectionSize).moveAllToScreen()

        LOG.i("moved get ${movedArea.center.y}")

        return listOf(movedArea)
    }

    override fun updateLastDetectedObjectPositionAtTime(adaptiveRect: AdaptiveRect?, absTime_ms: Long) {
        if (adaptiveRect == null) {
            return
        }

        lastDetectedObjectRect = adaptiveRect
        lastTimeOfUpdate_ms = absTime_ms

        updateGrowthFactor(lastDetectedObjectRect)

        LOG.i("update ${lastDetectedObjectRect.center.x} ${lastDetectedObjectRect.center.y}")
    }

    companion object {
        private const val maxTimeOfRelevance = 1.0f
    }
}